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
    private final Set<FieldInjector> fields;
    private static final String SUFFIX = "$$Maintain";

    public ClassInjector(String classPackage, String className) {
        this.classPackage = classPackage;
        this.originClassName = className;
        this.className = className + SUFFIX;
        this.fields = new LinkedHashSet<FieldInjector>();
    }

    public void addField(FieldInjector e) {
        fields.add(e);
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
        builder.append("import com.github.bluzwong.myflux.lib.*;\n");
        builder.append("import java.util.ArrayList;\n");
        builder.append("import java.util.List;\n");

        builder.append("public class ").append(this.className).append(" implements IMaintain {\n");

        builder.append("@Override public  void autoSave(Object obj, SavedData savingData) {\n"); // start of method
        builder.append("if (obj == null || savingData == null) {\n");
        builder.append("return;\n}\n");
        builder.append(originClassName).append(" target = (").append(originClassName).append(") obj;\n");
        for (FieldInjector methodInjector : fields) {
            builder.append(methodInjector.brewJava());
        }

        builder.append("}\n"); // end of method

        // method restore
        builder.append("@Override public  void autoRestore(Object obj, SavedData savedData)  {\n"); // start of method
        builder.append("if (obj == null || savedData == null) {\n");
        builder.append("return;\n}\n");

        builder.append(originClassName).append(" target = (").append(originClassName).append(") obj;\n");
        for (FieldInjector methodInjector : fields) {
            builder.append(methodInjector.brewJavaRestore());
        }
        builder.append("}\n"); // end of method
        builder.append("}\n"); // end of class
        return builder.toString();
    }
}
