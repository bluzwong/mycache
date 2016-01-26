package com.github.bluzwong.mycache_lib;


import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

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

    // manual do with disk cache
    public static SharedPreferences getDefaultDiskCacheSharedPreferences(Context context) {
        return context.getSharedPreferences("my-cache-lib-disk", Context.MODE_PRIVATE);
    }

    // manual do with memory cache
    public Map<String, CacheInfoObject> getDefaultMemoryMap() {
        return MemoryCacheManager.INSTANCE.getMap();
    }

    public static String getMethodName(Class clz, Method method) {
        String clzName = clz.getName();
        String methodName = method.getName();
        return clzName + "." + methodName;
    }

    private static ICacheManager manager;


    public static Cache myCacheCache(Context context, long size) {
        File httpCacheDirectory = new File(context.getExternalCacheDir(), "my-cache-lib-disk");
        return new Cache(httpCacheDirectory, size);
    }

    public static OkHttpClient myCacheClient(Context context, DefaultDiskCacheInterceptor interceptor) {
        return new OkHttpClient.Builder()
                .cache(myCacheCache(context, 10 * 1024 * 1024))
                .addInterceptor(interceptor)
                .build();
    }
}
