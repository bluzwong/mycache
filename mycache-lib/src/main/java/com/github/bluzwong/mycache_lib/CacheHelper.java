package com.github.bluzwong.mycache_lib;

import android.util.Log;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;



/**
 * Created by wangzhijie on 2015/10/28.
 */
public class CacheHelper {

    public static <T> Observable<T> getCachedMethod(Observable<T> originOb, final String key, final long timeout) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                Object obj = MemoryCacheManager.INSTANCE.get(key);
                if (obj != null) {
                    cacheLog("get object from memory cache: " + key + " => " + obj);
                    subscriber.onNext((T) obj);
                    return;
                }
                cacheLog("not in memory cache: " + key);
                subscriber.onCompleted();
            }
        }).concatWith(originOb.map(new Func1<T, T>() {
            @Override
            public T call(T t) {
                MemoryCacheManager.INSTANCE.put(key, timeout, t);
                return t;
            }
        })).filter(new Func1<T, Boolean>() {
            @Override
            public Boolean call(T t) {
                return t != null;
            }
        }).first();
    }

    public static void cacheLog(String msg) {
        cacheLog(msg, -1);
    }

    public static void logWarn(String msg) {
        if (CacheUtil.isNeedLog()) {
            Log.w("mycache", msg);
        }
    }

    public static void cacheLog(String msg, long startTime) {
        if (CacheUtil.isNeedLog()) {
            if (startTime > 0) {
                long t = System.currentTimeMillis() - startTime;
                msg = "[" + t + "ms] " + msg;
                Log.i("mycache", msg);
            } else {
                Log.d("mycache", msg);
            }
        }
    }
}
