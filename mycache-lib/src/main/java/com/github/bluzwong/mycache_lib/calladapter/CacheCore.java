package com.github.bluzwong.mycache_lib.calladapter;

/**
 * Created by bluzwong on 2016/1/30.
 */
public interface CacheCore {
    long NO_CACHE = -1;
    long ALWAYS_CACHE = Long.MAX_VALUE;

    void saveCache(String url, byte[] rawResponse, long timeOut);
    byte[] loadCache(String url);
}
