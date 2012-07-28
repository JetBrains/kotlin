/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.setQualifier;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Encapuslates different types of constants and naming conventions.
 */
public final class Namer {

    private static final String INITIALIZE_METHOD_NAME = "initialize";
    private static final String CLASS_OBJECT_NAME = "createClass";
    private static final String TRAIT_OBJECT_NAME = "createTrait";
    private static final String OBJECT_OBJECT_NAME = "createObject";
    private static final String SETTER_PREFIX = "set_";
    private static final String GETTER_PREFIX = "get_";
    private static final String BACKING_FIELD_PREFIX = "$";
    private static final String SUPER_METHOD_NAME = "super_init";
    private static final String KOTLIN_OBJECT_NAME = "Kotlin";
    private static final String ROOT_NAMESPACE = "_";
    private static final String RECEIVER_PARAMETER_NAME = "receiver";
    private static final String CLASSES_OBJECT_NAME = "classes";
    private static final String THROW_NPE_FUN_NAME = "throwNPE";

    @NotNull
    public static String getReceiverParameterName() {
        return RECEIVER_PARAMETER_NAME;
    }

    @NotNull
    public static String getRootNamespaceName() {
        return ROOT_NAMESPACE;
    }

    @NotNull
    public static JsNameRef initializeMethodReference() {
        return AstUtil.newQualifiedNameRef(INITIALIZE_METHOD_NAME);
    }

    @NotNull
    public static String superMethodName() {
        return SUPER_METHOD_NAME;
    }

    @NotNull
    public static String nameForClassesVariable() {
        return CLASSES_OBJECT_NAME;
    }

    @NotNull
    public static String getNameForAccessor(@NotNull String propertyName, boolean isGetter, boolean useNativeAccessor) {
        if (useNativeAccessor) {
            return propertyName;
        }

        if (isGetter) {
            return getNameForGetter(propertyName);
        }
        else {
            return getNameForSetter(propertyName);
        }
    }

    @NotNull
    public static String getKotlinBackingFieldName(@NotNull String propertyName) {
        return getNameWithPrefix(propertyName, BACKING_FIELD_PREFIX);
    }

    @NotNull
    private static String getNameForGetter(@NotNull String propertyName) {
        return getNameWithPrefix(propertyName, GETTER_PREFIX);
    }

    @NotNull
    private static String getNameForSetter(@NotNull String propertyName) {
        return getNameWithPrefix(propertyName, SETTER_PREFIX);
    }

    @NotNull
    private static String getNameWithPrefix(@NotNull String name, @NotNull String prefix) {
        return prefix + name;
    }

    public static Namer newInstance(@NotNull JsScope rootScope) {
        return new Namer(rootScope);
    }

    @NotNull
    private final JsName kotlinName;
    @NotNull
    private final JsScope kotlinScope;
    @NotNull
    private final JsName className;
    @NotNull
    private final JsName traitName;
    @NotNull
    private final JsExpression definePackage;
    @NotNull
    private final JsName objectName;

    @NotNull
    private final JsName isTypeName;

    @NotNull
    private final JsPropertyInitializer writablePropertyDescriptorField;

    @NotNull
    private final JsPropertyInitializer enumerablePropertyDescriptorField;

    private Namer(@NotNull JsScope rootScope) {
        kotlinName = rootScope.declareName(KOTLIN_OBJECT_NAME);
        kotlinScope = new JsScope(rootScope, "Kotlin standard object");
        traitName = kotlinScope.declareName(TRAIT_OBJECT_NAME);

        definePackage = kotlin("definePackage");

        className = kotlinScope.declareName(CLASS_OBJECT_NAME);
        objectName = kotlinScope.declareName(OBJECT_OBJECT_NAME);

        isTypeName = kotlinScope.declareName("isType");

        JsProgram program = rootScope.getProgram();
        writablePropertyDescriptorField = new JsPropertyInitializer(program.getStringLiteral("writable"), program.getTrueLiteral());
        enumerablePropertyDescriptorField = new JsPropertyInitializer(program.getStringLiteral("enumerable"), program.getTrueLiteral());
    }

    @NotNull
    public JsExpression classCreationMethodReference() {
        return kotlin(className);
    }

    @NotNull
    public JsExpression traitCreationMethodReference() {
        return kotlin(traitName);
    }

    @NotNull
    public JsExpression packageDefinitionMethodReference() {
        return definePackage;
    }

    @NotNull
    public JsExpression objectCreationMethodReference() {
        return kotlin(objectName);
    }

    @NotNull
    public JsExpression throwNPEFunctionCall() {
        JsNameRef reference = AstUtil.newQualifiedNameRef(THROW_NPE_FUN_NAME);
        JsInvocation invocation = AstUtil.newInvocation(reference);
        return kotlin(invocation);
    }

    @NotNull
    private JsExpression kotlin(@NotNull JsName name) {
        return kotlin(name.makeRef());
    }

    @NotNull
    public JsExpression kotlin(@NotNull String name) {
        return kotlin(kotlinScope.declareName(name));
    }

    @NotNull
    private JsExpression kotlin(@NotNull JsExpression reference) {
        setQualifier(reference, kotlinObject());
        return reference;
    }

    @NotNull
    public JsNameRef kotlinObject() {
        return kotlinName.makeRef();
    }

    @NotNull
    public JsExpression isOperationReference() {
        return kotlin(isTypeName);
    }

    @NotNull
    public JsPropertyInitializer writablePropertyDescriptorField() {
        return writablePropertyDescriptorField;
    }

    @NotNull
    public JsPropertyInitializer enumerablePropertyDescriptorField() {
        return enumerablePropertyDescriptorField;
    }

    @NotNull
        /*package*/ JsScope getKotlinScope() {
        return kotlinScope;
    }

    @NotNull
    static String generateNamespaceName(DeclarationDescriptor descriptor) {
        if (DescriptorUtils.isRootNamespace((NamespaceDescriptor) descriptor)) {
            return getRootNamespaceName();
        }
        else {
            return descriptor.getName().getName();
        }
    }
}
