package com.github.bluzwong.mycache_lib.calladapter;

import android.app.Activity;
import com.github.bluzwong.mycache_lib.calladapter.model.ValueIndex;
import com.github.bluzwong.mycache_lib.calladapter.model.WebApi;
import com.github.bluzwong.mycache_lib.BuildConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.*;

/**
 * Created by Bruce-Home on 2016/1/31.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class SimpleCacheCallAdapterTest {

    WebApi.MyService service;

    @Before
    public void setUp() throws Exception {
        WebApi api = new WebApi();
        api.init(RuntimeEnvironment.application);
        service = api.myService;
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testAdapt() throws Exception {
        ValueIndex index = service.getUrls().toBlocking().single();
        assertEquals("http://mt58866.xicp.net:66/value1.php", index.getUrl_value1());
    }
}