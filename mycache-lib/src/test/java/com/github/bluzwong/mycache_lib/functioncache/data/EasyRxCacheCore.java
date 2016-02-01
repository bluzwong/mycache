package com.github.bluzwong.mycache_lib.functioncache.data;

import com.github.bluzwong.mycache_lib.functioncache.RxCacheCore;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bluzwong on 2016/2/1.
 */
public class EasyRxCacheCore implements RxCacheCore {
    Map<String, ObjectAndExpireTime> map = new HashMap<>();
    @Override
    public <T> void saveCache(String key, T object, long timeOut) {
        map.put(key, new ObjectAndExpireTime(object, System.currentTimeMillis() + timeOut));
    }

    @Override
    public <T> T loadCache(String key, long timeOut) {
        ObjectAndExpireTime objectAndExpireTime = map.get(key);
        if (objectAndExpireTime == null || objectAndExpireTime.expireTime < System.currentTimeMillis()) {
            return null;
        }
        return (T) objectAndExpireTime.obj;
    }

    static class ObjectAndExpireTime {
        public Object obj;
        public long expireTime;

        public ObjectAndExpireTime(Object obj, long expireTime) {
            this.obj = obj;
            this.expireTime = expireTime;
        }
    }
}
