package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.resolve.DescriptorRenderer.getFQName;

/**
 * @author Talanov Pavel
 */
public final class StandardClasses {

    @NotNull
    public static StandardClasses bindImplementations(@NotNull JetStandardLibrary standardLibrary,
                                                      @NotNull JsScope kotlinScope) {
        StandardClasses standardClasses = new StandardClasses(kotlinScope);

        return standardClasses;
    }

    @NotNull
    private final JsScope kotlinScope;

    @NotNull
    private final Map<String, JsName> topLevelNameMap = new HashMap<String, JsName>();

    @NotNull
    private final Map<String, JsScope> scopeMap = new HashMap<String, JsScope>();

    @NotNull
    private final Map<String, Map<String, JsName>> methodNameMap = new HashMap<String, Map<String, JsName>>();


    private StandardClasses(@NotNull JsScope kotlinScope) {
        this.kotlinScope = kotlinScope;
    }

    private void declareStandardTopLevelObject(@NotNull String fullQualifiedName, @NotNull String kotlinLibName) {
        topLevelNameMap.put(fullQualifiedName, kotlinScope.declareName(kotlinLibName));
        scopeMap.put(fullQualifiedName, new JsScope(kotlinScope, "class " + kotlinLibName));
        methodNameMap.put(fullQualifiedName, new HashMap<String, JsName>());
    }

    private void declareStandardMethod(@NotNull String fullQualifiedClassName, @NotNull String methodName,
                                       @NotNull String kotlinLibName) {
        JsScope classScope = scopeMap.get(fullQualifiedClassName);
        Map<String, JsName> classMethodsMap = methodNameMap.get(fullQualifiedClassName);
        classMethodsMap.put(methodName, classScope.declareName(kotlinLibName));
    }

    //TODO: refactor
    public boolean isStandardObject(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            return topLevelNameMap.containsKey(getFQName(classDescriptor));
        }
        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
            DeclarationDescriptor containing = descriptor.getContainingDeclaration();
            assert containing != null : "Cannot have top level functions.";
            if (!isStandardObject(containing)) {
                return false;
            }
            Map<String, JsName> methodMapForClass = methodNameMap.get(getFQName(containing));
            return methodMapForClass.containsKey(functionDescriptor.getName());
        }
        return false;
    }

    @NotNull
    public JsName getStandardObjectName(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            return topLevelNameMap.get(getFQName(classDescriptor));
        }
        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
            DeclarationDescriptor containing = descriptor.getContainingDeclaration();
            assert containing != null : "Cannot have top level functions.";
            Map<String, JsName> methodMapForClass = methodNameMap.get(getFQName(containing));
            return methodMapForClass.get(functionDescriptor.getName());
        }
        throw new AssertionError("Only classes and functions can be standard objects.");
    }
}
