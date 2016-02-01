package com.github.bluzwong.mycache_lib.functioncache;

import android.content.Context;
import com.github.bluzwong.mycache_lib.BaseCacheCore;
import com.github.bluzwong.mycache_lib.BuildConfig;
import com.github.bluzwong.mycache_lib.functioncache.data.Person;
import com.github.bluzwong.mycache_lib.functioncache.data.TestDataGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Created by bluzwong on 2016/2/1.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class SimpleRxCacheCoreTest {

    SimpleRxCacheCore cacheCore;
    Context context;
    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.application;
        cacheCore = SimpleRxCacheCore.create(context);
        cacheCore.getPreferences().edit().clear().commit();
        cacheCore.getMemoryCache().evictAll();
        cacheCore.getBook().destroy();
        cacheCore.setWillLoad(new BaseCacheCore.WillLoad() {
            @Override
            public boolean shouldLoad(String key) {
                if (key.contains("no load")) {
                    return false;
                }
                return true;
            }
        });
        cacheCore.setWillSave(new BaseCacheCore.WillSave() {
            @Override
            public boolean shouldSave(String key, Object object, long timeOut) {
                if (key.contains("no save")) {
                    return false;
                }
                return true;
            }
        });
    }

    @Test
    public void testCreate() throws Exception {
        assertNotNull(cacheCore);
        assertNotNull(cacheCore.getPreferences());
        assertNotNull(cacheCore.getMemoryCache());
    }

    @Test
    public void testSaveCache() throws Exception {
        long startTime = System.currentTimeMillis();
        cacheCore.saveCache("ccf", "ccf-obj", 500);
        String ccfKey = "ccf";
        BaseCacheCore.TimeAndObject timeAndObject = cacheCore.getMemoryCache().get(ccfKey);
        assertNotNull(timeAndObject);
        assertNotNull(timeAndObject.object);
        assertEquals("ccf-obj", timeAndObject.object);
        assertTrue(timeAndObject.expireTime > startTime);
        assertTrue(timeAndObject.expireTime < startTime + 600);

        long ccf = cacheCore.getPreferences().getLong(ccfKey, -1);
        assertNotEquals(ccf, -1);
        assertTrue(ccf > startTime);
        assertTrue(ccf < startTime + 600);

        assertTrue(cacheCore.getBook().exist(ccfKey));

        Object readFromBook = cacheCore.getBook().read(ccfKey);
        assertNotNull(readFromBook);
        assertEquals("ccf-obj", readFromBook);

        cacheCore.saveCache("ccf-no save", "ccf-obj-nosave", 1000);
        String keyNoSave = "ccf-no save";
        long nosaveTimeOut = cacheCore.getPreferences().getLong(keyNoSave, -1);
        assertEquals(nosaveTimeOut, -1);

        assertFalse(cacheCore.getBook().exist(keyNoSave));
    }

    @Test
    public void testLoadCache() throws Exception {
        cacheCore.saveCache("ccf", "ccf-obj", 500);
        Object ccfFromLoadCache = cacheCore.loadCache("ccf", 500);
        assertNotNull(ccfFromLoadCache);
        assertEquals("ccf-obj", ccfFromLoadCache);

        Thread.sleep(500);
        ccfFromLoadCache = cacheCore.loadCache("ccf", 500);
        assertNull(ccfFromLoadCache);

        String keyNoLoad = "ccf-no load";
        cacheCore.saveCache(keyNoLoad, "ccf-obj", 500);

        Object loadCache = cacheCore.loadCache(keyNoLoad, 500);
        assertNull(loadCache);
    }

    @Test
    public void testTwice() {
        cacheCore.saveCache("ccf", "wsd", 5000);
        cacheCore.saveCache("ccf", "wsd2", 5000);
        int size = cacheCore.getMemoryCache().size();
        assertEquals(size, 1);
        BaseCacheCore.TimeAndObject ccf1 = cacheCore.getMemoryCache().get("ccf");
        assertEquals(ccf1.object, "wsd2");
        boolean ccfExists = cacheCore.getBook().exist("ccf");
        assertTrue(ccfExists);
        Object ccf = cacheCore.getBook().read("ccf");
        assertEquals(ccf, "wsd2");
    }

    @Test
    public void testClearAll() {
        cacheCore.saveCache("ccf", "wsd", 5000);
        cacheCore.saveCache("ccf2", "wsd2", 5000);
        int size = cacheCore.getMemoryCache().size();
        assertEquals(size, 2);
        cacheCore.clearAll();

        size = cacheCore.getMemoryCache().size();
        assertEquals(size, 0);
        assertFalse(cacheCore.getBook().exist("ccf"));
        assertFalse(cacheCore.getBook().exist("ccf2"));
    }

    @Test
    public void testClearMemory() {
        cacheCore.saveCache("ccf", "wsd", 5000);
        cacheCore.saveCache("ccf2", "wsd2", 5000);
        int size = cacheCore.getMemoryCache().size();
        assertEquals(size, 2);
        cacheCore.clearMemoryCache();
        size = cacheCore.getMemoryCache().size();
        assertEquals(size, 0);
        assertTrue(cacheCore.getBook().exist("ccf"));
        assertTrue(cacheCore.getBook().exist("ccf2"));
    }

    @Test
    public void testClearDisk() {
        cacheCore.saveCache("ccf", "wsd", 5000);
        cacheCore.saveCache("ccf2", "wsd2", 5000);
        assertTrue(cacheCore.getBook().exist("ccf"));
        assertTrue(cacheCore.getBook().exist("ccf2"));
        cacheCore.clearDiskCache();
        assertFalse(cacheCore.getBook().exist("ccf"));
        assertFalse(cacheCore.getBook().exist("ccf2"));
        int size = cacheCore.getMemoryCache().size();
        assertEquals(size, 2);
    }

    @Test
    public void testNestedClass() {
        Person origin = TestDataGenerator.genPerson(2);
        cacheCore.saveCache("nested", origin, 5000);

        Object load = cacheCore.loadCache("nested", 5000);
        assertEquals(origin, load);
    }
}