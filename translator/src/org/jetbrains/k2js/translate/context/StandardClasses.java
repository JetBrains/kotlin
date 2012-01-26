package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.JetStandardLibrary;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFQName;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getContainingDeclaration;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getFunctionByName;

/**
 * @author Pavel Talanov
 */
//TODO: REFACTOR FFS
public final class StandardClasses {

    private final class Builder {

        @Nullable
        private /*var*/ String currentFQName = null;
        @Nullable
        private /*var*/ String currentKotlinName = null;

        @NotNull
        public Builder forFQ(@NotNull String classFQName) {
            currentFQName = classFQName;
            return this;
        }

        @NotNull
        public Builder kotlinName(@NotNull String kotlinName) {
            assert currentFQName != null;
            currentKotlinName = kotlinName;
            declareStandardTopLevelObject(currentFQName, kotlinName);
            constructor();
            return this;
        }

        @NotNull
        private Builder constructor() {
            assert currentFQName != null;
            assert currentKotlinName != null;
            declareStandardInnerDeclaration(currentFQName, "<init>", currentKotlinName);
            return this;
        }

        @NotNull
        public Builder methods(String... methodNames) {
            assert currentFQName != null;
            declareMethods(currentFQName, methodNames);
            return this;
        }

        @NotNull
        public Builder properties(String... propertyNames) {
            assert currentFQName != null;
            declareReadonlyProperties(currentFQName, propertyNames);
            return this;
        }
    }

    //TODO: move declaration code to some kind of builder
    @NotNull
    public static StandardClasses bindImplementations(@NotNull JetStandardLibrary standardLibrary,
                                                      @NotNull JsScope kotlinObjectScope) {
        StandardClasses standardClasses = new StandardClasses(kotlinObjectScope);
        declareJetObjects(standardLibrary, standardClasses);
        declareJavaUtilObjects(standardClasses);
        declareTopLevelFunctions(standardClasses);
        declareInteger(standardClasses);
        return standardClasses;
    }

    //TODO: test all the methods
    private static void declareJavaUtilObjects(@NotNull StandardClasses standardClasses) {
        standardClasses.declare().forFQ("java.util.ArrayList").kotlinName("ArrayList")
                .methods("size", "add", "get", "isEmpty", "set", "remove", "addAll", "contains", "clear", "iterator");

        standardClasses.declare().forFQ("java.util.Collection").kotlinName("Collection")
                .methods("iterator");

        standardClasses.declare().forFQ("java.util.HashMap").kotlinName("HashMap")
                .methods("size", "put", "get", "isEmpty", "remove", "addAll", "clear", "keySet");

        standardClasses.declare().forFQ("java.util.StringBuilder").kotlinName("StringBuilder")
                .methods("append", "toString");

        standardClasses.declare().forFQ("java.util.Map").kotlinName("Map")
                .methods("size", "put", "get", "isEmpty", "remove", "addAll", "clear", "keySet");
    }

    private static void declareJetObjects(@NotNull JetStandardLibrary standardLibrary,
                                          @NotNull StandardClasses standardClasses) {
        declareArray(standardClasses, standardLibrary);

        standardClasses.declare().forFQ("jet.Iterator").kotlinName("ArrayIterator")
                .methods("next", "hasNext");

        standardClasses.declare().forFQ("jet.IntRange").kotlinName("NumberRange")
                .methods("iterator", "contains").properties("start", "size", "end", "reversed");

        standardClasses.declare().forFQ("jet.String").kotlinName("String").
                properties("length");
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

    private static void declareArray(@NotNull StandardClasses standardClasses,
                                     @NotNull JetStandardLibrary standardLibrary) {
        standardClasses.declare().forFQ("jet.Array").kotlinName("Array")
                .properties("size", "indices");
        FunctionDescriptor nullConstructorFunction = getFunctionByName(standardLibrary.getLibraryScope(), "Array");
        standardClasses.declareTopLevel(nullConstructorFunction, "array");
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

    private void declareMethods(@NotNull String classFQName,
                                @NotNull String... methodNames) {
        for (String methodName : methodNames) {
            declareStandardInnerDeclaration(classFQName, methodName, methodName);
        }
    }

    private void declareReadonlyProperties(@NotNull String classFQName,
                                           @NotNull String... propertyNames) {
        for (String propertyName : propertyNames) {
            declareStandardInnerDeclaration(classFQName, propertyName, propertyName);

        }
    }

    public boolean isStandardObject(@NotNull DeclarationDescriptor descriptor) {
        return nameMap.containsKey(getFQName(descriptor));
    }

    @NotNull
    public JsName getStandardObjectName(@NotNull DeclarationDescriptor descriptor) {
        return nameMap.get(getFQName(descriptor));
    }

    @NotNull
    private Builder declare() {
        return new Builder();
    }
}
