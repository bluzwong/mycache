package com.github.bluzwong.mycache_lib.calladapter;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by bluzwong on 2016/1/30.
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface MyCache {
    long timeOut() default CacheCore.ALWAYS_CACHE;
}
