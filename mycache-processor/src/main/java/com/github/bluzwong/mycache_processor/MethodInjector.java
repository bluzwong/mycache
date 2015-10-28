package com.github.bluzwong.mycache_processor;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/9/24.
 */
public class MethodInjector {
    private String funcName="",returnType = "", params = "",signature = "", typeParams = "",
            needMem="" , needDisk="",
            memTimeout="", diskTimeout="";

    private boolean isStatic;
    public MethodInjector(String funcName, boolean isStatic, String returnType, String params, String typeParams, String signature, String needMem, String needDisk, String memTimeout, String diskTimeout) {
        this.funcName = funcName;
        this.returnType = returnType;
        this.params = params;
        this.signature = funcName+"." + signature.replace(" ", "..").replace(",", "...");
        this.typeParams = typeParams;
        this.needMem = needMem;
        this.needDisk = needDisk;
        this.memTimeout = memTimeout;
        this.diskTimeout = diskTimeout;
        this.isStatic = isStatic;
    }

    public String brewJava(String originClass) throws Exception{
        StringBuilder builder = new StringBuilder();
        builder.append("public static ")
                .append(returnType).append(" ")
                .append(funcName).append("(");
        if (!isStatic) {
            builder.append(originClass).append(" target, ");
        }
        builder.append(typeParams).append(") {\n")
                .append("return CacheHelper.getCachedMethod(");

        if (isStatic) {
            builder.append(originClass);
        } else {
            builder.append("target");
        }
        builder.append(".").append(funcName).append("(").append(params).append(")")
                .append(", \"").append(originClass).append(".").append(signature).append("\", ").append(needMem).append(", ")
                .append(memTimeout).append(", ").append(needDisk).append(", ").append(diskTimeout)
                .append(");")

                .append("\n}\n");

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
    public static String firstLetterToUpper(String str){
        char[] array = str.toCharArray();
        array[0] -= 32;
        return String.valueOf(array);
    }
}
