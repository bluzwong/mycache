package com.github.bluzwong.mycache_lib.calladapter;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Created by bluzwong on 2016/1/30.
 */
public class MyUtils {
    static Request buildRequestFromCall(Call call){
        try {
            Field argsField = call.getClass().getDeclaredField("args");
            argsField.setAccessible(true);
            Object[] args = (Object[]) argsField.get(call);

            Field requestFactoryField = call.getClass().getDeclaredField("requestFactory");
            requestFactoryField.setAccessible(true);
            Object requestFactory = requestFactoryField.get(call);
            Method createMethod = requestFactory.getClass().getDeclaredMethod("create", Object[].class);
            createMethod.setAccessible(true);
            return (Request) createMethod.invoke(requestFactory, new Object[]{args});
        }catch(Exception exc){
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    static <T> Converter<ResponseBody, T> getResponseConverter(Retrofit retrofit, Type dataType, Annotation[] annotations) {
        for(Converter.Factory factory : retrofit.converterFactories()) {
            if (factory == null) continue;
            Converter<ResponseBody, T> converter =
                    (Converter<ResponseBody, T>) factory.responseBodyConverter(dataType, annotations, retrofit);

            if (converter != null) {
                return converter;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T> Converter<T, RequestBody> getRequestConverter(Retrofit retrofit, Type dataType, Annotation[] annotations) {
        for(Converter.Factory factory : retrofit.converterFactories()){
            if(factory == null) continue;
            Converter<T, RequestBody> converter =
                    (Converter<T, RequestBody>) factory.requestBodyConverter(dataType, annotations, retrofit);

            if (converter != null) {
                return converter;
            }
        }
        return null;
    }
    static<T> byte[] getRawResponseFromEntity(T t, Retrofit retrofit, Type dataType, Annotation[] annotations) {
        Converter<T, RequestBody> converter = getRequestConverter(retrofit, dataType, annotations);
        if (converter == null) {
            return null;
        }
        try {
            RequestBody requestBody = converter.convert(t);
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            return buffer.readByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static <T> T getEntityFromResponse(byte[] rawResponse, Retrofit retrofit, Type dataType, Annotation[] annotations) {
        Converter<ResponseBody, T> converter = getResponseConverter(retrofit, dataType, annotations);
        if (converter == null) {
            return null;
        }
        try {
            return converter.convert(ResponseBody.create(null, rawResponse));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
