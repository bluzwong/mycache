package com.github.bluzwong.mycache_lib.calladapter;

import retrofit2.Call;
import retrofit2.CallAdapter;
import rx.Observable;
import rx.Single;

import java.lang.reflect.Type;

/**
 * Created by bluzwong on 2016/1/30.
 */
final class SingleHelper {
    static CallAdapter<Single<?>> makeSingle(final CallAdapter<Observable<?>> callAdapter) {
        return new CallAdapter<Single<?>>() {
            @Override public Type responseType() {
                return callAdapter.responseType();
            }

            @Override public <R> Single<?> adapt(Call<R> call) {
                Observable<?> observable = callAdapter.adapt(call);
                return observable.toSingle();
            }
        };
    }
}
