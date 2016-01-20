package com.github.bluzwong.mycache;

/**
 * Created by wangzhijie on 2015/10/21.
 */
public class Value2 {
    @Override
    public String toString() {
        return "value2 : " + value2;
    }
    /**
     * value2 : 24
     */

    private String value2;

    public void setValue2(String value2) {
        this.value2 = value2;
    }

    public String getValue2() {
        return value2;
    }
}
