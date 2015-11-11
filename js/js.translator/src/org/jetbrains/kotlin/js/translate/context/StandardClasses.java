/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.translate.context;

import com.google.common.collect.Maps;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsObjectScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;

import java.util.Map;

import static com.google.dart.compiler.backend.js.ast.JsScopesKt.JsObjectScope;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName;

/**
 * Provides a mechanism to bind some of the Kotlin/Java declarations with library implementations.
 * Makes sense only for those declaration that cannot be annotated. (Use library annotation in this case)
 */
public final class StandardClasses {

    private final class Builder {

        @Nullable
        private /*var*/ FqNameUnsafe currentFQName = null;
        @Nullable
        private /*var*/ String currentObjectName = null;

        @NotNull
        public Builder forFQ(@NotNull String classFQName) {
            currentFQName = new FqNameUnsafe(classFQName);
            return this;
        }

        @NotNull
        public Builder kotlinClass(@NotNull String kotlinName) {
            kotlinTopLevelObject(kotlinName);
            constructor();
            return this;
        }

        private void kotlinTopLevelObject(@NotNull String kotlinName) {
            assert currentFQName != null;
            currentObjectName = kotlinName;
            declareKotlinObject(currentFQName, kotlinName);
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
    public static StandardClasses bindImplementations(@NotNull JsObjectScope kotlinObjectScope) {
        StandardClasses standardClasses = new StandardClasses(kotlinObjectScope);
        declareKotlinStandardClasses(standardClasses);
        return standardClasses;
    }

    private static void declareKotlinStandardClasses(@NotNull StandardClasses standardClasses) {
        for (PrimitiveType type : PrimitiveType.NUMBER_TYPES) {
            if (type == PrimitiveType.CHAR || type == PrimitiveType.LONG) continue;

            String typeName = type.getTypeName().asString();
            standardClasses.declare().forFQ("kotlin." + typeName + "Range").kotlinClass("NumberRange");
            standardClasses.declare().forFQ("kotlin." + typeName + "Progression").kotlinClass("NumberProgression");
        }

        standardClasses.declare().forFQ("kotlin.LongRange").kotlinClass("LongRange");
        standardClasses.declare().forFQ("kotlin.CharRange").kotlinClass("CharRange");

        standardClasses.declare().forFQ("kotlin.LongProgression").kotlinClass("LongProgression");
        standardClasses.declare().forFQ("kotlin.CharProgression").kotlinClass("CharProgression");

        standardClasses.declare().forFQ("kotlin.Enum").kotlinClass("Enum");

        standardClasses.declare().forFQ("kotlin.Comparable").kotlinClass("Comparable");

        standardClasses.declare().forFQ("koltin.Throwable").kotlinClass("Throwable");
    }


    @NotNull
    private final JsObjectScope kotlinScope;


    @NotNull
    private final Map<FqNameUnsafe, JsName> standardObjects = Maps.newHashMap();

    @NotNull
    private final Map<FqNameUnsafe, JsObjectScope> scopeMap = Maps.newHashMap();

    private StandardClasses(@NotNull JsObjectScope kotlinScope) {
        this.kotlinScope = kotlinScope;
    }

    private void declareTopLevelObjectInScope(@NotNull JsObjectScope scope, @NotNull Map<FqNameUnsafe, JsName> map,
                                              @NotNull FqNameUnsafe fullQualifiedName, @NotNull String name) {
        JsName declaredName = scope.declareName(name);
        map.put(fullQualifiedName, declaredName);
        scopeMap.put(fullQualifiedName, JsObjectScope(scope, "scope for " + name));
    }

    private void declareKotlinObject(@NotNull FqNameUnsafe fullQualifiedName, @NotNull String kotlinLibName) {
        declareTopLevelObjectInScope(kotlinScope, standardObjects, fullQualifiedName, kotlinLibName);
    }

    private void declareInner(@NotNull FqNameUnsafe fullQualifiedClassName,
                              @NotNull String shortMethodName,
                              @NotNull String javascriptName) {
        JsObjectScope classScope = scopeMap.get(fullQualifiedClassName);
        assert classScope != null;
        FqNameUnsafe fullQualifiedMethodName = fullQualifiedClassName.child(Name.guess(shortMethodName));
        standardObjects.put(fullQualifiedMethodName, classScope.declareName(javascriptName));
    }

    private void declareMethods(@NotNull FqNameUnsafe classFQName,
                                @NotNull String... methodNames) {
        for (String methodName : methodNames) {
            declareInner(classFQName, methodName, methodName);
        }
    }

    private void declareReadonlyProperties(@NotNull FqNameUnsafe classFQName,
                                           @NotNull String... propertyNames) {
        for (String propertyName : propertyNames) {
            declareInner(classFQName, propertyName, propertyName);
        }
    }

    public boolean isStandardObject(@NotNull DeclarationDescriptor descriptor) {
        return standardObjects.containsKey(getFqName(descriptor));
    }

    @NotNull
    public JsName getStandardObjectName(@NotNull DeclarationDescriptor descriptor) {
        return standardObjects.get(getFqName(descriptor));
    }

    @NotNull
    private Builder declare() {
        return new Builder();
    }
}
