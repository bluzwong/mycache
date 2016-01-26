package com.github.bluzwong.mycache_lib;

import android.content.Context;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URL;

/**
 * Created by wangzhijie on 2016/1/21.
 */
public class DefaultDiskCacheInterceptor implements Interceptor {
    private ICacheManager manager;
    private UrlTimeOutMap map;
    private ShouldSaveCacheAfterResponse shouldSaveCacheAfterResponse;
    private ShouldLoadCacheBeforeRequest shouldLoadCacheBeforeRequest;

    public DefaultDiskCacheInterceptor(ICacheManager manager, UrlTimeOutMap map, ShouldSaveCacheAfterResponse shouldSaveCacheAfterResponse, ShouldLoadCacheBeforeRequest shouldLoadCacheBeforeRequest) {
        this.manager = manager;
        this.map = map;
        this.shouldSaveCacheAfterResponse = shouldSaveCacheAfterResponse;
        this.shouldLoadCacheBeforeRequest = shouldLoadCacheBeforeRequest;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originRequest = chain.request();
        Request request = originRequest;
        String urlString = request.url().url().toString();
        CacheHelper.cacheLog("request url => " + urlString);
        boolean hasCached = false;
        if (manager!= null) {
            hasCached = manager.get(urlString);
        }

        if (shouldLoadCacheBeforeRequest != null
                && !shouldLoadCacheBeforeRequest.shouldLoadCache(request)) {
            hasCached = false;
        }

        if (hasCached) {
            CacheHelper.cacheLog("requesting url => " + urlString + "\n may has cache, try cache");
            request = request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build();
        } else {
            CacheHelper.cacheLog("requesting url => " + urlString + "\n has not cache, so get from origin");
        }

        Response response = chain.proceed(request);
        // try load from cache
        if (hasCached && response.code() == 504) {
            // cache not exists, reload from origin
            CacheHelper.cacheLog("requesting url => " + urlString + "\n cache not exists, reload from origin");
            response = chain.proceed(originRequest);
        } else {
            CacheHelper.cacheLog("requesting url => " + urlString + "\n cache is exists, and not 504. success");
        }
        boolean shouldSave = true;

        if (shouldSaveCacheAfterResponse != null
                && !shouldSaveCacheAfterResponse.shouldSaveCache(response)) {
            shouldSave = false;
        }

        if (!hasCached && manager != null && shouldSave) {
            long timeOut = 60_000;
            if (map != null) {
                URL url = new URL(urlString);
                timeOut = map.urlForMap(url, url.getAuthority(), url.getPath());
            }
            if (timeOut > 0) {
                CacheHelper.cacheLog("requesting url => " + urlString + "\n save to memory cache with timeout @ " + timeOut + "ms");
                manager.put(urlString, timeOut);
            } else {
                CacheHelper.cacheLog("requesting url => " + urlString + "\n time out <= 0, do not make cache");
            }
        }
        return response;
    }

    public static Builder Builder(Context context) {
        return new Builder(context);
    }

    public static class Builder {
        private ICacheManager manager;
        private UrlTimeOutMap map;
        private ShouldSaveCacheAfterResponse shouldSaveCacheAfterResponse;
        private ShouldLoadCacheBeforeRequest shouldLoadCacheBeforeRequest;
        private Context context;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setManager(ICacheManager manager) {
            this.manager = manager;
            return this;
        }

        public Builder setMap(UrlTimeOutMap map) {
            this.map = map;
            return this;
        }

        public Builder setShouldLoadCacheBeforeRequest(ShouldLoadCacheBeforeRequest shouldLoadCacheBeforeRequest) {
            this.shouldLoadCacheBeforeRequest = shouldLoadCacheBeforeRequest;
            return this;
        }

        public Builder setShouldSaveCacheAfterResponse(ShouldSaveCacheAfterResponse shouldSaveCacheAfterResponse) {
            this.shouldSaveCacheAfterResponse = shouldSaveCacheAfterResponse;
            return this;
        }

        public DefaultDiskCacheInterceptor build() {
            if (manager == null) {
                manager = new DiskCacheManager(context);
            }
            return new DefaultDiskCacheInterceptor(manager, map, shouldSaveCacheAfterResponse, shouldLoadCacheBeforeRequest);
        }
    }


    public interface ShouldSaveCacheAfterResponse {
        boolean shouldSaveCache(Response response);
    }

    public interface ShouldLoadCacheBeforeRequest {
        boolean shouldLoadCache(Request request);
    }

}
