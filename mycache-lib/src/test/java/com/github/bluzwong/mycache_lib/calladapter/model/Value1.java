package com.github.bluzwong.mycache_lib.calladapter.model;

/**
 * Created by wangzhijie on 2015/10/21.
 */
public class Value1 {

    @Override
    public String toString() {
        return "value1 : " + value1;
    }

    /**
     * value1 : 24
     */

    private String value1;

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    public String getValue1() {
        return value1;
    }
}
