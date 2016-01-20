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
        initBtn();
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
}
