package com.github.bluzwong.mycache_lib;

/**
 * Created by wangzhijie on 2016/1/20.
 */
public class CacheInfoObject {
    private String key;
    private long saveTime;
    private long timeout;
    private long expireTime;
    private Object object;

    public CacheInfoObject(String key, long saveTime, long timeout, Object object) {
        this.key = key;
        this.saveTime = saveTime;
        this.timeout = timeout;
        this.object = object;
        if (timeout <= 0 ) {
            expireTime = Long.MAX_VALUE;
        } else {
            expireTime = timeout + saveTime;
        }
    }

    public Object getObject() {
        return object;
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
