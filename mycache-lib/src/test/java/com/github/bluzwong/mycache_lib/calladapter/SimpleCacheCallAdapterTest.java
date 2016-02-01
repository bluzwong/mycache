package com.github.bluzwong.mycache_lib.calladapter;

import android.content.Context;
import com.github.bluzwong.mycache_lib.calladapter.model.*;
import com.github.bluzwong.mycache_lib.BuildConfig;
import com.github.bluzwong.mycache_lib.calladapter.model.Result;
import com.github.bluzwong.mycache_lib.impl.SimpleRetroCacheCore;
import com.google.gson.reflect.TypeToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * Created by Bruce-Home on 2016/1/31.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class SimpleCacheCallAdapterTest {

    WebApi.MyService service;
    Context context;
    RetrofitCacheRxCallAdapterFactory factory;
    Retrofit retrofit;
    Type type = new TypeToken<Observable<String>>() {}.getType();
    Annotation[] annotations;
    SimpleRetroCacheCore cacheCore;
    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.application;
        WebApi api = new WebApi();
        api.init(context);
        retrofit = api.retrofit;
        service = api.myService;
        cacheCore = SimpleRetroCacheCore.create(context);
        factory = RetrofitCacheRxCallAdapterFactory.create(cacheCore);
        annotations = getClass().getDeclaredMethod("testForAnnotation").getAnnotations();
    }

    @After
    public void tearDown() throws Exception {
        //cacheCore.getDiskCore().delete();
        cacheCore.getMemoryCache().evictAll();
    }

    @RetrofitCache
    private void testForAnnotation() {}

    @Test
    public void testAdapt() throws Exception {
        ValueIndex index = service.getUrls().toBlocking().single();
        assertEquals("http://mt58866.xicp.net:66/value1.php", index.getUrl_value1());
    }

    @Test
    public void testGetCallAdapter() throws Exception {
        CallAdapter<Observable<?>> adapter = factory.getCallAdapter(type, annotations, retrofit);
        assertTrue(adapter instanceof RetrofitCacheRxCallAdapterFactory.SimpleCacheCallAdapter);
    }

    @Test
    public void testWholeCall() throws Exception {
        long startTime = System.currentTimeMillis();
        Result result = service.getResult("2", "2").toBlocking().single();
        long using = System.currentTimeMillis() - startTime;
        System.out.println("2 + 2 第一次用时 => " + using);
        assertEquals(result.getResult(), "4");

         startTime = System.currentTimeMillis();
         result = service.getResult("1", "2").toBlocking().single();
         using = System.currentTimeMillis() - startTime;
        assertEquals(result.getResult(), "3");
        System.out.println("1 + 2 第一次用时 => " + using);


         startTime = System.currentTimeMillis();
         result = service.getResult("2", "2").toBlocking().single();
         using = System.currentTimeMillis() - startTime;
        System.out.println("2 + 2 第二次用时 => " + using);
        assertEquals(result.getResult(), "4");
        assertTrue(using <= 5);

        Thread.sleep(5000);
         startTime = System.currentTimeMillis();
         result = service.getResult("2", "2").toBlocking().single();
         using = System.currentTimeMillis() - startTime;
        System.out.println("2 + 2 第三次用时 => " + using);
        assertEquals(result.getResult(), "4");
        assertTrue(using > 5);

        startTime = System.currentTimeMillis();
        result = service.getResult("2", "2").toBlocking().single();
        using = System.currentTimeMillis() - startTime;
        System.out.println("2 + 2 第四次用时 => " + using);
        assertEquals(result.getResult(), "4");
        assertTrue(using <= 5);
    }

    @Test
    public void testMultiCall() throws Exception {
        final long startTime = System.currentTimeMillis();
        final long[] usingtime = {0,0};
        final CountDownLatch latch = new CountDownLatch(2);
        service.getResult("1", "1").subscribeOn(Schedulers.io()).subscribe(new Action1<Result>() {
            @Override
            public void call(Result result) {
                long using = System.currentTimeMillis() - startTime;
                usingtime[0] = using;
                System.out.println("并发第一次时间 => " + using);
                latch.countDown();
            }
        });

        final long startTime2 = System.currentTimeMillis();
        service.getResult("1", "1").subscribeOn(Schedulers.io()).subscribe(new Action1<Result>() {
            @Override
            public void call(Result result) {
                long using = System.currentTimeMillis() - startTime2;
                System.out.println("并发第二次时间 => " + using);
                usingtime[1] = using;
                latch.countDown();
            }
        });

        latch.await();
        assertTrue(usingtime[1] <= usingtime[0] + 5);
    }
}