package com.github.bluzwong.mycache_lib;

import android.content.SharedPreferences;
import android.util.LruCache;
import com.github.bluzwong.mycache_lib.calladapter.RetrofitCacheCore;
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
    public void clearMemoryCache() {
        memoryCache.evictAll();
    }

    public void clearDiskCache() {
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            if (editor != null) {
                editor.clear();
            }
        }
        if (book != null) {
            book.destroy();
        }
    }

    public void clearAll() {
        clearDiskCache();
        clearMemoryCache();
    }

    protected void baseSaveCache(String key, Object object, long timeOut) {
        if (willSave != null && !willSave.shouldSave(key, object, timeOut)) {
            return;
        }
        if (object == null) {
            return;
        }
        if (timeOut == RetrofitCacheCore.NO_CACHE || timeOut <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long expireTime = now + timeOut;

        if (timeOut == Long.MAX_VALUE || timeOut == RetrofitCacheCore.ALWAYS_CACHE) {
            expireTime = Long.MAX_VALUE;
        }

        memoryCache.put(key, new TimeAndObject(expireTime, object));
        if (preferences == null || book == null) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        if (editor == null) {
            return;
        }
        editor.putLong(key, expireTime).apply();
        if (book.exist(key)) {
            book.delete(key);
        }
        book.write(key, object);
    }

    protected Object baseLoadCache(String key, long timeOut) {
        if (willLoad != null && !willLoad.shouldLoad(key)) {
            return null;
        }
        long now = System.currentTimeMillis();

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

        long expireTime = preferences.getLong(key, RetrofitCacheCore.NO_CACHE);
        if (expireTime == RetrofitCacheCore.NO_CACHE || expireTime <= 0 || expireTime < now || expireTime > now + timeOut) {
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
