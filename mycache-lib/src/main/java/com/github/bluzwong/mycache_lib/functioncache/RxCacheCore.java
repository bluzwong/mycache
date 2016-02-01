package com.github.bluzwong.mycache_lib.functioncache;

/**
 * Created by bluzwong on 2016/2/1.
 */
public interface RxCacheCore {
    long NO_CACHE = -1;
    long ALWAYS_CACHE = Long.MAX_VALUE;
    <T> void saveCache(String key, T object, long timeOut);
    <T>T loadCache(String key, long timeOut);
}
