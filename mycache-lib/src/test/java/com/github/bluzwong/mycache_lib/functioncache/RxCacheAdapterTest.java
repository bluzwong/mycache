package com.github.bluzwong.mycache_lib.functioncache;

import android.content.Context;
import com.github.bluzwong.mycache_lib.BuildConfig;
import com.github.bluzwong.mycache_lib.MyCache;
import com.github.bluzwong.mycache_lib.impl.SimpleRxCacheCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by bluzwong on 2016/2/1.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class RxCacheAdapterTest {

    RxCacheAdapter adapter;
    Context context;

    SimpleRxCacheCore cacheCore;
    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.application;
        MyCache.init(context);
        adapter = RxCacheAdapter.INSTANCE;
        //adapter.setCacheCore(new EasyRxCacheCore());
        cacheCore = SimpleRxCacheCore.create();
        cacheCore.getBook().destroy();
        //cacheCore.getPreferences().edit().clear().commit();
        cacheCore.getMemoryCache().evictAll();
        adapter.setCacheCore(cacheCore);
        adapter.getLatches().clear();
    }

    @After
    public void tearDown() throws Exception {
        adapter.getLatches().clear();
    }

    @Test
    public void testInit() throws Exception {
        assertNotNull(RxCacheAdapter.INSTANCE.getCacheCore());
    }

    @Test
    public void testWhole() throws Exception {
        assertEquals(adapter.getLatches().size() , 0);
        Observable<Integer> cachedWsd = adapter.cachedObservable(getOrigin(2), "wsd", 2000);

        final long[] using = {0, 0};

        final CountDownLatch latch = new CountDownLatch(2);
        final long startTime = System.currentTimeMillis();
        cachedWsd.subscribeOn(Schedulers.io()).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                using[0] = System.currentTimeMillis() - startTime;
                System.out.println("whole 第一次耗时 => " + using[0]);
                assertEquals((int)integer, 7);
                latch.countDown();
            }
        });

        Thread.sleep(100);
        final long startTime2 = System.currentTimeMillis();
        cachedWsd.subscribeOn(Schedulers.io()).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                using[1] = System.currentTimeMillis() - startTime2;

                System.out.println("whole 第二次耗时 => " + using[1]);
                assertEquals((int)integer, 7);
                latch.countDown();
            }
        });

        latch.await();
        latch.await(400, TimeUnit.MILLISECONDS);
        long endTime = System.currentTimeMillis();
        assertTrue(endTime - startTime <= 400);
        assertTrue(using[0] > 200);
        assertTrue(using[1] > 100);
        assertTrue(using[1] < 200);

        long start3 = System.currentTimeMillis();
        Integer single = cachedWsd.toBlocking().single();
        long using3 = System.currentTimeMillis() - start3;
        System.out.println("while 第三次耗时 => " + using3);
        assertTrue(using3 < 20);
        assertEquals((int)single, 7);

        Thread.sleep(2000);
        long start4 = System.currentTimeMillis();
        Integer single4 = cachedWsd.toBlocking().single();
        long using4 = System.currentTimeMillis() - start4;
        System.out.println("while 第四次耗时 => " + using4);
        assertTrue(using4 > 200);
        assertEquals((int)single, 7);
    }

    private Observable<Integer> getOrigin(final int a) {
        return Observable.just(null).map(new Func1<Object, Integer>() {
            @Override
            public Integer call(Object o) {
                adapter.addToLatches("ccf");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return a + 5;
            }
        });
    }
    @Test
    public void testOrigin() {
        long start0 = System.currentTimeMillis();
        Integer single = getOrigin(5).toBlocking().single();
        assertEquals((int)single, 10);
        long us = System.currentTimeMillis() - start0 - 200;
        System.out.println("origin test using time => " + us);
        assertTrue(us < 20 && us > -20);
    }

    @Test
    public void testSecondLoadFromCacheOrOrigin() throws Exception {

        final long[] using = {0, 0};
        final long startTime = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(2);
        adapter.secondLoadFromCacheOrOrigin(getOrigin(1), "ccf", 99999)
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        assertEquals(6, (int)integer);
                        using[0] = System.currentTimeMillis() - startTime;
                        System.out.println("并发请求1结束 => " + using[0]);
                        adapter.getCacheCore().saveCache("ccf", integer, 99999);
                        adapter.removeFromLatches("ccf");
                        latch.countDown();
                    }
                });


        Thread.sleep(100);
        final long startTime2 = System.currentTimeMillis();
        adapter.secondLoadFromCacheOrOrigin(getOrigin(1), "ccf", 99999)
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        assertEquals(6, (int)integer);
                        using[1] = System.currentTimeMillis() - startTime2;
                        System.out.println("并发请求2结束 => " + using[1]);
                        latch.countDown();
                    }
                });
        latch.await(400, TimeUnit.MILLISECONDS);
        assertTrue(System.currentTimeMillis() - startTime > 200);
        assertTrue(System.currentTimeMillis() - startTime < 400);
        assertNotEquals(using[1],using[0]);
        assertTrue(using[1] + 40 < using[0]);


        int size = adapter.getLatches().size();
        assertEquals(size , 0);
        final long startTime3 = System.currentTimeMillis();
        Integer ccf = adapter.secondLoadFromCacheOrOrigin(getOrigin(1), "ccf", 99999).toBlocking().single();
        long t3 = System.currentTimeMillis() - startTime3;
        System.out.println("第三次缓存请求结束 => " + t3);
        assertEquals(6, (int)ccf);
        assertTrue(t3 < 15);

        final long startTime4 = System.currentTimeMillis();
        Integer wsd = adapter.secondLoadFromCacheOrOrigin(getOrigin(1), "wsd", 99999).toBlocking().single();
        long t4 = System.currentTimeMillis() - startTime4;
        System.out.println("第四次无缓存请求结束 => " + t4);
        assertEquals(6, (int)wsd);
        assertTrue(t4 >= 200);
        //downLatch.await();
    }

    @Test
    public void testRemoveFromLatches() throws Exception {
        testAddToLatches();

        CountDownLatch latch = adapter.getLatches().get("ccf");
        assertNotNull(latch);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                adapter.removeFromLatches("wsd");
                assertTrue(adapter.getLatches().size() == 1);
                adapter.removeFromLatches("ccf");
                assertTrue(adapter.getLatches().size() == 0);
                adapter.removeFromLatches("ccf");
                assertTrue(adapter.getLatches().size() == 0);
            }
        }).start();
        long now = System.currentTimeMillis();
        latch.await(3, TimeUnit.SECONDS);
        assertTrue(System.currentTimeMillis() - now < 3000);
    }

    @Test
    public void testAddToLatches() throws Exception {
        CountDownLatch ccf = adapter.addToLatches("ccf");
        assertTrue(adapter.getLatches().containsValue(ccf));
        assertTrue(adapter.getLatches().containsKey("ccf"));

        CountDownLatch ccf2 = adapter.addToLatches("ccf");
        assertEquals(ccf, ccf2);
        assertTrue(adapter.getLatches().size() == 1);
    }

    @Test
    public void testLoadFromCache() throws Exception {
        adapter.saveToCache(Observable.just("ccf"), "ccf", 1000).subscribe();
        adapter.loadFromCache("ccf", 1000).subscribe(new Action1<Object>() {
            @Override
            public void call(Object ccf) {
                assertNotNull(ccf);
                assertEquals(ccf, "ccf");
            }
        });
        Thread.sleep(1200);
        adapter.loadFromCache("ccf", 1000).subscribe(new Action1<Object>() {
            @Override
            public void call(Object ccf) {
                assertNull(ccf);
            }
        });
    }

    @Test
    public void testSaveToCache() throws Exception {
        adapter.saveToCache(Observable.just("ccf"), "ccf", 1000).subscribe();
        Object ccf = adapter.getCacheCore().loadCache("ccf", 1000);
        assertNotNull(ccf);
        assertEquals(ccf, "ccf");
        Thread.sleep(1200);
        ccf = adapter.getCacheCore().loadCache("ccf", 1000);
        assertNull(ccf);
    }

    @Test
    public void testWaitIfNeed() throws Exception {
        testAddToLatches();
        long startTime = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                adapter.removeFromLatches("ccf");
            }
        }).start();
        adapter.waitIfNeed("ccf").subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                assertTrue(aBoolean);
                //assertFalse(aBoolean);
            }
        });
        long wucha = System.currentTimeMillis() - startTime - 1000;
        System.out.println(wucha + " ms");
        assertTrue(wucha < 15 && wucha > - 15);
    }

    @Test
    public void testWaitIfNeed2() throws Exception {
        //testAddToLatches();
        long startTime = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                adapter.removeFromLatches("wsd");
            }
        }).start();

        adapter.waitIfNeed("ccf").subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                assertFalse(aBoolean);
            }
        });
        long wucha = System.currentTimeMillis() - startTime;
        System.out.println(wucha + " ms");
        assertTrue(wucha < 15 && wucha > - 15);
    }
}