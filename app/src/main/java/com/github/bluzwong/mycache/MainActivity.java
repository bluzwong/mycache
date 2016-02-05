package com.github.bluzwong.mycache;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import com.github.bluzwong.mycache_lib.*;
import com.github.bluzwong.mycache_lib.functioncache.RxCache;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.Random;


public class MainActivity extends AppCompatActivity {
    WebApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MyCache.init(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MyCache.setNeedLog(true);
        api = new WebApi();
        api.init();
        initBtn();
    }

    private void initBtn() {
        findViewById(R.id.btn_rx).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                // 使用自动生成的包装方法来缓存rxjava请求
                final Timer timer = new Timer();
                timer.setStartTime();
                MainActivity_Cache.requestRxjava(MainActivity.this, new Random().nextInt(2))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Integer>() {
                            @Override
                            public void call(Integer result) {
                                Snackbar.make(v, "rxjava请求完成 => " + result, Snackbar.LENGTH_SHORT)
                                        .setAction("Action", null).show();
                                Log.i("cache", "result = " + result + " [" + timer.getUsingTime() +"ms]");
                            }
                        });
                /*final Timer timer = new Timer();
                timer.setStartTime();

                int i = 1;//new Random().nextInt(2);
                api.myService.getResult("http://mt58866.xicp.net:66/result.php", i + "", 1 - i + "")
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Result>() {
                            @Override
                            public void call(Result result) {
                                timer.printUsingTime("bruce-re");
                                Log.i("bruce-re", "result  => " + result.getResult());
                            }
                        });*/
            }
        });

    }

    static int plus5(int value) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return value + 5;
    }

    @RxCache(timeOut = 5000)
    public Observable<Integer> requestRxjava(final int value) {
        return Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.just(plus5(value));
            }
        });
    }

    @RxCache(timeOut = 5000)
    public Observable<Integer> requestRxjava(final int value, final int value2) {
        return Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.just(plus5(value+value2));
            }
        });
    }


    @RxCache(timeOut = 5000)
    public static Observable<Integer> requestRxjavaStatic(final int value) {
        return Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.just(plus5(value));
            }
        });
    }

    @RxCache(timeOut = 5000)
    public static Observable<Integer> requestRxjavaStatic(final int value, final int value2) {
        return Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.just(plus5(value+value2));
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
}
