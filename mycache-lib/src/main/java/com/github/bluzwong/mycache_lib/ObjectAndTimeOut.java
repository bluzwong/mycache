package com.github.bluzwong.mycache_lib;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/9/15.
 */
public class ObjectAndTimeOut {
    public Object obj;
    public long timeOut;

    public ObjectAndTimeOut(Object obj, long timeOut) {
        this.obj = obj;
        this.timeOut = timeOut;
    }
}
