package com.github.bluzwong.mycache;

import android.test.InstrumentationTestCase;
import com.github.bluzwong.mycache_lib.functioncache.RxCache;
import rx.Observable;
import rx.functions.Func0;

/**
 * Created by Bruce-Home on 2016/1/31.
 */
public class CacheTestCase extends InstrumentationTestCase {
    public void testMyCache() throws Exception {
        assertEquals(1,1);
    }

    private static int plus5(int value) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return value + 5;
    }

    @RxCache(timeOut = 5000)
    public Observable<Integer> requestRxjava(final int value) {
        return Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.just(plus5(value));
            }
        });
    }
}
