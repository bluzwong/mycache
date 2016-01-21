package com.github.bluzwong.mycache;

import android.content.Intent;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CacheUtil.setNeedLog(true);
        WebApi.INSTANCE.init(this);
        initBtn();

        if (savedInstanceState != null) {
            long saveStartTime = System.currentTimeMillis();
            List<String> datas = (List<String>) ByteToObject(savedInstanceState.getByteArray("ccf"));
//            List<String> datas = savedInstanceState.getStringArrayList("ccf");
            long endStartTime = System.currentTimeMillis();
            Log.i("bruce", "[" + (endStartTime - saveStartTime)+"ms] save wsd @ " + datas.hashCode() + " => " + datas);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        ArrayList<String> datas = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            datas.add("string " + i);
        }
        long saveStartTime = System.currentTimeMillis();
        outState.putByteArray("ccf", ObjectToByte(datas));
//        outState.putStringArrayList("ccf", datas);
        long endStartTime = System.currentTimeMillis();
        super.onSaveInstanceState(outState);
        Log.i("bruce", "[" + (endStartTime - saveStartTime)+"ms] save wsd @ " + datas.hashCode() + " => " + datas);
    }

    private void initBtn() {
        findViewById(R.id.btn_rx).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                // 使用自动生成的包装方法来缓存rxjava请求
                MainActivity_Cache.requestRxjava(MainActivity.this, new Random().nextInt(2), 2)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<String>() {
                            @Override
                            public void call(String result) {
                                Snackbar.make(v, "rxjava请求完成 => " + result, Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                Log.i("cache", "result = " + result);
                            }
                        });
                final Timer timer = new Timer();
                timer.setStartTime();

                int i = new Random().nextInt(2);
                WebApi.INSTANCE.myService.getResult("http://mt58866.xicp.net:66/result.php", i+"", 1-i+"")
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Result>() {
                            @Override
                            public void call(Result result) {
                                timer.printUsingTime("bruce-re");
                                Log.i("bruce-re", "result  => " + result.getResult());
                            }
                        });
            }
        });

    }

    /**
     * 模拟耗时的rxjava请求
     * @return
     */
    @CacheInMemory(timeOut = 10_000)
    public Observable<String> requestRxjava(final int value,  int iccf) {
        return Observable.just(null)
                .map(new Func1<Object, String>() {
                    @Override
                    public String call(Object o) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return value + " after @@@@@@@@@@";
                    }
                });
    }

    static class Timer {
        private long startTime;

        void setStartTime() {
            startTime = System.currentTimeMillis();
        }

        long getUsingTime() {
            return System.currentTimeMillis() - startTime;
        }

        void printUsingTime(String owner) {
            Log.i("httprequest", owner + " using time => " + getUsingTime() + " ms");
        }
    }



    public static Object ByteToObject(byte[] bytes){
        java.lang.Object obj = null;
        try{
            ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
            ObjectInputStream oi = new ObjectInputStream(bi);
            obj = oi.readObject();
            bi.close();
            oi.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return obj;
    }

    public static byte[] ObjectToByte(Object obj)
    {
        byte[] bytes = null;
        try {
            //object to bytearray
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);
            bytes = bo.toByteArray();
            bo.close();
            oo.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
