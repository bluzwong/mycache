package com.github.bluzwong.mycache

import com.github.bluzwong.mycache_lib.functioncache.RxCache
import rx.Observable

/**
 * Created by bluzwong on 2016/2/2.
 */
class KotClass {

    @RxCache(timeOut = 5000)
    fun kotRx():Observable<Int> {
        return Observable.defer {
            Observable.just(MainActivity.plus5(5))
        }
    }
}