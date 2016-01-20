package com.github.bluzwong.mycache;

import android.content.Context;
import com.github.bluzwong.mycache_lib.CacheUtil;
import com.github.bluzwong.mycache_lib.UrlTimeOutMap;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.RxJavaCallAdapterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangzhijie on 2016/1/19.
 */
public enum WebApi {
    INSTANCE;

    public interface MyService {
        @GET("/valueindex.php")
        Observable<ValueIndex> getUrls();

        @GET
        Observable<Value1> getValue1(@Url String url);

        @GET
        Observable<Value2> getValue2(@Url String url);

        @GET
        Observable<Result> getResult(@Url String url, @Query("value1") String value1, @Query("value2") String value2);
    }


    String[] paths = new String[] { "/valueindex.php", "/result.php" };
    long[] timeOuts = new long[] { 60_000, 10_000 };
    Map<String, Long> pathToTimeout;
    UrlTimeOutMap map = new UrlTimeOutMap() {
        @Override
        public long urlForMap(URL url, String baseUrl, String path) {
            if (pathToTimeout.containsKey(path)) {
                return pathToTimeout.get(path);
            }
            return 0;
        }
    };

    private Retrofit retrofit;
    public MyService myService;

    public synchronized void init(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("http://mt58866.xicp.net:66/")
                    .client(CacheUtil.myCacheClient(context.getApplicationContext(), map))
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build();
        }
        if (myService == null) {
            myService = retrofit.create(MyService.class);
        }
        if (pathToTimeout == null) {
            pathToTimeout = new HashMap<>();
            for (int i = 0; i < paths.length; i++) {
                long timeout = 60_000;
                if (i < timeOuts.length) {
                    timeout = timeOuts[i];
                }
                pathToTimeout.put(paths[i], timeout);
            }
        }
    }
}