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
        assert iteratorClass != null;
        bindIterator(standardClasses, iteratorClass);

        declareJavaArrayList(standardClasses);
        return standardClasses;
    }

    private static void declareJavaArrayList(@NotNull StandardClasses standardClasses) {
        String arrayListFQName = "<java_root>.java.util.ArrayList";
        standardClasses.declareStandardTopLevelObject(arrayListFQName, "ArrayList");
        standardClasses.declareStandardInnerDeclaration(arrayListFQName, "size", "size");
        standardClasses.declareStandardInnerDeclaration(arrayListFQName, "<init>", "ArrayList");
        standardClasses.declareStandardInnerDeclaration(arrayListFQName, "add", "add");
        standardClasses.declareStandardInnerDeclaration(arrayListFQName, "get", "get");
        standardClasses.declareStandardInnerDeclaration(arrayListFQName, "isEmpty", "isEmpty");
        standardClasses.declareStandardInnerDeclaration(arrayListFQName, "set", "set");
        standardClasses.declareStandardInnerDeclaration(arrayListFQName, "remove", "remove");
        standardClasses.declareStandardInnerDeclaration(arrayListFQName, "addAll", "addAll");
    }

    //TODO: more generic ways to declare standard classes
    private static void bindIterator(@NotNull StandardClasses standardClasses,
                                     @NotNull ClassDescriptor iteratorClass) {
        standardClasses.declareStandardTopLevelObject(iteratorClass, "ArrayIterator");
        FunctionDescriptor nextFunction =
                getFunctionByName(iteratorClass.getDefaultType().getMemberScope(), "next");
        standardClasses.declareStandardMethodOrProperty(nextFunction, "next");
        PropertyDescriptor hasNextProperty =
                getPropertyByName(iteratorClass.getDefaultType().getMemberScope(), "hasNext");
        standardClasses.declareStandardMethodOrProperty(hasNextProperty, "hasNext");
    }

    private static void bindArray(@NotNull StandardClasses standardClasses,
                                  @NotNull JetStandardLibrary standardLibrary) {
        ClassDescriptor arrayClass = standardLibrary.getArray();
        standardClasses.declareStandardTopLevelObject(arrayClass, "Array");
        FunctionDescriptor nullConstructorFunction = getFunctionByName(standardLibrary.getLibraryScope(), "Array");
        standardClasses.declareStandardTopLevelObject(nullConstructorFunction, "array");
        PropertyDescriptor sizeProperty =
                getPropertyByName(arrayClass.getDefaultType().getMemberScope(), "size");
        standardClasses.declareStandardMethodOrProperty(sizeProperty, "size");
    }


    @NotNull
    private final JsScope kotlinScope;

    @NotNull
    private final Map<String, JsName> nameMap = new HashMap<String, JsName>();

    @NotNull
    private final Map<String, JsScope> scopeMap = new HashMap<String, JsScope>();

    private StandardClasses(@NotNull JsScope kotlinScope) {
        this.kotlinScope = kotlinScope;
    }

    private void declareStandardTopLevelObject(@NotNull DeclarationDescriptor descriptor,
                                               @NotNull String kotlinLibName) {
        declareStandardTopLevelObject(getFQName(descriptor), kotlinLibName);
    }

    private void declareStandardTopLevelObject(@NotNull String fullQualifiedName, @NotNull String kotlinLibName) {
        nameMap.put(fullQualifiedName, kotlinScope.declareName(kotlinLibName));
        scopeMap.put(fullQualifiedName, new JsScope(kotlinScope, "standard object " + kotlinLibName));
    }

    private void declareStandardMethodOrProperty(@NotNull DeclarationDescriptor descriptor,
                                                 @NotNull String kotlinLibName) {
        String containingFQName = getFQName(getContainingDeclaration(descriptor));
        declareStandardInnerDeclaration(containingFQName, descriptor.getName(), kotlinLibName);
    }

    private void declareStandardInnerDeclaration(@NotNull String fullQualifiedClassName,
                                                 @NotNull String shortMethodName,
                                                 @NotNull String kotlinLibName) {
        JsScope classScope = scopeMap.get(fullQualifiedClassName);
        String fullQualifiedMethodName = fullQualifiedClassName + "." + shortMethodName;
        nameMap.put(fullQualifiedMethodName, classScope.declareName(kotlinLibName));
    }

    public boolean isStandardObject(@NotNull DeclarationDescriptor descriptor) {
        return nameMap.containsKey(getFQName(descriptor));
    }

    @NotNull
    public JsName getStandardObjectName(@NotNull DeclarationDescriptor descriptor) {
        return nameMap.get(getFQName(descriptor));
    }
}
