package com.github.bluzwong.mycache_processor;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by geminiwen on 15/5/21.
 */
public class ClassInjector {

    private final String classPackage;
    private final String className;
    private final String originClassName;
    private final Set<MethodInjector> methods;
    private static final String SUFFIX = "Cached";

    public ClassInjector(String classPackage, String className) {
        this.classPackage = classPackage;
        this.originClassName = className;
        this.className = className + SUFFIX;
        this.methods = new LinkedHashSet<MethodInjector>();
    }

    public void addMethod(MethodInjector e) {
        methods.add(e);
    }

    public String getFqcn() {
        return classPackage + "." + className;
    }

    public static void main(String[] args) {
        String ccf = "";
        Map<String, String> nameTypes = new HashMap<String, String>();

    }
    public String brewJava() throws Exception {
        StringBuilder builder = new StringBuilder("package " + this.classPackage + ";\n");
        builder.append("import com.github.bluzwong.mycache_lib.*;\n");
        builder.append("public class ").append(this.className).append(" {\n");
        for (MethodInjector method : methods) {
            builder.append(method.brewJava(originClassName));
        }
        builder.append("}\n"); // end of class
        return builder.toString();
    }
}
