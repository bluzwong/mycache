package com.github.bluzwong.mycache;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import com.github.bluzwong.mycache_lib.*;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CacheUtil.setApplicationContext(this);
        CacheUtil.setNeedLog(true);
        initBtn();
    }

    private void initBtn() {
        findViewById(R.id.btn_sync).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 使用自动生成的包装方法来缓存同步请求
                        final int result = MainActivity_Cache.requestSync(MainActivity.this, 123, null);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Snackbar.make(v, "同步请求完成 => " + result, Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                Log.i("cache", "integer = " + result);
                            }
                        });
                    }
                }).start();
            }
        });

        findViewById(R.id.btn_async).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                // 使用自动生成的包装方法来缓存异步请求
                MainActivity_Cache.requestAsync(MainActivity.this, new Response() {
                    @Override
                    public void fun(int result) {
                        Snackbar.make(v, "异步请求完成 => " + result, Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        Log.i("cache", "integer = " + result);
                    }
                }, "");
            }
        });


        findViewById(R.id.btn_rx).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                // 使用自动生成的包装方法来缓存rxjava请求
                MainActivity_Cache.requestRxjava(MainActivity.this)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Integer>() {
                            @Override
                            public void call(Integer integer) {
                                Snackbar.make(v, "rxjava请求完成 => " + integer, Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                Log.i("cache", "integer = " + integer);
                            }
                        });
            }
        });

    }


    /**
     * 模拟耗时的同步请求
     * @param a
     * @param c
     * @return
     */
    @Cache(inMemory = true, memTimeOut = 5000, inDisk = true, diskTimeOut = 10000)
    public int requestSync(@Ignore int a, List c) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 123;
    }




    interface Response {
        void fun(int result);
    }

    /**
     * 模拟耗时的异步请求
     * @param callback
     * @param wsd
     */
    @Cache(inMemory = true, memTimeOut = 5000, inDisk = true, diskTimeOut = 10000)
    public void requestAsync(@Callback final Response callback, String wsd) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.fun(123);
                    }
                });
            }
        }).start();
    }
/*
    public void requestAsyncDynamic(final Response callback, final int ccf, final String wsd) {
        final CountDownLatch latch = new CountDownLatch(1);
        Object result;
        Object instance = Proxy.newProxyInstance(getClassLoader(), new Class[]{Response.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return null;
            }
        });

        final Response myResponse = (Response) instance;
        Object cachedResult = CacheHelper.getCachedMethodSync(new CacheHelper.Fun1() {
            @Override
            public Object func() {
                requestAsync(myResponse, ccf, wsd);
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return ;
            }
        }, "ccf", true, 1111, true, 1111);
    }*/

    /**
     * 模拟耗时的rxjava请求
     * @param a
     * @param b
     * @param c
     * @return
     */
    @Cache(inMemory = true, memTimeOut = 5000, inDisk = true, diskTimeOut = 10000)
    public Observable<Integer> requestRxjava() {
        return Observable.just(null)
                .map(new Func1<Object, Integer>() {
                    @Override
                    public Integer call(Object o) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return 123;
                    }
                });
    }
}
