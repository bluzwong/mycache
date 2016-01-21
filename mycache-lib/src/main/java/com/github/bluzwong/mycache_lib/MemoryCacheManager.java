package com.github.bluzwong.mycache_lib;

import java.util.Map;

/**
 * Created by wangzhijie on 2016/1/20.
 */
public enum MemoryCacheManager {
    INSTANCE;
    private final Map<String, CacheInfoObject> cacheInfoObjectMap = new LruMap<>(100, 0.75f, true);

    public void put(String key, long timeout, Object object) {
        if (key == null || key.equals("")) {
            return;
        }
        long now = System.currentTimeMillis();
        CacheInfoObject cacheInfoObject = new CacheInfoObject(key, now, timeout, object);
        cacheInfoObjectMap.put(key, cacheInfoObject);
    }

    public Object get(String key) {
        if (key == null || key.equals("")) {
            return null;
        }
        if (!cacheInfoObjectMap.containsKey(key)) {
            return null;
        }
        CacheInfoObject cacheInfoObject = cacheInfoObjectMap.get(key);
        if (cacheInfoObject == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now <= cacheInfoObject.getExpireTime()) {
            // ok
            return cacheInfoObject.getObject();
        }
        return null;
    }

    public Map<String, CacheInfoObject> getMap() {
        return cacheInfoObjectMap;
    }
}
