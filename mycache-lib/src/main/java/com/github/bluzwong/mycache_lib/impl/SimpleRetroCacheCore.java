package com.github.bluzwong.mycache_lib.impl;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.LruCache;
import com.github.bluzwong.mycache_lib.CacheUtils;
import com.github.bluzwong.mycache_lib.calladapter.RetrofitCacheCore;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;

/**
 * Created by bluzwong on 2016/1/30.
 */
public class SimpleRetroCacheCore implements RetrofitCacheCore {
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10; //10MB
    private static final int DEFAULT_MEMORY_CACHE_SIZE = 100; // 100ge
    private static final String CACHE_NAME = "mycache_core";
    private DiskLruCache diskCache;
    private LruCache<String, TimeAndObject> memoryCache;

    private WillSave willSave;
    private WillLoad willLoad;
    private SharedPreferences preferences;

    public SimpleRetroCacheCore(Context context, File diskDirectory, int memorySize, long maxDiskSize) {
        try {
            diskCache = DiskLruCache.open(diskDirectory, 1, 1, maxDiskSize);
            preferences = context.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE);
        } catch (IOException exc) {
            diskCache = null;
        }
        memoryCache = new LruCache<>(memorySize);
    }

    public static SimpleRetroCacheCore create(Context context, int memoryCacheSize, int diskCacheSize ) {
        return new SimpleRetroCacheCore(context, new File(context.getCacheDir(), CACHE_NAME), memoryCacheSize, diskCacheSize);
    }

    public static SimpleRetroCacheCore create(Context context) {
        return create(context, DEFAULT_MEMORY_CACHE_SIZE, DEFAULT_DISK_CACHE_SIZE);
    }

    public SimpleRetroCacheCore setWillSave(WillSave willSave) {
        this.willSave = willSave;
        return this;
    }

    public SimpleRetroCacheCore setWillLoad(WillLoad willLoad) {
        this.willLoad = willLoad;
        return this;
    }

    @Override
    public void saveCache(String url, byte[] rawResponse, long timeOut) {
        if (willSave != null && !willSave.shouldSave(url, rawResponse, timeOut)) { return; }
        if (rawResponse == null || rawResponse.length <= 0) { return; }
        if (timeOut == RetrofitCacheCore.NO_CACHE || timeOut <= 0) { return; }

        long now = System.currentTimeMillis();
        long expireTime = now + timeOut;

        if (timeOut == Long.MAX_VALUE || timeOut == RetrofitCacheCore.ALWAYS_CACHE) {
            expireTime = Long.MAX_VALUE;
        }
        String key = CacheUtils.getMD5(url);

        memoryCache.put(key, new TimeAndObject(expireTime, rawResponse));
        if (diskCache == null) { return; }

        synchronized (this) {
            preferences.edit().putLong(key, expireTime).apply();
            try {
                DiskLruCache.Editor editor = diskCache.edit(key);
                if (editor == null) {
                    return;
                }
                editor.set(0, new String(rawResponse, "UTF-8"));
                editor.commit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public byte[] loadCache(String url, long timeOut) {
        if (willLoad != null && !willLoad.shouldLoad(url)) { return null ;}
        long now = System.currentTimeMillis();

        String key = CacheUtils.getMD5(url);
        TimeAndObject timeAndObject = memoryCache.get(key);
        if (timeAndObject != null && timeAndObject.object != null && timeAndObject.object.length > 0) {
            if (timeAndObject.expireTime >= now && timeAndObject.expireTime <= now + timeOut) {
                // object exists and not timeout
                return timeAndObject.object;
            } else {
                // objects exists but is timeout
                removeKey(key);
                return null;
            }
        }
        // not in memory cache, check disk cache
        if (diskCache == null) {
            // not in disk cache
            return null;
        }

        long expireTime = preferences.getLong(key, RetrofitCacheCore.NO_CACHE);
        if (expireTime == RetrofitCacheCore.NO_CACHE || expireTime <= 0 || expireTime < now || expireTime > now + timeOut) {
            // time out or no need to cache
            removeKey(key);
            return null;
        }
        // not time out , try load from disk cache
        try {
            String stringObject = diskCache.get(key).getString(0);
            if (stringObject == null || stringObject.equals("")) {
                // not in disk cache
                return null;
            }
            // in disk cache
            byte[] rawResponse = stringObject.getBytes();
            // and save in memory cache
            memoryCache.put(key, new TimeAndObject(expireTime, rawResponse));
            return rawResponse;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void removeKey(String key) {
        memoryCache.remove(key);
        if (diskCache != null) {
            try {
                diskCache.remove(key);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class TimeAndObject {
        public long expireTime;
        public byte[] object;

        public TimeAndObject(long expireTime, byte[] object) {
            this.expireTime = expireTime;
            this.object = object;
        }
    }

    public DiskLruCache getDiskCore() {
        return diskCache;
    }

    public LruCache<String, TimeAndObject> getMemoryCache() {
        return memoryCache;
    }

    public String getKeyByUrl(String url) {
        return CacheUtils.getMD5(url);
    }

    public interface WillSave {
        boolean shouldSave(String url, byte[] rawResponse, long timeOut);
    }

    public interface WillLoad {
        boolean shouldLoad(String url);
    }
}
