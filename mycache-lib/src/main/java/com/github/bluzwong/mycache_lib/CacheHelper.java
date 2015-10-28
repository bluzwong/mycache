package com.github.bluzwong.mycache_lib;

import android.util.Log;
import io.paperdb.Paper;
import io.realm.Realm;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Created by wangzhijie on 2015/10/28.
 */
public class CacheHelper {
    private final static class Block {
        public Block(boolean started) {
            this.started = started;
        }

        public boolean started = false;
    }

    private static final Map<String, Block> blocks = new HashMap<String, Block>();


    public static Observable<Object> getCachedMethod(final Observable<Object> originObservable, String methodSignature,
                                                     final boolean needMemoryCache, final long memTime,
                                                     boolean needDiskCache, final long diskTime)
    {
        // 不需要缓存的话直接返回
        if (originObservable == null || (!needMemoryCache && !needDiskCache)) {
            logWarn("originObservable cannot be null or needMemoryCache or needDiskCache");
            return originObservable;
        }
        if (needDiskCache  && CacheUtil.getApplicationContext() == null) {
            needDiskCache = false;
            logWarn("!!!Cannot init database caused no Context. Please call CacheUtil.setApplicationContext(this); in onCreate().");
        }
        final DefaultCacheMemoryHolder memoryRepo = DefaultCacheMemoryHolder.INSTANCE;
        final String key = methodSignature;
        Object objCachedInMemory = memoryRepo.get(key);
        if (needMemoryCache && objCachedInMemory != null) {
            // 需要内存缓存 并且获取到了缓存数据
            long outTime = memoryRepo.getTimeOut(key);
            if (outTime > System.currentTimeMillis() || outTime <= 0) {
                // 没过期 直接返回
                cacheLog(" hit in memory cache key:" + key + "  so return object:" + objCachedInMemory);
                return Observable.just(objCachedInMemory);
            } else {
                cacheLog(" key:" + key + " in memory is out of time");
            }
        } else {
            cacheLog(" key:" + key + " in memory is missed or out of time");
        }

        // 如果内存中有缓存 上面就已经返回了 以下都是内存中没有缓存的情形
        // 先尝试从数据库获取
        final boolean finalNeedDiskCache = needDiskCache;
        return Observable.just(null)
                .map(new Func1<Object, Object>() {
                    @Override
                    public Object call(Object o) {
                        final long now = System.currentTimeMillis();
                        final Realm realm = Realm.getInstance(CacheUtil.getApplicationContext());
                        CacheInfo cacheInfo = realm.where(CacheInfo.class)
                                .equalTo("key", key)
                                .greaterThan("expTime", now)
                                .findFirst();
                        if (finalNeedDiskCache) {
                            if (cacheInfo != null && Paper.exist(cacheInfo.getObjGuid())) {
                                Object objFromDb = Paper.get(cacheInfo.getObjGuid());
                                if (needMemoryCache) {
                                    memoryRepo.put(key, objFromDb, cacheInfo.getExpTime(), 0);
                                    cacheLog(" hit in database cache key:" + key + "  so save to memory object:" + objFromDb);
                                }
                                cacheLog(" hit in database cache key:" + key + "  so return object:" + objFromDb);
                                realm.close();
                                return objFromDb;
                            } else {
                                cacheLog(" key:" + key + " in database is missed");
                            }
                        }
                        // 如果硬盘中有缓存 上面就已经返回了 以下是本地都没有缓存的情形
                        // 从原始数据源获取
                        final Block block;
                        synchronized (this) {
                            if (!blocks.containsKey(key)) {
                                block = new Block(false);
                                blocks.put(key, block);
                            } else {
                                block = blocks.get(key);
                            }
                        }
                        final Object[] newResponse = new Object[1];
                        final CountDownLatch latch = new CountDownLatch(1);
                        synchronized (block) {
                            Object cachedValueAfterBlock = memoryRepo.get(key);
                            if (needMemoryCache && cachedValueAfterBlock != null) {
                                if (memoryRepo.getTimeOut(key) > now) {
                                    cacheLog(" hit in blocked memory cache key:" + key + "  so return object:" + cachedValueAfterBlock);
                                    return cachedValueAfterBlock;
                                }
                                //Log.d(cachedValueAfterBlock + "", " after newRequestStarted return cached key:" + key + " value" + cachedValueAfterBlock);
                            }
                            realm.refresh();
                            cacheInfo = realm.where(CacheInfo.class)
                                    .equalTo("key", key)
                                    .greaterThan("expTime", now)
                                    .findFirst();

                            if (finalNeedDiskCache && cacheInfo != null && Paper.exist(cacheInfo.getObjGuid())) {
                                Object objFromDb = Paper.get(cacheInfo.getObjGuid());
                                if (needMemoryCache) {
                                    memoryRepo.put(key, objFromDb, cacheInfo.getExpTime(), 0);
                                    cacheLog(" hit in database cache key:" + key + "  so save to memory object:" + objFromDb);
                                }
                                cacheLog(" hit in blocked database cache key:" + key +  "  so return object:" + cachedValueAfterBlock);
                                realm.close();
                                return objFromDb;
                            }

                            originObservable.subscribe(new Action1<Object>() {
                                @Override
                                public void call(Object o) {
//                      Log.d("bruce", "2 thread = " + Thread.currentThread().getName());
                                    newResponse[0] = o;
                                    if (o != null && needMemoryCache) {
                                        memoryRepo.put(key, o, memTime > 0 ? memTime + System.currentTimeMillis() : Long.MAX_VALUE, 0);
                                        cacheLog(" got new object save to memory cache key:" + key + "  object:" + o);
                                    }
                                    if (o != null && finalNeedDiskCache) {

                                        //CacheObject cacheObject = new CacheObject();
                                        Realm realm = Realm.getInstance(CacheUtil.getApplicationContext());
                                        realm.beginTransaction();
                                        CacheInfo info;
                                        info = realm.where(CacheInfo.class).equalTo("key", key).findFirst();
                                        if (info == null) {
                                            info = realm.createObject(CacheInfo.class);
                                            info.setKey(key);
                                            String guid = UUID.randomUUID().toString();
                                            info.setObjGuid(guid);
                                        }
                                        info.setEditTime(now);
                                        info.setExpTime(diskTime > 0 ? diskTime + System.currentTimeMillis() : Long.MAX_VALUE);
                                        Paper.put(info.getObjGuid(), o);
                                        realm.commitTransaction();
                                        realm.close();
                                        cacheLog(" got new object save to database cache key:" + key +  "  object:" + o);
                                    }
                                    latch.countDown();
                                }
                            });
                        }
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
//                Log.d("bruce", "3 thread = " + Thread.currentThread().getName());
                        cacheLog(" got new object return it: " + key + " object:" + newResponse[0]);
                        realm.close();
                        return newResponse[0];
                    }
                });
    }

    private static void cacheLog(String msg) {
        cacheLog(msg, -1);
    }

    private static void logWarn(String msg) {
        if (CacheUtil.isNeedLog()) {
            Log.w("mycache", msg);
        }
    }
    private static void cacheLog(String msg, long startTime) {
        if (CacheUtil.isNeedLog()) {
            if (startTime > 0) {
                long t = System.currentTimeMillis() - startTime;
                msg = "[" + t + "ms] " + msg;
                Log.i("mycache", msg);
            } else {
                Log.d("mycache", msg);
            }
        }
    }
}
