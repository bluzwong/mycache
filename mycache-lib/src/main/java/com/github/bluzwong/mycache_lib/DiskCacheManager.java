package com.github.bluzwong.mycache_lib;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by wangzhijie on 2016/1/20.
 */
public class DiskCacheManager implements ICacheManager {
    private SharedPreferences preferences;

    public DiskCacheManager(Context context) {
        preferences = CacheUtil.getDefaultDiskCacheSharedPreferences(context);
    }

    @Override
    public void put(String key, long timeout) {
        if (key == null || key.equals("")) {
            return;
        }
        if (preferences == null) {
            return;
        }
        long expireTime = 0;
        long now = System.currentTimeMillis();
        if (timeout >= Long.MAX_VALUE - now) {
            expireTime = Long.MAX_VALUE;
        } else if (timeout > 0 ) {
            expireTime = now + timeout;
        }

        if (expireTime > now) {
            preferences.edit().putLong(key, expireTime).apply();
        }
    }

    @Override
    public boolean get(String key) {
        if (key == null || key.equals("")) {
            return false;
        }
        if (preferences == null || !preferences.contains(key)) {
            return false;
        }
        long savedExpireTime = preferences.getLong(key, -1);
        if (savedExpireTime == -1) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now <= savedExpireTime) {
            return true;
        }
        return false;
    }
}
