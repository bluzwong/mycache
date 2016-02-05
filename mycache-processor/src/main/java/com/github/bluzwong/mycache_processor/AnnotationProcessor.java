package com.github.bluzwong.mycache_processor;



import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/9/24.
 */
@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor{
    private static boolean needLog = true;

    private Filer filer;
    private Elements elementUtils;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        filer = env.getFiler();
        elementUtils = env.getElementUtils();
        typeUtils = env.getTypeUtils();
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add("com.github.bluzwong.mycache_lib.functioncache.RxCache");
        return types;
    }

    @Override public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * {@inheritDoc}
     *
     * @param annotations
     * @param roundEnv
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        log("start process ");
        Map<TypeElement, ClassInjector> targetClassMap = findAndParseTargets(annotations, roundEnv);
        for (Map.Entry<TypeElement, ClassInjector> entry : targetClassMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            ClassInjector injector = entry.getValue();
            try {
                String value = injector.brewJava();
                log(value);
                JavaFileObject jfo = filer.createSourceFile(injector.getFqcn(), typeElement);
                Writer writer = jfo.openWriter();
                writer.write(value);
                writer.flush();
                writer.close();
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), typeElement);
            }
        }

        return true;
    }

    private Map<TypeElement, ClassInjector> findAndParseTargets(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        Map<TypeElement, ClassInjector> targetClassMap = new LinkedHashMap<TypeElement, ClassInjector>();

        for (TypeElement te : annotations) {
            // te = zhujie
            String annoName = te.getQualifiedName().toString();
            log("process at => " + annoName);
            /*for (AnnotationMirror mirror : te.getAnnotationMirrors()) {
                log("anno mirror -> " + mirror); // @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
            }*/
            if (!getSupportedAnnotationTypes().contains(annoName)) {
                continue;
            }

            for (Element e : roundEnv.getElementsAnnotatedWith(te)) {
                //log("work on -> " + e.toString());
                Name fieldName = e.getSimpleName();
                log("fieldName -> " + fieldName); //  fieldName -> testFunc

                TypeElement className = (TypeElement) e.getEnclosingElement();
                String funcName = fieldName.toString();
                String timeOut = "0";
                for (AnnotationMirror mirror : e.getAnnotationMirrors()) {
                    //log("annotation mirror -> " + mirror); // annotation mirror -> @com.github.bluzwong.mycache_lib.Cache(time=true)
                    //log("anno type -> " + mirror.getAnnotationType());
                    //log("anno values -> " + mirror.getElementValues());
                    Set<? extends ExecutableElement> set = mirror.getElementValues().keySet();
                    for (ExecutableElement element : set) {
                        log("key set -> " + element); //key set -> time()
                        log("key value ->" + mirror.getElementValues().get(element)); // key value ->true
                        if (element.toString().equals("timeOut()")) {
                            timeOut = mirror.getElementValues().get(element).toString();
                        }
                    }
                }
                ExecutableElement executableElement = (ExecutableElement) e;
                TypeMirror returnType = executableElement.getReturnType();
                log("fieldInClass -> " + className); //  fieldInClass -> com.github.bluzwong.mycache.MainActivity
                log("fieldType -> " + e.asType().toString()); //  fieldType -> (int,boolean,float)int
                //log("type kind -> " + e.asType().getKind());// type kind -> EXECUTABLE

                StringBuilder paramsBuilder = new StringBuilder();
                StringBuilder paramsTypeBuilder = new StringBuilder();
                StringBuilder signatureBuilder = new StringBuilder();
                signatureBuilder.append(".").append(returnType);

                for (VariableElement element : executableElement.getParameters()) {
                    boolean ignored = false;
                    //boolean thisIsCallBack = false;
                    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                        log("params annot -> " + mirror); //mirror -> @com.github.bluzwong.mycache_lib.Ignore
                        if (mirror.getAnnotationType().toString().endsWith("com.github.bluzwong.mycache_lib.functioncache..Ignore")) {
                            ignored = true;
                        }

                    }

                    if (!ignored ) {
                        if (signatureBuilder.length() != 0) {
                            signatureBuilder.append(",");
                        }
                        signatureBuilder.append(element.asType().toString()).append(" \"+").append(element.toString()).append("+\"");
                        log("signature => " + signatureBuilder.toString());
                    }
                    log("params -> " + element); // params -> a
                    log("as type -> " + element.asType()); // as type -> java.util.List

                    if (paramsBuilder.length() != 0) {
                        paramsBuilder.append(",");
                    }
                    paramsBuilder.append(element.toString());

                    if (paramsTypeBuilder.length() != 0) {
                        paramsTypeBuilder.append(",");
                    }
                    paramsTypeBuilder.append("final ").append(element.asType().toString()).append(" ").append(element.toString());
                }
                // public static final
                boolean isStatic = false;
                for (Modifier modifier : e.getModifiers()) {
                    log("modifier -> " + modifier );
                    if (modifier.toString().equals("static")) {
                        isStatic = true;
                    }
                }

//                log("simplename" + e.getSimpleName());
                ClassInjector injector = getOrCreateTargetClass(targetClassMap, className);

                MethodInjector methodInjector = new MethodInjector(funcName, isStatic, returnType.toString()
                        , paramsBuilder.toString(), paramsTypeBuilder.toString(),signatureBuilder.toString(), timeOut);

                injector.addMethod(methodInjector);
            }

        }
        return targetClassMap;
    }
    /**
     *
     *
     * @param targetClassMap
     * @param enclosingElement
     * @return
     */

    private ClassInjector getOrCreateTargetClass(Map<TypeElement, ClassInjector> targetClassMap, TypeElement enclosingElement) {
        ClassInjector injector = targetClassMap.get(enclosingElement);
        if (injector == null) {
            String classPackage = getPackageName(enclosingElement);
            String className = getClassName(enclosingElement, classPackage);
            injector = new ClassInjector(classPackage, className);
            targetClassMap.put(enclosingElement, injector);
        }
        return injector;
    }

    /**
     *
     * @param type
     * @param packageName
     * @return
     */
    private String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    /**

     *
     * @param type
     * @return
     */
    private String getPackageName(TypeElement type) {
        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }
    private void log(String msg) {
        if (!needLog) {
            return;
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }
}
