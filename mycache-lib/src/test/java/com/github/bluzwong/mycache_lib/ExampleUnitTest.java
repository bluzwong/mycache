package com.github.bluzwong.mycache_lib;

import org.junit.Test;
import rx.Observable;
import rx.Subscriber;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        int single = getNumOb(5).toBlocking().single();
        assertEquals(single, 10);
    }
    private int getNum(int i) throws InterruptedException {
        Thread.sleep(2000);
        return i + 5;
    }
    private Observable<Integer> getNumOb(final int i ) {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                try {
                    subscriber.onNext(getNum(i));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        });
    }
}