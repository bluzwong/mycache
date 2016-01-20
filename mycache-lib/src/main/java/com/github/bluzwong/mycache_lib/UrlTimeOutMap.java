package com.github.bluzwong.mycache_lib;

import java.net.URL;

/**
 * Created by wangzhijie on 2016/1/20.
 */
public interface UrlTimeOutMap {
    long urlForMap(URL url, String baseUrl, String path);
}
