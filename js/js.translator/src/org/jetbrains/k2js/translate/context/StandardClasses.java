/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.context;

import com.google.common.collect.Maps;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Map;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFqName;

/**
 * Provides a mechanism to bind some of the kotlin/java declations with library implementations.
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
    public static StandardClasses bindImplementations(@NotNull JsScope kotlinObjectScope) {
        StandardClasses standardClasses = new StandardClasses(kotlinObjectScope);
        declareJetObjects(standardClasses);
        return standardClasses;
    }

    private static void declareJetObjects(@NotNull StandardClasses standardClasses) {
        standardClasses.declare().forFQ("jet.Iterator").kotlinClass("Iterator").methods("next").properties("hasNext");

        standardClasses.declare().forFQ("jet.IntRange").kotlinClass("NumberRange")
                .methods("iterator", "contains").properties("start", "end", "increment");

        standardClasses.declare().forFQ("jet.IntProgression").kotlinClass("NumberProgression")
                .methods("iterator", "contains").properties("start", "end", "increment");

        standardClasses.declare().forFQ("jet.Enum").kotlinClass("Enum");
    }


    @NotNull
    private final JsScope kotlinScope;


    @NotNull
    private final Map<FqNameUnsafe, JsName> standardObjects = Maps.newHashMap();

    @NotNull
    private final Map<FqNameUnsafe, JsScope> scopeMap = Maps.newHashMap();

    private StandardClasses(@NotNull JsScope kotlinScope) {
        this.kotlinScope = kotlinScope;
    }

    private void declareTopLevelObjectInScope(@NotNull JsScope scope, @NotNull Map<FqNameUnsafe, JsName> map,
                                              @NotNull FqNameUnsafe fullQualifiedName, @NotNull String name) {
        JsName declaredName = scope.declareName(name);
        map.put(fullQualifiedName, declaredName);
        scopeMap.put(fullQualifiedName, new JsScope(scope, "scope for " + name));
    }

    private void declareKotlinObject(@NotNull FqNameUnsafe fullQualifiedName, @NotNull String kotlinLibName) {
        declareTopLevelObjectInScope(kotlinScope, standardObjects, fullQualifiedName, kotlinLibName);
    }

    private void declareInner(@NotNull FqNameUnsafe fullQualifiedClassName,
                              @NotNull String shortMethodName,
                              @NotNull String javascriptName) {
        JsScope classScope = scopeMap.get(fullQualifiedClassName);
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
