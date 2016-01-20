package com.github.bluzwong.mycache;

/**
 * Created by wangzhijie on 2015/10/21.
 */
public class Result {

    @Override
    public String toString() {
        return " result : " + result;
    }

    /**
     * result : 3
     */

    private String result;

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}
