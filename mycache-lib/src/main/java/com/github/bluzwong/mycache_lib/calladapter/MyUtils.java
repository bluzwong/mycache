package com.github.bluzwong.mycache_lib.calladapter;

import android.util.Log;
import com.github.bluzwong.mycache_lib.CacheUtil;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    static String getMD5(String info) {
        return getMD5(info.getBytes());
    }


    static String getMD5(byte[] info) {
        if (null == info || info.length == 0) {
            return null;
        }
        StringBuilder buf = new StringBuilder("");
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        md.update(info);
        byte b[] = md.digest();
        int i;

        for (int offset = 0; offset < b.length; offset++) {
            i = b[offset];
            if (i < 0)
                i += 256;
            if (i < 16)
                buf.append("0");
            buf.append(Integer.toHexString(i));
        }
        return buf.toString();
    }


    static void cacheLog(String msg) {
        cacheLog(msg, -1);
    }

    static void logWarn(String msg) {
        if (CacheUtil.isNeedLog()) {
            Log.w("mycache", msg);
        }
    }

    static void cacheLog(String msg, long startTime) {
        if (CacheUtil.isNeedLog()) {
            if (startTime > 0) {
                long t = System.currentTimeMillis() - startTime;
                msg = "[" + t + "ms] " + msg;
                Log.i("mycache", msg);
            } else {
                Log.d("mycache", msg);
            }
        }
    }
}
