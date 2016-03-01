package com.github.bluzwong.mycache_lib.impl;

import android.content.Context;
import android.util.LruCache;
import com.github.bluzwong.mycache_lib.MyCache;
import com.github.bluzwong.mycache_lib.functioncache.RxCacheCore;
import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.HawkBuilder;
import com.orhanobut.hawk.LogLevel;
/*
import io.paperdb.Book;
import io.paperdb.Paper;
*/

/**
 * Created by bluzwong on 2016/2/1.
 */
public class SimpleRxCacheCore extends BaseCacheCore implements RxCacheCore {

//    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10; //10MB
    private static final int DEFAULT_MEMORY_CACHE_SIZE = 100; // 100ge
    private static final String CACHE_NAME = "rx_cache_core";

    private SimpleRxCacheCore(LruCache<String, TimeAndObject> memoryCache/*, Book book*/) {
        super(memoryCache/*, book*/);

    }

    private SimpleRxCacheCore(int memorySize) {
        this(new LruCache<String, TimeAndObject>(memorySize)/*, Paper.book(CACHE_NAME)*/);
    }

    public static SimpleRxCacheCore create(int memorySize) {
        Context context = MyCache.sContext;
        if (context == null) {
            throw new IllegalArgumentException("need call MyCache.setCacheCore(context);");
        }
        //Paper.init(context.getApplicationContext());
        if (!Hawk.isBuilt()) {
            Hawk.init(context)
                    .setStorage(HawkBuilder.newSharedPrefStorage(context))
                    .setEncryptionMethod(HawkBuilder.EncryptionMethod.NO_ENCRYPTION)
                    .setLogLevel(MyCache.isNeedLog()? LogLevel.FULL:LogLevel.NONE)
                    .build();
        }


        return new SimpleRxCacheCore(memorySize);
    }

    public static SimpleRxCacheCore create() {
        return create(DEFAULT_MEMORY_CACHE_SIZE);
    }

    @Override
    public <T> void saveCache(String key, T object, long timeOut) {
        baseSaveCache(key, object, timeOut);
    }

    @Override
    public <T> T loadCache(String key, long timeOut) {
        return (T) baseLoadCache(key, timeOut);
    }
}
