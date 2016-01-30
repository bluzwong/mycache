package com.github.bluzwong.mycache;

import android.content.Context;
import android.util.Log;
import com.github.bluzwong.mycache_lib.CacheUtil;
import com.github.bluzwong.mycache_lib.DefaultDiskCacheInterceptor;
import com.github.bluzwong.mycache_lib.UrlTimeOutMap;
import com.github.bluzwong.mycache_lib.calladapter.MyCache;
import com.github.bluzwong.mycache_lib.calladapter.MyCacheCore;
import com.github.bluzwong.mycache_lib.calladapter.MyCacheRxCallAdapterFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangzhijie on 2016/1/19.
 */
public class WebApi {

    public interface MyService {
        @GET("/valueindex.php")
        Observable<ValueIndex> getUrls();

        @GET
        Observable<Value1> getValue1(@Url String url);

        @GET
        Observable<Value2> getValue2(@Url String url);

        @GET
        @MyCache(timeOut = 5000)
        Observable<Result> getResult(@Url String url, @Query("value1") String value1, @Query("value2") String value2);
    }

    String[] paths = new String[] { "/valueindex.php", "/result.php" };


    private Retrofit retrofit;
    public MyService myService;

    public synchronized void init(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("http://mt58866.xicp.net:66/")
                    .client(new OkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(MyCacheRxCallAdapterFactory.create(MyCacheCore.create(context)))
                    .build();
        }
        if (myService == null) {
            myService = retrofit.create(MyService.class);
        }
    }
}
