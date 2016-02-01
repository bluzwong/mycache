package com.github.bluzwong.mycache_lib.functioncache;

import static com.github.bluzwong.mycache_lib.CacheHelper.*;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by bluzwong on 2016/2/1.
 */
public enum RxCacheAdapter {
    INSTANCE;

    public static final String DEFAULT_NAME = "RxCache-lib";
    private Map<String, CountDownLatch> latches = new HashMap<>();
    private RxCacheCore cacheCore;

    public void init(RxCacheCore cacheCore) {
        if (cacheCore == null) {
            throw new IllegalArgumentException("RxCacheCore can not be null");
        }
        this.cacheCore = cacheCore;
    }


    public <T> Observable<T> addCache(final Observable<T> originOb, final String key, final long timeout) {
        Observable<T> loadFromCache = loadFromCache(key, timeout);
        Observable<T> cachedLatchOriginOb = Observable.just(addToLatches(key))
                .flatMap(new Func1<CountDownLatch, Observable<T>>() {
                    @Override
                    public Observable<T> call(CountDownLatch countDownLatch) {
                        return saveToCache(originOb, key, timeout).subscribeOn(Schedulers.io());
                    }
                }).doOnNext(new Action1<T>() {
                    @Override
                    public void call(T t) {
                        removeFromLatches(key);
                    }
                });


        Observable<T> secondLoadFromCacheOrOrigin = secondLoadFromCacheOrOrigin(cachedLatchOriginOb, key, timeout);
        return Observable.concat(loadFromCache.subscribeOn(Schedulers.io()), secondLoadFromCacheOrOrigin.subscribeOn(Schedulers.io()))
                .filter(new Func1<T, Boolean>() {
                    @Override
                    public Boolean call(T t) {
                        return t != null;
                    }
                }).first();

    }

    private <T> Observable<T> secondLoadFromCacheOrOrigin(Observable<T> originOb, final String key, final long timeout) {
        return waitIfNeed(key)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<Boolean, Observable<T>>() {
                    @Override
                    public Observable<T> call(Boolean needLoadCache) {
                        if (needLoadCache) {
                            Observable<T> loadFromCache = loadFromCache(key, timeout);
                            return loadFromCache.subscribeOn(Schedulers.io());
                        }
                        return Observable.empty();
                    }
                }).concatWith(originOb.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()))
                .filter(new Func1<T, Boolean>() {
                    @Override
                    public Boolean call(T t) {
                        return t != null;
                    }
                }).first();
    }


    private void removeFromLatches(String key) {
        if (latches.containsKey(key)) {
            CountDownLatch latch = latches.get(key);
            if (latch != null) {
                latch.countDown();
            }
            latches.remove(key);
        }
    }

    private CountDownLatch addToLatches(String key) {
        if (latches.containsKey(key)) {
            CountDownLatch latch = latches.get(key);
            if (latch != null) {
                return latch;
            }
        }
        CountDownLatch latch = new CountDownLatch(1);
        latches.put(key, latch);
        return latch;
    }

    private <T> Observable<T> loadFromCache(final String key, final long timeout) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                cacheLog("try load from cache => " + key);
                T objFromCache = cacheCore.loadCache(key, timeout);
                if (objFromCache != null) {
                    cacheLog("hit cache => " + key);
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(objFromCache);
                    }
                } else {
                    cacheLog("not cached => " + key);
                }
                subscriber.onCompleted();
            }
        });
    }

    private <T> Observable<T> saveToCache(Observable<T> originOb, final String key, final long timeout) {
        return originOb.doOnNext(new Action1<T>() {
            @Override
            public void call(T t) {
                if (t == null) {
                    cacheLog("origin data is null, don't save to cache => " + key);
                    return;
                }
                cacheLog(" save to cache => " + key);
                cacheCore.saveCache(key, t, timeout);
            }
        });
    }

    private Observable<Boolean> waitIfNeed(final String key) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                CountDownLatch latch = latches.get(key);
                if (latch == null) {
                    // is not requesting
                    cacheLog("no latch no wait => " + key);
                    // do not need load from cache
                    subscriber.onNext(false);
                    subscriber.onCompleted();
                    return;
                }
                try {
                    // wait for request finish
                    cacheLog("latch await until origin complete => " + key);
                    latch.await(10, TimeUnit.SECONDS);
                    // request finished
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cacheLog("latch await down go next => " + key);
                // try from cache
                subscriber.onNext(true);
                subscriber.onCompleted();
            }
        });
    }
}
