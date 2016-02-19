package com.github.bluzwong.mycache_lib;

import android.content.Context;

/**
 * Created by Bruce-Home on 2016/2/1.
 */
public class MyCache {
    private static boolean sNeedLog = false;

    public static boolean isNeedLog() {
        return sNeedLog;
    }

    public static void setNeedLog(boolean needLog) {
        sNeedLog = needLog;
    }

    public static Context sContext;

    public static void init(Context context) {
        if (sContext == null) {
            sContext = context;
        }
    }
}
