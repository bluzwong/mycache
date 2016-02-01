package com.github.bluzwong.mycache_processor;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/9/24.
 */
public class MethodInjector {
    private String funcName = "", returnType = "", params = "", signature = "", typeParams = "",

    memTimeout = "";



    private boolean isStatic;

    public MethodInjector(String funcName, boolean isStatic, String returnType, String params, String typeParams, String signature, String memTimeout) {
        this.funcName = funcName;
        this.returnType = returnType;
        this.params = params;
        this.signature = funcName + "-" + signature.replace(" ", "_").replace(",", "-");
        this.typeParams = typeParams;
        this.memTimeout = memTimeout;
        this.isStatic = isStatic;
    }

    public String brewJava(String originClass) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("public static ")
                .append(returnType).append(" ")
                .append(funcName).append("(");
        if (!isStatic) {
            builder.append("final ").append(originClass).append(" target");
        }
        if (!typeParams.equals("") && !isStatic) {
            builder.append(", ");
        }
        builder.append(typeParams).append(") {\n");
        if (returnType.startsWith("rx.Observable<")) {
            builder.append("return com.github.bluzwong.mycache_lib.functioncache.RxCacheAdapter.INSTANCE.cachedObservable(");
        }
        StringBuilder firstParam = new StringBuilder();


        if (isStatic) {
            firstParam.append(originClass);
        } else {
            firstParam.append("target");
        }

        firstParam.append(".").append(funcName).append("(").append(params).append(")");


        builder.append(firstParam.toString())
                .append(", \"")
                ;
        if (isStatic) {
            builder.append("static_");
        }
        builder.append(originClass).append(".").append(signature).append("\", ")
                .append(memTimeout)
                .append(");");


        builder.append("\n}\n");

        return builder.toString();
    }

    public static void main(String[] args) {
        Map<String, String> nameTypes = new HashMap<String, String>();
        String s = "aaaa";
        Object o = s;
        String fieldName = "";
        Class clz = o.getClass();

        // String aaa = clz.cast(o);
        //System.out.println(aaa);
        nameTypes.put(fieldName, s.getClass().getCanonicalName());
    }

    public static String firstLetterToUpper(String str) {
        char[] array = str.toCharArray();
        array[0] -= 32;
        return String.valueOf(array);
    }
}
