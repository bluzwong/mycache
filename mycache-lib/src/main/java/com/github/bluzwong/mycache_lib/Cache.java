package com.github.bluzwong.mycache_lib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wangzhijie on 2015/10/28.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cache {
    boolean inMemory() default true;
    long memTimeOut() default 0;
    boolean inDisk() default true;
    long diskTimeOut() default 0;
}
