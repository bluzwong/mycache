package com.github.bluzwong.mycache_processor;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/9/24.
 */
public class MethodInjector {
    private String funcName = "", returnType = "", params = "", signature = "", typeParams = "",
            needMem = "", needDisk = "",
            memTimeout = "", diskTimeout = "";

    private String originCallBack = "";
    private String callBackCls = "", callBackFunc = "", callBackParam = "";

    public void setCallBackCls(String callBackCls) {
        this.callBackCls = callBackCls;
    }

    public void setCallBackFunc(String callBackFunc) {
        this.callBackFunc = callBackFunc;
    }

    public void setCallBackParam(String callBackParam) {
        this.callBackParam = callBackParam;
    }

    public void setOriginCallBack(String originCallBack) {
        this.originCallBack = originCallBack;
    }

    private boolean isStatic;

    public MethodInjector(String funcName, boolean isStatic, String returnType, String params, String typeParams, String signature, String needMem, String needDisk, String memTimeout, String diskTimeout) {
        this.funcName = funcName;
        this.returnType = returnType;
        this.params = params;
        this.signature = funcName + "." + signature.replace(" ", "..").replace(",", "...");
        this.typeParams = typeParams;
        this.needMem = needMem;
        this.needDisk = needDisk;
        this.memTimeout = memTimeout;
        this.diskTimeout = diskTimeout;
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
        if (!typeParams.equals("")) {
            builder.append(", ");
        }
        builder.append(typeParams).append(") {\n");
        boolean isSync = false;
        boolean isAsync = false;
        if (returnType.startsWith("rx.Observable<")) {
            builder.append("return CacheHelper.getCachedMethod(");
        } else if (!returnType.equals("void")) {
            isSync = true;
            builder.append("return (").append(returnType).append(")CacheHelper.getCachedMethodSync(");
        } else {
            // no known return type found
            isAsync = true;
            builder.append("final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);\n" +
                    "        final Object[] resultObj = new Object[1];\n" +
                    "        final ");
            builder.append(callBackCls).append(" myCallBack = new ").append(callBackCls)
                    .append("() {\n" +
                            "            @Override\n" +
                            "            public void ").append(callBackFunc)
                    .append("(").append(callBackParam).append(" result) {\n" +
                    "                resultObj[0] = result;\n" +
                    "                latch.countDown();\n" +
                    "            }\n" +
                    "        };");
            builder.append("new Thread(new Runnable() {\n" +
                    "            @Override\n" +
                    "            public void run() {\n" +
                    "                final Object objAfterCache = CacheHelper.getCachedMethodSync(new CacheHelper.Fun1() {\n" +
                    "                    @Override\n" +
                    "                    public Object func() {");
        }
        StringBuilder firstParam = new StringBuilder();

        if (isSync) {
            firstParam.append("new CacheHelper.Fun1() {\n" +
                    "    @Override\n" +
                    "    public Object func() {\n" +
                    "        return ");
        }

        if (isStatic) {
            firstParam.append(originClass);
        } else {
            firstParam.append("target");
        }
        if (isAsync) {
            firstParam.append(".").append(funcName).append("(").append(params.replace("${myCallBack}", "myCallBack ")).append(");");
        } else {
            firstParam.append(".").append(funcName).append("(").append(params).append(")");
        }
        if (isSync) {
            firstParam.append(";\n    }}");
        }
        if (isAsync) {
            builder.append(firstParam.toString())
                    .append("try {\n" +
                            "                            latch.await();\n" +
                            "                        } catch (InterruptedException e) {\n" +
                            "                            e.printStackTrace();\n" +
                            "                        }\n" +
                            "                        return resultObj[0];\n" +
                            "                    }\n" +
                            "                }, ");
            builder.append("\"").append(originClass).append(".").append(signature).append("\", ").append(needMem).append(", ")
                    .append(memTimeout).append(", ").append(needDisk).append(", ").append(diskTimeout)
                    .append(");");
            builder.append("new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {\n" +
                    "                    @Override\n" +
                    "                    public void run() {\n" +
                    "                        " + originCallBack + "." + callBackFunc + "((" + callBackParam + ") objAfterCache);\n" +
                    "                    }\n" +
                    "                });\n" +
                    "            }\n" +
                    "        }).start();");

        } else {
            builder.append(firstParam.toString())
                    .append(", \"").append(originClass).append(".").append(signature).append("\", ").append(needMem).append(", ")
                    .append(memTimeout).append(", ").append(needDisk).append(", ").append(diskTimeout)
                    .append(");");

        }
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
