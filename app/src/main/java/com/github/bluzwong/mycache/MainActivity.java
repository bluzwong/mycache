package com.github.bluzwong.mycache;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import com.github.bluzwong.mycache_lib.Cache;
import com.github.bluzwong.mycache_lib.CacheUtil;
import com.github.bluzwong.mycache_lib.Ignore;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CacheUtil.setApplicationContext(this);
        CacheUtil.setNeedLog(true);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                /*MainActivity$Cached.testFunc(MainActivity.this, 1, true, null)
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(Schedulers.newThread())
                        .subscribe(new Action1<Integer>() {
                            @Override
                            public void call(Integer integer) {
                                Snackbar.make(view, "integer is => " + integer, Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                Log.i("cache", "integer = " + integer);
                            }
                        });*/
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final int result = MainActivity$Cached.testSync(MainActivity.this, 123, null);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Snackbar.make(view, "integer is => " + result, Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                Log.i("cache", "integer = " + result);

                            }
                        });
                    }
                }).start();
            }
        });
    }

    @Cache(inMemory = true, memTimeOut = 5000, inDisk = true, diskTimeOut = 10000)
    public int testSync(@Ignore int a, List c) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 123;
    }

    @Cache(inMemory = true, memTimeOut = 5000, inDisk = true, diskTimeOut = 10000)
    public Observable<Integer> testFunc(@Ignore int a, boolean b, List c) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
