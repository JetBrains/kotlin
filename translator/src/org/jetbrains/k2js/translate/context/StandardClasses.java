package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.resolve.DescriptorRenderer.getFQName;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.*;

/**
 * @author Talanov Pavel
 */
public final class StandardClasses {

    @NotNull
    public static StandardClasses bindImplementations(@NotNull JetStandardLibrary standardLibrary,
                                                      @NotNull JsScope kotlinObjectScope) {
        StandardClasses standardClasses = new StandardClasses(kotlinObjectScope);

        bindArray(standardClasses, standardLibrary);
        ClassDescriptor iteratorClass = (ClassDescriptor)
                standardLibrary.getLibraryScope().getClassifier("Iterator");
        bindIterator(standardClasses, iteratorClass);
        return standardClasses;
    }

    private static void bindIterator(StandardClasses standardClasses,
                                     ClassDescriptor iteratorClass) {
        standardClasses.declareStandardTopLevelObject(iteratorClass, "ArrayIterator");
        FunctionDescriptor nextFunction =
                getFunctionByName(iteratorClass.getDefaultType().getMemberScope(), "next");
        standardClasses.declareStandardMethodOrProperty(iteratorClass, nextFunction, "next");
        PropertyDescriptor hasNextProperty =
                getPropertyByName(iteratorClass.getDefaultType().getMemberScope(), "hasNext");
        standardClasses.declareStandardMethodOrProperty(iteratorClass, hasNextProperty, "hasNext");
    }

    private static void bindArray(@NotNull StandardClasses standardClasses,
                                  @NotNull JetStandardLibrary standardLibrary) {
        ClassDescriptor arrayClass = standardLibrary.getArray();
        standardClasses.declareStandardTopLevelObject(arrayClass, "Array");
        FunctionDescriptor nullConstructorFunction = getFunctionByName(standardLibrary.getLibraryScope(), "Array");
        standardClasses.declareStandardTopLevelObject(nullConstructorFunction, "array");
        PropertyDescriptor sizeProperty =
                getPropertyByName(arrayClass.getDefaultType().getMemberScope(), "size");
        standardClasses.declareStandardMethodOrProperty(arrayClass, sizeProperty, "size");
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

    private void declareStandardTopLevelObject(@NotNull DeclarationDescriptor descriptor,
                                               @NotNull String kotlinLibName) {
        declareStandardTopLevelObject(getFQName(descriptor), kotlinLibName);
    }

    private void declareStandardTopLevelObject(@NotNull String fullQualifiedName, @NotNull String kotlinLibName) {
        topLevelNameMap.put(fullQualifiedName, kotlinScope.declareName(kotlinLibName));
        scopeMap.put(fullQualifiedName, new JsScope(kotlinScope, "class " + kotlinLibName));
        methodNameMap.put(fullQualifiedName, new HashMap<String, JsName>());
    }

    private void declareStandardMethodOrProperty(@NotNull DeclarationDescriptor topLevelDescriptor,
                                                 @NotNull DeclarationDescriptor innerDescriptor,
                                                 @NotNull String kotlinLibName) {
        declareStandardMethodOrProperty(getFQName(topLevelDescriptor), innerDescriptor.getName(), kotlinLibName);
    }

    private void declareStandardMethodOrProperty(@NotNull String fullQualifiedClassName, @NotNull String methodOrPropertyName,
                                                 @NotNull String kotlinLibName) {
        JsScope classScope = scopeMap.get(fullQualifiedClassName);
        Map<String, JsName> classMethodsMap = methodNameMap.get(fullQualifiedClassName);
        classMethodsMap.put(methodOrPropertyName, classScope.declareName(kotlinLibName));
    }

    //TODO: refactor
    public boolean isStandardObject(@NotNull DeclarationDescriptor descriptor) {
        if (canBeTopLevelObject(descriptor)) {
            if (topLevelNameMap.containsKey(getFQName(descriptor))) {
                return true;
            }
        }
        if (canBeInnerObject(descriptor)) {
            DeclarationDescriptor containing = getContainingDeclaration(descriptor);
            if (!isStandardObject(containing)) {
                return false;
            }
            Map<String, JsName> methodMapForClass = methodMapForDescriptor(containing);
            return methodMapForClass.containsKey(descriptor.getName());
        }
        return false;
    }

    private Map<String, JsName> methodMapForDescriptor(DeclarationDescriptor containing) {
        return methodNameMap.get(getFQName(containing));
    }

    private boolean canBeInnerObject(DeclarationDescriptor descriptor) {
        return (descriptor instanceof FunctionDescriptor) || (descriptor instanceof PropertyDescriptor);
    }

    private boolean canBeTopLevelObject(DeclarationDescriptor descriptor) {
        return (descriptor instanceof ClassDescriptor) || (descriptor instanceof FunctionDescriptor);
    }

    @NotNull
    public JsName getStandardObjectName(@NotNull DeclarationDescriptor descriptor) {
        if (canBeTopLevelObject(descriptor)) {
            JsName result = topLevelNameMap.get(getFQName(descriptor));
            if (result != null) {
                return result;
            }
        }
        if (canBeInnerObject(descriptor)) {
            DeclarationDescriptor containing = getContainingDeclaration(descriptor);
            Map<String, JsName> methodMapForClass = methodMapForDescriptor(containing);
            return methodMapForClass.get(descriptor.getName());
        }
        throw new AssertionError("Only classes and functions can be standard objects.");
    }
}
