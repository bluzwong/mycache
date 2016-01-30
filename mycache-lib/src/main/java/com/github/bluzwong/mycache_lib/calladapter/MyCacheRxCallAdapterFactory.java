package com.github.bluzwong.mycache_lib.calladapter;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.*;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by bluzwong on 2016/1/30.
 */


public final class MyCacheRxCallAdapterFactory implements CallAdapter.Factory {

    private CacheCore cacheCore;

    /**
     * TODO
     */
    public static MyCacheRxCallAdapterFactory create(CacheCore cacheCore) {
        if (cacheCore == null) {
            throw new IllegalArgumentException("CacheCore can not be null.");
        }
        return new MyCacheRxCallAdapterFactory(cacheCore);
    }

    private MyCacheRxCallAdapterFactory(CacheCore cacheCore) {
        this.cacheCore = cacheCore;
    }

    @Override
    public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        Class<?> rawType = Utils.getRawType(returnType);
        boolean isSingle = "rx.Single".equals(rawType.getCanonicalName());
        if (rawType != Observable.class && !isSingle) {
            return null;
        }
        if (!(returnType instanceof ParameterizedType)) {
            String name = isSingle ? "Single" : "Observable";
            throw new IllegalStateException(name + " return type must be parameterized"
                    + " as " + name + "<Foo> or " + name + "<? extends Foo>");
        }

        CallAdapter<Observable<?>> callAdapter = getCallAdapter(returnType, annotations, retrofit);
        if (isSingle) {
            // Add Single-converter wrapper from a separate class. This defers classloading such that
            // regular Observable operation can be leveraged without relying on this unstable RxJava API.
            return SingleHelper.makeSingle(callAdapter);
        }
        return callAdapter;
    }

    private CallAdapter<Observable<?>> getCallAdapter(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        Type observableType = Utils.getParameterUpperBound(0, (ParameterizedType) returnType);
        Class<?> rawObservableType = Utils.getRawType(observableType);
        if (rawObservableType == Response.class) {
            if (!(observableType instanceof ParameterizedType)) {
                throw new IllegalStateException("Response must be parameterized"
                        + " as Response<Foo> or Response<? extends Foo>");
            }
            Type responseType = Utils.getParameterUpperBound(0, (ParameterizedType) observableType);
            return new ResponseCallAdapter(responseType);
        }

        if (rawObservableType == Result.class) {
            if (!(observableType instanceof ParameterizedType)) {
                throw new IllegalStateException("Result must be parameterized"
                        + " as Result<Foo> or Result<? extends Foo>");
            }
            Type responseType = Utils.getParameterUpperBound(0, (ParameterizedType) observableType);
            return new ResultCallAdapter(responseType);
        }

        for (Annotation annotation : annotations) {
            if (annotation instanceof MyCache) {
                MyCache myCache = (MyCache) annotation;
                return new SimpleCacheCallAdapter(retrofit, observableType, cacheCore, annotations, myCache.timeOut());
            }
        }
        return new SimpleCallAdapter(observableType);
    }

    static final class CallOnSubscribe<T> implements Observable.OnSubscribe<Response<T>> {
        private final Call<T> originalCall;

        CallOnSubscribe(Call<T> originalCall) {
            this.originalCall = originalCall;
        }

        @Override
        public void call(final Subscriber<? super Response<T>> subscriber) {
            // Since Call is a one-shot type, clone it for each new subscriber.
            final Call<T> call = originalCall.clone();

            // Attempt to cancel the call if it is still in-flight on unsubscription.
            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    call.cancel();
                }
            }));

            try {
                Response<T> response = call.execute();
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(response);
                }
            } catch (Throwable t) {
                Exceptions.throwIfFatal(t);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(t);
                }
                return;
            }

            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        }
    }

    static final class ResponseCallAdapter implements CallAdapter<Observable<?>> {
        private final Type responseType;

        ResponseCallAdapter(Type responseType) {
            this.responseType = responseType;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public <R> Observable<Response<R>> adapt(Call<R> call) {
            return Observable.create(new CallOnSubscribe<>(call));
        }
    }

    static final class SimpleCallAdapter implements CallAdapter<Observable<?>> {
        private final Type responseType;

        SimpleCallAdapter(Type responseType) {
            this.responseType = responseType;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public <R> Observable<R> adapt(Call<R> call) {
            return Observable.create(new CallOnSubscribe<>(call)) //
                    .flatMap(new Func1<Response<R>, Observable<R>>() {
                        @Override
                        public Observable<R> call(Response<R> response) {
                            if (response.isSuccess()) {
                                return Observable.just(response.body());
                            }
                            return Observable.error(new HttpException(response));
                        }
                    });
        }
    }

    // add by bluz
    static final class SimpleCacheCallAdapter implements CallAdapter<Observable<?>> {
        private Retrofit retrofit;
        private final Type responseType;
        private CacheCore cacheCore;
        private Annotation[] annotations;
        private long timeOut;

        SimpleCacheCallAdapter(Retrofit retrofit, Type responseType, CacheCore cacheCore, Annotation[] annotations, long timeOut) {
            this.retrofit = retrofit;
            this.responseType = responseType;
            this.cacheCore = cacheCore;
            this.annotations = annotations;
            this.timeOut = timeOut;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public <R> Observable<R> adapt(Call<R> call) {
            Request request = MyUtils.buildRequestFromCall(call);
            final String url = request.url().toString();

            Observable<R> realRequestObservable = Observable.create(new CallOnSubscribe<>(call)) //
                    .flatMap(new Func1<Response<R>, Observable<R>>() {
                        @Override
                        public Observable<R> call(Response<R> response) {
                            if (response.isSuccess()) {
                                return Observable.just(response.body());
                            }
                            return Observable.error(new HttpException(response));
                        }
                    });

            return Observable.create(new Observable.OnSubscribe<R>() {
                @Override
                public void call(Subscriber<? super R> subscriber) {
                    byte[] bytesFromCache = cacheCore.loadCache(url);
                    if (bytesFromCache != null && bytesFromCache.length > 0) {
                        Converter<ResponseBody, Object> converter = MyUtils.getResponseConverter(retrofit, responseType, annotations);
                        try {
                            R responseObject = (R) converter.convert(ResponseBody.create(null, bytesFromCache));
                            if (subscriber.isUnsubscribed()) return;
                            if (responseObject != null) {
                                subscriber.onNext(responseObject);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    subscriber.onCompleted();
                }
            }).concatWith(realRequestObservable.observeOn(Schedulers.io()).doOnNext(new Action1<R>() {
                @Override
                public void call(R r) {
                    Converter<Object, RequestBody> converter = MyUtils.getRequestConverter(retrofit, responseType, annotations);
                    try {
                        RequestBody requestBody = converter.convert(r);
                        Buffer buffer = new Buffer();
                        requestBody.writeTo(buffer);
                        byte[] responseToCache = buffer.readByteArray();
                        cacheCore.saveCache(url, responseToCache, timeOut);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            })).filter(new Func1<R, Boolean>() {
                @Override
                public Boolean call(R r) {
                    return r != null;
                }
            }).first();
        }
    }

    static final class ResultCallAdapter implements CallAdapter<Observable<?>> {
        private final Type responseType;

        ResultCallAdapter(Type responseType) {
            this.responseType = responseType;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public <R> Observable<Result<R>> adapt(Call<R> call) {
            return Observable.create(new CallOnSubscribe<>(call)) //
                    .map(new Func1<Response<R>, Result<R>>() {
                        @Override
                        public Result<R> call(Response<R> response) {
                            return Result.response(response);
                        }
                    })
                    .onErrorReturn(new Func1<Throwable, Result<R>>() {
                        @Override
                        public Result<R> call(Throwable throwable) {
                            return Result.error(throwable);
                        }
                    });
        }
    }
}

