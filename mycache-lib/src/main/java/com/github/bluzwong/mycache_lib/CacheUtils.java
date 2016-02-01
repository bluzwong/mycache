package com.github.bluzwong.mycache_lib;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/8/27.
 */
public class CacheUtils {

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
    public static String getMD5(String info) {
        return getMD5(info.getBytes());
    }

    public static String getMD5(byte[] info) {
        if (null == info || info.length == 0) {
            return null;
        }
        StringBuilder buf = new StringBuilder("");
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        md.update(info);
        byte b[] = md.digest();
        int i;

        for (int offset = 0; offset < b.length; offset++) {
            i = b[offset];
            if (i < 0)
                i += 256;
            if (i < 16)
                buf.append("0");
            buf.append(Integer.toHexString(i));
        }
        return buf.toString();
    }

    public static void cacheLog(String msg) {
        cacheLog(msg, -1);
    }

    public static void logWarn(String msg) {
        if (CacheUtils.isNeedLog()) {
            Log.w("mycache", msg);
        }
    }

    public static void cacheLog(String msg, long startTime) {
        if (CacheUtils.isNeedLog()) {
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
