package com.github.bluzwong.mycache_lib.calladapter.model;

import android.content.Context;
import com.github.bluzwong.mycache_lib.calladapter.MyCache;
import com.github.bluzwong.mycache_lib.calladapter.MyCacheCore;
import com.github.bluzwong.mycache_lib.calladapter.MyCacheRxCallAdapterFactory;
import okhttp3.OkHttpClient;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

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

        @GET("/result.php")
        @MyCache(timeOut = 5000)
        Observable<Result> getResult(@Query("value1") String value1, @Query("value2") String value2);
    }



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
