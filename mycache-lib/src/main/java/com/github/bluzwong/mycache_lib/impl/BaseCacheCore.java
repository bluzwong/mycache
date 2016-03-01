package com.github.bluzwong.mycache_lib.impl;

import android.util.LruCache;
import com.github.bluzwong.mycache_lib.CacheUtils;
import com.github.bluzwong.mycache_lib.calladapter.RetrofitCacheCore;
import com.orhanobut.hawk.Hawk;
//import io.paperdb.Book;


/**
 * Created by bluzwong on 2016/2/1.
 */
public class BaseCacheCore {
    public static class TimeAndObject {
        public long savedTime;
        public Object object;

        public TimeAndObject() {
            /* add for kryo save */
        }

        public TimeAndObject(long savedTime, Object object) {
            /* add for proguard*/
            this();
            this.savedTime = savedTime;
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
//    private SharedPreferences preferences;
//    private Book book;

//    public SharedPreferences getPreferences() {
//        return preferences;
//    }

    public LruCache<String, TimeAndObject> getMemoryCache() {
        return memoryCache;
    }

//    public Book getBook() {
//        return book;
//    }

    public BaseCacheCore(LruCache<String, TimeAndObject> memoryCache/*, Book book*/) {
        if (memoryCache == null /*|| book == null*/) {
            throw new IllegalArgumentException("can not be null");
        }
        this.memoryCache = memoryCache;
//        this.preferences = preferences;
//        this.book = book;
    }

    public void setWillSave(WillSave willSave) {
        this.willSave = willSave;
    }

    public void setWillLoad(WillLoad willLoad) {
        this.willLoad = willLoad;
    }



    private void removeByKey(String key) {
        memoryCache.remove(key);
//        if (preferences != null) {
//            SharedPreferences.Editor editor = preferences.edit();
//            if (editor != null) {
//                editor.remove(key).apply();
//            }
//        }
        if (Hawk.contains(key)) {
            Hawk.remove(key);
        }
       /* if (book != null) {
            if (book.exist(key)) {
                book.delete(key);
            }
        }*/
    }
    public void clearMemoryCache() {
        memoryCache.evictAll();
    }

    public void clearDiskCache() {
//        if (preferences != null) {
//            SharedPreferences.Editor editor = preferences.edit();
//            if (editor != null) {
//                editor.clear();
//            }
//        }
        /*if (book != null) {
            book.destroy();
        }*/
        // todo clear all
        Hawk.clear();
    }

    public void clearAll() {
        clearDiskCache();
        clearMemoryCache();
    }

    protected void baseSaveCache(String key, Object object, long timeOut) {
        if (willSave != null && !willSave.shouldSave(key, object, timeOut)) {
            CacheUtils.cacheLog("will not save: " + key);
            return;
        }
        if (object == null) {
            CacheUtils.cacheLog("saving object is null! : "  + key);
            return;
        }
        if (timeOut == RetrofitCacheCore.NO_CACHE || timeOut <= 0) {
            CacheUtils.cacheLog("time out <= 0, no need to save : " + key);
            return;
        }

        long now = System.currentTimeMillis();
        //long savedTime = now + timeOut;

//        if (timeOut == Long.MAX_VALUE || timeOut == RetrofitCacheCore.ALWAYS_CACHE) {
//            savedTime = Long.MAX_VALUE;
//        }

        TimeAndObject timeAndObject = new TimeAndObject(now, object);
        memoryCache.put(key, timeAndObject);

       CacheUtils.cacheLog("cache to memory done => " + key);
        Hawk.put(key, timeAndObject);
       /* if (book == null) {
            CacheUtils.cacheLog("disk is not inited");
            return;
        }

        if (book.exist(key)) {
            book.delete(key);
        }
        book.write(key, timeAndObject);*/
        CacheUtils.cacheLog("cache to disk done => " + key);
    }

    protected Object baseLoadCache(String key, long timeOut) {
        if (willLoad != null && !willLoad.shouldLoad(key)) {
            CacheUtils.cacheLog("will not load: " + key);
            return null;
        }
        long now = System.currentTimeMillis();

        TimeAndObject timeAndObject = memoryCache.get(key);
        if (false && timeAndObject != null && timeAndObject.object != null) {
            if (timeAndObject.savedTime > 0 && timeAndObject.savedTime < now && now <= timeAndObject.savedTime + timeOut) {
                // object exists and not timeout
                CacheUtils.cacheLog("hit in memory cache => " + key);
                return timeAndObject.object;
            } else {
                // objects exists but is timeout
                CacheUtils.cacheLog("objects exists is memory but is timeout => " + key);
                removeByKey(key);
                return null;
            }
        } else {
            CacheUtils.cacheLog("not in memory cache : " + key);
        }
        // not in memory cache, check disk cache
       /* if (book == null) {
            CacheUtils.cacheLog("disk is not inited : " + key);
            // not in disk cache
            return null;
        }*/

        Object objectFromDisk = Hawk.get(key);// book.read(key);
        if (objectFromDisk == null) {
            // not in disk cache
            CacheUtils.cacheLog("not in disk cache : " + key);
            return null;
        }

        if (!(objectFromDisk instanceof TimeAndObject)) {
           // book.delete(key);
            Hawk.remove(key);
            CacheUtils.cacheLog("saved object error, remove it: " + key);
            return null;
        }
        timeAndObject = (TimeAndObject) objectFromDisk;
        if (timeAndObject.object != null) {
            if (timeAndObject.savedTime > 0 && timeAndObject.savedTime < now && now <= timeAndObject.savedTime + timeOut) {
                // object exists and not timeout
                CacheUtils.cacheLog("hit in disk cache and save it to memory=> " + key);
                memoryCache.put(key, new TimeAndObject(timeAndObject.savedTime, timeAndObject.object));
                return timeAndObject.object;
            } else {
                // objects exists but is timeout
                CacheUtils.cacheLog("objects exists is disk but is timeout => " + key);
                removeByKey(key);
                return null;
            }
        }
        return null;/*
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

        CacheUtils.cacheLog("hit in disk cache => " + key);
        return objectFromDisk;*/
    }

}
