package com.github.bluzwong.mycache_lib.functioncache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.LruCache;
import com.github.bluzwong.mycache_lib.BaseCacheCore;
import io.paperdb.Book;
import io.paperdb.Paper;

/**
 * Created by bluzwong on 2016/2/1.
 */
public class SimpleRxCacheCore extends BaseCacheCore implements RxCacheCore {

//    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10; //10MB
    private static final int DEFAULT_MEMORY_CACHE_SIZE = 100; // 100ge
    private static final String CACHE_NAME = "rx_cache_core";

    private SimpleRxCacheCore(LruCache<String, TimeAndObject> memoryCache, SharedPreferences preferences, Book book) {
        super(memoryCache, preferences, book);

    }

    private SimpleRxCacheCore(Context context, int memorySize) {
        this(new LruCache<String, TimeAndObject>(memorySize), context.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE), Paper.book(CACHE_NAME));
    }

    public static SimpleRxCacheCore create(Context context, int memorySize) {
        Paper.init(context.getApplicationContext());
        return new SimpleRxCacheCore(context, memorySize);
    }

    public static SimpleRxCacheCore create(Context context) {
        return create(context, DEFAULT_MEMORY_CACHE_SIZE);
    }

    @Override
    public <T> void saveCache(String sign, T object, long timeOut) {
        baseSaveCache(sign, object, timeOut);
    }

    @Override
    public <T> T loadCache(String sign, long timeOut) {
        return (T) baseLoadCache(sign, timeOut);
    }
}
