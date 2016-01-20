package com.github.bluzwong.mycache_lib;

/**
 * Created by wangzhijie on 2016/1/20.
 */
public class CacheInfo {
    private String key;
    private long saveTime;
    private long timeout;
    private long expireTime;

    public CacheInfo(String key, long saveTime, long timeout) {
        this.key = key;
        this.saveTime = saveTime;
        this.timeout = timeout;
        if (timeout <= 0 ) {
            expireTime = Long.MAX_VALUE;
        } else {
            expireTime = timeout + saveTime;
        }
    }

    public long getExpireTime() {
        return expireTime;
    }

    public String getKey() {
        return key;
    }

    public long getSaveTime() {
        return saveTime;
    }

    public long getTimeout() {
        return timeout;
    }
}
