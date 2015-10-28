package com.github.bluzwong.mycache_processor;


import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/9/24.
 */
@SupportedAnnotationTypes({"com.github.bluzwong.myflux.lib.Maintain", "com.github.bluzwong.myflux.lib.MaintainProperty"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class AnnotationProcessor extends AbstractProcessor{


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
        /*for (TypeElement te : annotations) {
            // te = zhujie
            log("size " + annotations.size());
            for (Element e : roundEnv.getElementsAnnotatedWith(te)) {
                //log("work on -> " + e.toString());
                Name name = e.getSimpleName();
                log("name -> " + name); //
                Element enclosingElement = e.getEnclosingElement();
                log("element -> " +enclosingElement); //

//                log("simplename" + e.getSimpleName());
            }
        }
*/
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

    private Map<TypeElement, ClassInjector> findAndParseTargets(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<TypeElement, ClassInjector> targetClassMap = new LinkedHashMap<TypeElement, ClassInjector>();

        for (TypeElement te : annotations) {
            // te = zhujie
            String annoName = te.getSimpleName().toString();
            for (Element e : roundEnv.getElementsAnnotatedWith(te)) {
                //log("work on -> " + e.toString());
                Name fieldName = e.getSimpleName();
                log("fieldName -> " + fieldName); //
                TypeElement className = (TypeElement) e.getEnclosingElement();
                log("fieldInClass -> " + className); //
                log("fieldType -> " + e.asType().toString());
//                log("simplename" + e.getSimpleName());
                ClassInjector injector = getOrCreateTargetClass(targetClassMap, className);
                boolean isProperty = false;
                if (annoName.equals("MaintainProperty")) {
                    isProperty = true;
                }
                FieldInjector methodInjector = new FieldInjector(fieldName.toString(), e.asType().toString(), isProperty);
                injector.addField(methodInjector);
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
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }
}
