package com.github.bluzwong.mycache_lib;


import android.content.Context;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/8/27.
 */
public class CacheUtil {

    private static boolean sNeedLog = false;

    public static boolean isNeedLog() {
        return sNeedLog;
    }

    public static void setNeedLog(boolean needLog) {
        sNeedLog = needLog;
    }

    public static String getMethodName(Class clz, Method method) {
        String clzName = clz.getName();
        String methodName = method.getName();
        return clzName + "." + methodName;
    }

    private static ICacheManager manager;

    public static Interceptor myCacheInterceptor(Context context, final UrlTimeOutMap map) {
        if (manager == null) {
            manager = new DiskCacheManager(context);
        }
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request originRequest = chain.request();
                Request request = originRequest;
                String urlString = request.url().url().toString();
                CacheHelper.cacheLog("request url => " + urlString);
                boolean hasCached = manager.get(urlString);
                if (hasCached) {
                    CacheHelper.cacheLog("requesting url => " + urlString + "\n may has cache, try cache");
                    request = request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build();
                }

                Response response = chain.proceed(request);
                // try load from cache
                if (hasCached && response.code() == 504) {
                    // cache not exists, reload from origin
                    CacheHelper.cacheLog("requesting url => " + urlString + "\n cache not exists, reload from origin");
                    response = chain.proceed(originRequest);
                }

                if (!hasCached) {
                    long timeOut = 60_000;
                    if (map != null) {
                        timeOut = map.urlForMap(new URL(urlString));
                    }
                    CacheHelper.cacheLog("requesting url => " + urlString + "\n save to memory cache with timeout @ " + timeOut + "ms");
                    manager.put(urlString, timeOut);
                }
                return response;
            }
        };
    }

    public static Cache myCacheCache(Context context, long size) {
        File httpCacheDirectory = new File(context.getExternalCacheDir(), "my-cache-lib-disk");
        return new Cache(httpCacheDirectory, size);
    }

    public static OkHttpClient myCacheClient(Context context, final UrlTimeOutMap map) {
        return new OkHttpClient.Builder()
                .cache(myCacheCache(context, 10 * 1024 * 1024))
                .addInterceptor(myCacheInterceptor(context, map))
                .build();
    }
}
