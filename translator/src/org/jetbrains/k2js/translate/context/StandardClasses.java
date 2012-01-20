package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.JetStandardLibrary;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFQName;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.*;

/**
 * @author Pavel Talanov
 */
//TODO: REFACTOR FFS
public final class StandardClasses {

    //TODO: move declaration code to some kind of builder
    @NotNull
    public static StandardClasses bindImplementations(@NotNull JetStandardLibrary standardLibrary,
                                                      @NotNull JsScope kotlinObjectScope) {
        StandardClasses standardClasses = new StandardClasses(kotlinObjectScope);
        declareArray(standardClasses, standardLibrary);
        declareIterator(standardClasses, standardLibrary);
        declareRange(standardClasses);
        declareString(standardClasses);
        declareJavaArrayList(standardClasses);
        declareJavaHasMap(standardClasses);
        declareJavaStringBuilder(standardClasses);
        declareTopLevelFunctions(standardClasses);
        declareJavaCollection(standardClasses);
        declareMap(standardClasses);
        declareInteger(standardClasses);
        return standardClasses;
    }

    private static void declareMap(@NotNull StandardClasses standardClasses) {
        String hashMapFQName = "java.util.Map";
        standardClasses.declareStandardTopLevelObject(hashMapFQName, "Map");
        standardClasses.declareStandardInnerDeclaration(hashMapFQName, "<init>", "HashMap");
        declareMethods(standardClasses, hashMapFQName, "size", "put", "get",
                "isEmpty", "remove", "addAll", "clear", "keySet");
    }

    private static void declareJavaCollection(@NotNull StandardClasses standardClasses) {
        String collection = "java.util.Collection";
        standardClasses.declareStandardTopLevelObject(collection, "Collection");
        declareMethods(standardClasses, collection, "iterator");
    }

    private static void declareJavaStringBuilder(@NotNull StandardClasses standardClasses) {
        String stringBuilderFQName = "java.util.StringBuilder";
        standardClasses.declareStandardTopLevelObject(stringBuilderFQName, "StringBuilder");
        standardClasses.declareStandardInnerDeclaration(stringBuilderFQName, "<init>", "StringBuilder");
        declareMethods(standardClasses, stringBuilderFQName, "append", "toString");
    }

    private static void declareJavaHasMap(@NotNull StandardClasses standardClasses) {
        String hashMapFQName = "java.util.HashMap";
        standardClasses.declareStandardTopLevelObject(hashMapFQName, "HashMap");
        standardClasses.declareStandardInnerDeclaration(hashMapFQName, "<init>", "HashMap");
        declareMethods(standardClasses, hashMapFQName, "size", "put", "get",
                "isEmpty", "remove", "addAll", "clear", "keySet");
    }

    //TODO: refactor
    private static void declareTopLevelFunctions(@NotNull StandardClasses standardClasses) {
        String parseIntFQName = "js.parseInt";
        standardClasses.declareStandardTopLevelObject(parseIntFQName, "parseInt");
        String printlnFQName = "js.println";
        standardClasses.declareStandardTopLevelObject(printlnFQName, "println");
        String printFQName = "js.print";
        standardClasses.declareStandardTopLevelObject(printFQName, "print");
    }

    private static void declareInteger(@NotNull StandardClasses standardClasses) {
        String integerFQName = "Integer";
        standardClasses.declareStandardTopLevelObject(integerFQName, "Integer");
        standardClasses.declareStandardInnerDeclaration(integerFQName, "parseInt", "parseInt");
    }


    private static void declareString(@NotNull StandardClasses standardClasses) {
        String stringFQName = "jet.String";
        standardClasses.declareStandardTopLevelObject(stringFQName, "String");
        declareReadonlyProperties(standardClasses, stringFQName, "length");
    }

    //TODO: duplication
    private static void declareRange(@NotNull StandardClasses standardClasses) {
        String intRangeFQName = "jet.IntRange";
        standardClasses.declareStandardTopLevelObject(intRangeFQName, "NumberRange");
        standardClasses.declareStandardInnerDeclaration(intRangeFQName, "<init>", "NumberRange");
        declareMethods(standardClasses, intRangeFQName, "iterator", "contains");
        declareReadonlyProperties(standardClasses, intRangeFQName, "start", "size", "end", "reversed");
    }


    private static void declareJavaArrayList(@NotNull StandardClasses standardClasses) {
        String arrayListFQName = "java.util.ArrayList";
        standardClasses.declareStandardTopLevelObject(arrayListFQName, "ArrayList");
        standardClasses.declareStandardInnerDeclaration(arrayListFQName, "<init>", "ArrayList");
        declareMethods(standardClasses, arrayListFQName, "size", "add", "get",
                "isEmpty", "set", "remove", "addAll", "contains", "clear", "iterator");
    }

    private static void declareIterator(@NotNull StandardClasses standardClasses,
                                        @NotNull JetStandardLibrary standardLibrary) {
        ClassDescriptor iteratorClass = (ClassDescriptor)
                standardLibrary.getLibraryScope().getClassifier("Iterator");
        assert iteratorClass != null;
        standardClasses.declareTopLevel(iteratorClass, "ArrayIterator");
        declareMethods(standardClasses, getFQName(iteratorClass), "next", "hasNext");
    }

    private static void declareArray(@NotNull StandardClasses standardClasses,
                                     @NotNull JetStandardLibrary standardLibrary) {
        ClassDescriptor arrayClass = standardLibrary.getArray();
        standardClasses.declareTopLevel(arrayClass, "Array");
        FunctionDescriptor nullConstructorFunction = getFunctionByName(standardLibrary.getLibraryScope(), "Array");
        standardClasses.declareTopLevel(nullConstructorFunction, "array");
        PropertyDescriptor sizeProperty =
                getPropertyByName(arrayClass.getDefaultType().getMemberScope(), "size");
        standardClasses.declareInner(sizeProperty, "size");
        PropertyDescriptor indices = getPropertyByName(arrayClass.getDefaultType().getMemberScope(), "indices");
        standardClasses.declareInner(indices, "indices");
    }


    private static void declareMethods(@NotNull StandardClasses standardClasses,
                                       @NotNull String classFQName,
                                       @NotNull String... methodNames) {
        for (String methodName : methodNames) {
            standardClasses.declareStandardInnerDeclaration(classFQName, methodName, methodName);
        }
    }

    private static void declareReadonlyProperties(@NotNull StandardClasses standardClasses,
                                                  @NotNull String classFQName,
                                                  @NotNull String... propertyNames) {
        for (String propertyName : propertyNames) {
            standardClasses.declareStandardInnerDeclaration(classFQName,
                    propertyName, propertyName);
            //TODO: provide general and concise way to declare
//            standardClasses.declareStandardInnerDeclaration(classFQName,
//                    "get-" + propertyName, propertyName);

        }
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

    private void declareTopLevel(@NotNull DeclarationDescriptor descriptor,
                                 @NotNull String kotlinLibName) {
        declareStandardTopLevelObject(DescriptorUtils.getFQName(descriptor), kotlinLibName);
    }

    private void declareStandardTopLevelObject(@NotNull String fullQualifiedName, @NotNull String kotlinLibName) {
        JsName declaredName = kotlinScope.declareName(kotlinLibName);
        declaredName.setObfuscatable(false);
        nameMap.put(fullQualifiedName, declaredName);
        scopeMap.put(fullQualifiedName, new JsScope(kotlinScope, "standard object " + kotlinLibName));
    }

    private void declareInner(@NotNull DeclarationDescriptor descriptor,
                              @NotNull String kotlinLibName) {
        String containingFQName = DescriptorUtils.getFQName(getContainingDeclaration(descriptor));
        declareStandardInnerDeclaration(containingFQName, descriptor.getName(), kotlinLibName);
    }

    private void declareStandardInnerDeclaration(@NotNull String fullQualifiedClassName,
                                                 @NotNull String shortMethodName,
                                                 @NotNull String kotlinLibName) {
        JsScope classScope = scopeMap.get(fullQualifiedClassName);
        String fullQualifiedMethodName = fullQualifiedClassName + "." + shortMethodName;
        JsName declaredName = classScope.declareName(kotlinLibName);
        declaredName.setObfuscatable(false);
        nameMap.put(fullQualifiedMethodName, declaredName);
    }

    public boolean isStandardObject(@NotNull DeclarationDescriptor descriptor) {
        return nameMap.containsKey(getFQName(descriptor));
    }

    @NotNull
    public JsName getStandardObjectName(@NotNull DeclarationDescriptor descriptor) {
        return nameMap.get(getFQName(descriptor));
    }
}
