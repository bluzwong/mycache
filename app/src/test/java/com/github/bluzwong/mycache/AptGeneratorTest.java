package com.github.bluzwong.mycache;

import android.content.Context;
import com.github.bluzwong.mycache_lib.*;
import com.github.bluzwong.mycache_lib.functioncache.RxCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = com.github.bluzwong.mycache.BuildConfig.class)
public class AptGeneratorTest {


    Context context;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.application;
        //adapter.setCacheCore(new EasyRxCacheCore());

    }



    @Test
    public void testOneArgNotStatic() throws Exception {

    }

    @Test
    public void testTwoArgNotStatic() throws Exception {

    }

    @Test
    public void testOneArg() throws Exception {

    }

    @Test
    public void testTwoArg() throws Exception {

    }
}