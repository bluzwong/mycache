package com.github.bluzwong.mycache_lib.calladapter;

import com.github.bluzwong.mycache_lib.CacheUtils;
import okhttp3.Request;
import retrofit2.*;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by bluzwong on 2016/1/30.
 */


public final class RetrofitCacheRxCallAdapterFactory implements CallAdapter.Factory {

    private RetrofitCacheCore cacheCore;

    /**
     * TODO
     */
    public static RetrofitCacheRxCallAdapterFactory create(RetrofitCacheCore cacheCore) {
        if (cacheCore == null) {
            throw new IllegalArgumentException("CacheCore can not be null.");
        }
        return new RetrofitCacheRxCallAdapterFactory(cacheCore);
    }

    private RetrofitCacheRxCallAdapterFactory(RetrofitCacheCore cacheCore) {
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

    final CallAdapter<Observable<?>> getCallAdapter(Type returnType, Annotation[] annotations, Retrofit retrofit) {
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
            if (annotation instanceof RetrofitCache) {
                RetrofitCache myCache = (RetrofitCache) annotation;
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
        private RetrofitCacheCore cacheCore;
        private Annotation[] annotations;
        private long timeOut;
        private Map<String, CountDownLatch> latches = new HashMap<>();

        SimpleCacheCallAdapter(Retrofit retrofit, Type responseType, RetrofitCacheCore cacheCore, Annotation[] annotations, long timeOut) {
            this.retrofit = retrofit;
            this.responseType = responseType;
            this.cacheCore = cacheCore;
            this.annotations = annotations;
            this.timeOut = timeOut;
        }

        private void removeFromLatches(String url) {
            if (latches.containsKey(url)) {
                CountDownLatch latch = latches.get(url);
                if (latch != null) {
                    latch.countDown();
                }
                latches.remove(url);
            }
        }
        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public <R> Observable<R> adapt(Call<R> call) {
            Request request = RetrofitUtils.buildRequestFromCall(call);
            final String url = request.url().toString();
            CacheUtils.cacheLog("this requesting url => " + url);
            final Observable<R> realRequestObservable = Observable.create(new CallOnSubscribe<>(call)) //
                    .flatMap(new Func1<Response<R>, Observable<R>>() {
                        @Override
                        public Observable<R> call(Response<R> response) {

                            if (response.isSuccess()) {
                                return Observable.just(response.body());
                            }
                            CacheUtils.cacheLog("response error @@@ requesting end remove latch from map => " + url);
                            removeFromLatches(url);
                            return Observable.error(new HttpException(response));
                        }
                    });


            Observable<R> realRequestObservableAndLatch = Observable.create(new Observable.OnSubscribe<R>() {
                @Override
                public void call(Subscriber<? super R> subscriber) {
                    if (!latches.containsKey(url)) {
                        CacheUtils.cacheLog("before real request put latch to map => " + url);
                        CountDownLatch countDownLatch = new CountDownLatch(1);
                        latches.put(url, countDownLatch);
                    } else {
                        CacheUtils.cacheLog("before real request latch exists => " + url);
                    }
                    subscriber.onNext(null);
                    subscriber.onCompleted();
                }
            }).flatMap(new Func1<R, Observable<R>>() {
                @Override
                public Observable<R> call(R r) {
                    return realRequestObservable.subscribeOn(Schedulers.io());
                }
            }).doOnNext(new Action1<R>() {
                @Override
                public void call(R r) {
                    byte[] rawResponse = RetrofitUtils.getRawResponseFromEntity(r, retrofit, responseType, annotations);
                    if (rawResponse == null || rawResponse.length <= 0) {
                        CacheUtils.cacheLog("WARN! raw response is empty");
                        CacheUtils.cacheLog("response is empty WARN @@@ requesting end remove latch from map => " + url);
                        removeFromLatches(url);
                        return;
                    }
                    CacheUtils.cacheLog("try save cache ,then requesting end remove latch from map => " + url);
                    cacheCore.saveCache(url, rawResponse, timeOut);
                    removeFromLatches(url);
                }
            });

            Observable<Boolean> waitForRequest = Observable.create(new Observable.OnSubscribe<Boolean>() {
                @Override
                public void call(Subscriber<? super Boolean> subscriber) {
                    CountDownLatch latch = latches.get(url);
                    if (latch == null) {
                        // is not requesting
                        CacheUtils.cacheLog("no latch no wait => " + url);
                        // do not need load from cache
                        subscriber.onNext(false);
                        subscriber.onCompleted();
                        return;
                    }
                    try {
                        // wait for request finish
                        CacheUtils.cacheLog("latch await until request complete => " + url);
                        latch.await(10, TimeUnit.SECONDS);
                        // request finished
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    CacheUtils.cacheLog("latch await down go next => " + url);
                    // try from cache
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                }


            });

            final Observable<R> tryLoadCache = Observable.create(new Observable.OnSubscribe<R>() {
                @Override
                public void call(Subscriber<? super R> subscriber) {
                    CacheUtils.cacheLog("try load from cache => " + url);
                    byte[] bytesFromCache = cacheCore.loadCache(url, timeOut);
                    if (bytesFromCache != null && bytesFromCache.length > 0) {
                        R entityFromResponse = RetrofitUtils.getEntityFromResponse(bytesFromCache, retrofit, responseType, annotations);
                        if (entityFromResponse != null) {
                            CacheUtils.cacheLog("hit cache => " + url);
                            subscriber.onNext(entityFromResponse);
                        }
                    } else {
                        CacheUtils.cacheLog("not cached => " + url);
                    }
                    subscriber.onCompleted();
                }
            });

            Observable<R> tryWaitAndGetCacheOrRequest = waitForRequest.flatMap(new Func1<Boolean, Observable<R>>() {
                @Override
                public Observable<R> call(Boolean needFromCache) {
                    if (needFromCache) {
                        return tryLoadCache.subscribeOn(Schedulers.io());
                    }
                    return Observable.empty();
                }
            }).concatWith(realRequestObservableAndLatch.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()))
                    .filter(new Func1<R, Boolean>() {
                        @Override
                        public Boolean call(R r) {
                            return r != null;
                        }
                    }).first();


            return tryLoadCache.concatWith(tryWaitAndGetCacheOrRequest.subscribeOn(Schedulers.io()))
                    .filter(new Func1<R, Boolean>() {
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