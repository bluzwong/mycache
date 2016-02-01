package com.github.bluzwong.mycache_lib;

import android.content.SharedPreferences;
import android.util.LruCache;
import com.github.bluzwong.mycache_lib.calladapter.CacheCore;
import io.paperdb.Book;


/**
 * Created by bluzwong on 2016/2/1.
 */
public class BaseCacheCore {
    public static class TimeAndObject {
        public long expireTime;
        public Object object;

        public TimeAndObject(long expireTime, Object object) {
            this.expireTime = expireTime;
            this.object = object;
        }
    }

    public interface WillSave {
        boolean shouldSave(String key, Object object, long timeOut);
    }

    public interface WillLoad {
        boolean shouldLoad(String key);
    }

    private WillSave willSave;
    private WillLoad willLoad;
    private LruCache<String, TimeAndObject> memoryCache;
    private SharedPreferences preferences;
    private Book book;

    public SharedPreferences getPreferences() {
        return preferences;
    }

    public LruCache<String, TimeAndObject> getMemoryCache() {
        return memoryCache;
    }

    public Book getBook() {
        return book;
    }

    public BaseCacheCore(LruCache<String, TimeAndObject> memoryCache, SharedPreferences preferences, Book book) {
        if (memoryCache == null || preferences == null || book == null) {
            throw new IllegalArgumentException("can not be null");
        }
        this.memoryCache = memoryCache;
        this.preferences = preferences;
        this.book = book;
    }

    public void setWillSave(WillSave willSave) {
        this.willSave = willSave;
    }

    public void setWillLoad(WillLoad willLoad) {
        this.willLoad = willLoad;
    }

    public String getKeyBySign(String sign) {
        return CacheUtil.getMD5(sign);
    }

    private void removeByKey(String key) {
        memoryCache.remove(key);
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            if (editor != null) {
                editor.remove(key).apply();
            }
        }
        if (book != null) {
            if (book.exist(key)) {
                book.delete(key);
            }
        }
    }

    protected void baseSaveCache(String sign, Object object, long timeOut) {
        if (willSave != null && !willSave.shouldSave(sign, object, timeOut)) {
            return;
        }
        if (object == null) {
            return;
        }
        if (timeOut == CacheCore.NO_CACHE || timeOut <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long expireTime = now + timeOut;

        if (timeOut == Long.MAX_VALUE || timeOut == CacheCore.ALWAYS_CACHE) {
            expireTime = Long.MAX_VALUE;
        }
        String key = getKeyBySign(sign);

        memoryCache.put(key, new TimeAndObject(expireTime, object));
        if (preferences == null || book == null) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        if (editor == null) {
            return;
        }
        editor.putLong(key, expireTime).apply();
        book.write(key, object);
    }

    protected Object baseLoadCache(String sign, long timeOut) {
        if (willLoad != null && !willLoad.shouldLoad(sign)) {
            return null;
        }
        long now = System.currentTimeMillis();

        String key = getKeyBySign(sign);
        TimeAndObject timeAndObject = memoryCache.get(key);
        if (timeAndObject != null && timeAndObject.object != null) {
            if (timeAndObject.expireTime >= now && timeAndObject.expireTime <= now + timeOut) {
                // object exists and not timeout
                return timeAndObject.object;
            } else {
                // objects exists but is timeout
                removeByKey(key);
                return null;
            }
        }
        // not in memory cache, check disk cache
        if (preferences == null || book == null) {
            // not in disk cache
            return null;
        }

        long expireTime = preferences.getLong(key, CacheCore.NO_CACHE);
        if (expireTime == CacheCore.NO_CACHE || expireTime <= 0 || expireTime < now || expireTime > now + timeOut) {
            // time out or no need to cache
            removeByKey(key);
            return null;
        }
        // not time out , try load from disk cache

        Object objectFromDisk = book.read(key);
        if (objectFromDisk == null) {
            // not in disk cache
            return null;
        }
        // in disk cache
        // and save in memory cache
        memoryCache.put(key, new TimeAndObject(expireTime, objectFromDisk));
        return objectFromDisk;
    }

}
