package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFQName;

/**
 * @author Pavel Talanov
 */
public final class StandardClasses {

    private final class Builder {

        @Nullable
        private /*var*/ String currentFQName = null;
        @Nullable
        private /*var*/ String currentObjectName = null;

        @NotNull
        public Builder forFQ(@NotNull String classFQName) {
            currentFQName = classFQName;
            return this;
        }

        @NotNull
        public Builder kotlinClass(@NotNull String kotlinName) {
            kotlinTopLevelObject(kotlinName);
            constructor();
            return this;
        }

        @NotNull
        public Builder externalClass(@NotNull String nativeName) {
            externalObject(nativeName);
            constructor();
            return this;
        }

        @NotNull
        public Builder externalFunction(@NotNull String nativeName) {
            externalObject(nativeName);
            return this;
        }

        private void externalObject(@NotNull String nativeName) {
            currentObjectName = nativeName;
            assert currentFQName != null;
            declareExternalTopLevelObject(currentFQName, nativeName);
        }

        private void kotlinTopLevelObject(@NotNull String kotlinName) {
            assert currentFQName != null;
            currentObjectName = kotlinName;
            declareKotlinObject(currentFQName, kotlinName);
        }

        @NotNull
        public Builder kotlinFunction(@NotNull String kotlinName) {
            kotlinTopLevelObject(kotlinName);
            return this;
        }

        @NotNull
        private Builder constructor() {
            assert currentFQName != null;
            assert currentObjectName != null;
            declareInner(currentFQName, "<init>", currentObjectName);
            return this;
        }

        @NotNull
        public Builder methods(@NotNull String... methodNames) {
            assert currentFQName != null;
            declareMethods(currentFQName, methodNames);
            return this;
        }

        @NotNull
        public Builder properties(@NotNull String... propertyNames) {
            assert currentFQName != null;
            declareReadonlyProperties(currentFQName, propertyNames);
            return this;
        }
    }

    @NotNull
    public static StandardClasses bindImplementations(@NotNull JsScope kotlinObjectScope,
                                                      @NotNull JsScope rootScope) {
        StandardClasses standardClasses = new StandardClasses(kotlinObjectScope, rootScope);
        declareJetObjects(standardClasses);
        declareJavaUtilObjects(standardClasses);
        declareTopLevelFunctions(standardClasses);
        declareJQuery(standardClasses);
        return standardClasses;
    }

    private static void declareJQuery(@NotNull StandardClasses standardClasses) {
        standardClasses.declare().forFQ("jquery.JQuery").externalClass("jQuery")
                .methods("addClass", "attr", "hasClass", "append",
                        "text", "ready", "click", "hover", "slideUp",
                        "removeClass");
        standardClasses.declare().forFQ("jquery.jq").externalFunction("jQuery");
        //standardClasses.declare().forFQ("jquery.get-document").externalObject("document");
    }

    //TODO: test all the methods
    private static void declareJavaUtilObjects(@NotNull StandardClasses standardClasses) {
        standardClasses.declare().forFQ("java.util.ArrayList").kotlinClass("ArrayList")
                .methods("size", "add", "get", "isEmpty", "set", "remove", "addAll", "contains", "clear", "iterator");

        standardClasses.declare().forFQ("java.util.Collection").kotlinClass("Collection")
                .methods("iterator");

        standardClasses.declare().forFQ("java.util.HashMap").kotlinClass("HashMap")
                .methods("size", "put", "get", "isEmpty", "remove", "addAll", "clear", "keySet");

        standardClasses.declare().forFQ("java.util.StringBuilder").kotlinClass("StringBuilder")
                .methods("append", "toString");

        standardClasses.declare().forFQ("java.util.Map").kotlinClass("Map")
                .methods("size", "put", "get", "isEmpty", "remove", "addAll", "clear", "keySet");

        standardClasses.declare().forFQ("Integer").kotlinClass("Integer")
                .methods("parseInt");
    }

    private static void declareJetObjects(@NotNull StandardClasses standardClasses) {
        standardClasses.declare().forFQ("jet.Array").kotlinClass("Array")
                .properties("size", "indices");

        standardClasses.declare().forFQ("jet.Array").kotlinFunction("array");

        standardClasses.declare().forFQ("jet.Iterator").kotlinClass("ArrayIterator")
                .methods("next", "hasNext");

        standardClasses.declare().forFQ("jet.IntRange").kotlinClass("NumberRange")
                .methods("iterator", "contains").properties("start", "size", "end", "reversed");

        standardClasses.declare().forFQ("jet.String").kotlinClass("String").
                properties("length");

        standardClasses.declare().forFQ("jet.Any.toString").kotlinFunction("toString");
    }

    private static void declareTopLevelFunctions(@NotNull StandardClasses standardClasses) {
        standardClasses.declare().forFQ("js.parseInt").kotlinFunction("parseInt");
        standardClasses.declare().forFQ("js.println").kotlinFunction("println");
        standardClasses.declare().forFQ("js.print").kotlinFunction("print");
    }

    @NotNull
    private final JsScope kotlinScope;

    @NotNull
    private final JsScope rootScope;

    @NotNull
    private final Map<String, JsName> standardObjects = new HashMap<String, JsName>();

    @NotNull
    private final Map<String, JsName> externalObjects = new HashMap<String, JsName>();

    @NotNull
    private final Map<String, JsScope> scopeMap = new HashMap<String, JsScope>();

    private StandardClasses(@NotNull JsScope kotlinScope, @NotNull JsScope rootScope) {
        this.rootScope = rootScope;
        this.kotlinScope = kotlinScope;
    }

    private void declareTopLevelObjectInScope(@NotNull JsScope scope, @NotNull Map<String, JsName> map,
                                              @NotNull String fullQualifiedName, @NotNull String name) {
        JsName declaredName = scope.declareName(name);
        declaredName.setObfuscatable(false);
        map.put(fullQualifiedName, declaredName);
        scopeMap.put(fullQualifiedName, new JsScope(scope, "scope for " + name));
    }

    private void declareKotlinObject(@NotNull String fullQualifiedName, @NotNull String kotlinLibName) {
        declareTopLevelObjectInScope(kotlinScope, standardObjects, fullQualifiedName, kotlinLibName);
    }

    private void declareExternalTopLevelObject(@NotNull String fullQualifiedName, @NotNull String externalObjectName) {
        declareTopLevelObjectInScope(rootScope, externalObjects, fullQualifiedName, externalObjectName);
    }

    private void declareInner(@NotNull String fullQualifiedClassName,
                              @NotNull String shortMethodName,
                              @NotNull String javascriptName) {
        JsScope classScope = scopeMap.get(fullQualifiedClassName);
        assert classScope != null;
        String fullQualifiedMethodName = fullQualifiedClassName + "." + shortMethodName;
        JsName declaredName = classScope.declareName(javascriptName);
        declaredName.setObfuscatable(false);
        standardObjects.put(fullQualifiedMethodName, declaredName);
    }

    private void declareMethods(@NotNull String classFQName,
                                @NotNull String... methodNames) {
        for (String methodName : methodNames) {
            declareInner(classFQName, methodName, methodName);
        }
    }

    private void declareReadonlyProperties(@NotNull String classFQName,
                                           @NotNull String... propertyNames) {
        for (String propertyName : propertyNames) {
            declareInner(classFQName, propertyName, propertyName);

        }
    }

    public boolean isStandardObject(@NotNull DeclarationDescriptor descriptor) {
        return standardObjects.containsKey(getFQName(descriptor));
    }

    public boolean isExternalObject(@NotNull DeclarationDescriptor descriptor) {
        return externalObjects.containsKey(getFQName(descriptor));
    }

    @NotNull
    public JsName getExternalObjectName(@NotNull DeclarationDescriptor descriptor) {
        return externalObjects.get(getFQName(descriptor));
    }

    @NotNull
    public JsName getStandardObjectName(@NotNull DeclarationDescriptor descriptor) {
        return standardObjects.get(getFQName(descriptor));
    }

    @NotNull
    private Builder declare() {
        return new Builder();
    }
}
