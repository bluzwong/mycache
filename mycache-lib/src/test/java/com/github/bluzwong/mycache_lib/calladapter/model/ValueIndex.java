package com.github.bluzwong.mycache_lib.calladapter.model;

/**
 * Created by wangzhijie on 2015/10/21.
 */
public class ValueIndex {

    @Override
    public String toString() {
        return "url_value1: " + url_value1
                + "\nurl_value2: " + url_value2
                + "\nurl_result: " + url_result;
    }

    /**
     * url_value1 : http://mt58866.xicp.net:66/value1.php
     * url_value2 : http://mt58866.xicp.net:66/value2.php
     * url_result : http://mt58866.xicp.net:66/result.php
     */

    private String url_value1;
    private String url_value2;
    private String url_result;

    public void setUrl_value1(String url_value1) {
        this.url_value1 = url_value1;
    }

    public void setUrl_value2(String url_value2) {
        this.url_value2 = url_value2;
    }

    public void setUrl_result(String url_result) {
        this.url_result = url_result;
    }

    public String getUrl_value1() {
        return url_value1;
    }

    public String getUrl_value2() {
        return url_value2;
    }

    public String getUrl_result() {
        return url_result;
    }
}
