package com.github.bluzwong.mycache_processor;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/9/24.
 */
public class FieldInjector {
    private String fieldName;
    private String type;
    private boolean isProperty;
    public FieldInjector(String fieldName, String type, boolean isProperty) {
        this.fieldName = fieldName;
        this.type = type;
        this.isProperty = isProperty;
    }

    public String brewJava() throws Exception{
        StringBuilder builder = new StringBuilder();
        String fixField = fieldName;
        if (isProperty) {
            fixField = "get" + firstLetterToUpper(fieldName) + "()";
        }
        builder.append("savingData.put(\"").append(fieldName).append("\", target.").append(fixField).append(");\n");

        return builder.toString();
    }

    public String brewJavaRestore()  {
        StringBuilder builder = new StringBuilder();

        String tmp = "savedData.get(\"" + fieldName + "\")";
        if (isProperty) {
            builder.append("target.set" + firstLetterToUpper(fieldName) + "(" + "( " + type + ")"+tmp+");\n");
        } else {
            builder.append("target." + fieldName).append("= ( " + type + ")"+tmp+";\n");
        }
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
