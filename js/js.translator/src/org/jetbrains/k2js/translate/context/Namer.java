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

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.Named;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.lang.resolve.name.Name;

/**
 * Encapuslates different types of constants and naming conventions.
 */
public final class Namer {
    public static final String KOTLIN_NAME = JetLanguage.NAME;
    public static final String KOTLIN_LOWER_NAME = KOTLIN_NAME.toLowerCase();

    public static final String CALLEE_NAME = "$fun";
    public static final String OUTER_CLASS_NAME = "$outer";

    private static final String CALL_FUNCTION = "call";
    private static final String INITIALIZE_METHOD_NAME = "initialize";
    private static final String CLASS_OBJECT_NAME = "createClass";
    private static final String TRAIT_OBJECT_NAME = "createTrait";
    private static final String OBJECT_OBJECT_NAME = "createObject";
    private static final String ENUM_ENTRIES_NAME = "createEnumEntries";
    private static final String SETTER_PREFIX = "set_";
    private static final String GETTER_PREFIX = "get_";
    private static final String BACKING_FIELD_PREFIX = "$";
    private static final String SUPER_METHOD_NAME = "baseInitializer";
    private static final String ROOT_NAMESPACE = "_";
    private static final String RECEIVER_PARAMETER_NAME = "$receiver";
    private static final String CLASSES_OBJECT_NAME = "_c";
    private static final String THROW_NPE_FUN_NAME = "throwNPE";
    private static final String CLASS_OBJECT_GETTER = "object$";
    private static final String CLASS_OBJECT_INITIALIZER = "object_initializer$";


    private static final String DELEGATE_POSTFIX = "$delegate";
    private static final String PROPERTY_METADATA = "PropertyMetadata";

    private static final Named CLASS_OBJECT_INITIALIZER_NAMED = new Named() {
        @NotNull
        @Override
        public Name getName() {
            return Name.identifier(CLASS_OBJECT_INITIALIZER);
        }
    };

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
        return new JsNameRef(INITIALIZE_METHOD_NAME);
    }

    @NotNull
    public static JsNameRef superMethodNameRef(@NotNull JsName superClassJsName) {
        return new JsNameRef(SUPER_METHOD_NAME, superClassJsName.makeRef());
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
    public static JsExpression getClassObjectAccessor(@NotNull JsExpression referenceToClass) {
        return new JsInvocation(new JsNameRef(CLASS_OBJECT_GETTER, referenceToClass));
    }

    @NotNull
    public static Named getNamedForClassObjectInitializer() {
        return CLASS_OBJECT_INITIALIZER_NAMED;
    }

    @NotNull
    public static String getDelegateName(@NotNull String propertyName) {
        return propertyName + DELEGATE_POSTFIX;
    }

    @NotNull
    public static JsNameRef getDelegateNameRef(String propertyName) {
        return new JsNameRef(getDelegateName(propertyName), JsLiteral.THIS);
    }

    @NotNull
    private static String getNameWithPrefix(@NotNull String name, @NotNull String prefix) {
        return prefix + name;
    }

    @NotNull
    public static JsNameRef getFunctionCallRef(@NotNull JsExpression functionExpression) {
        return new JsNameRef(CALL_FUNCTION, functionExpression);
    }

    @NotNull
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
    private final JsExpression defineRootPackage;
    @NotNull
    private final JsName objectName;
    @NotNull
    private final JsName enumEntriesName;

    @NotNull
    private final JsName isTypeName;

    private Namer(@NotNull JsScope rootScope) {
        kotlinName = rootScope.declareName(KOTLIN_NAME);
        kotlinScope = new JsScope(rootScope, "Kotlin standard object");
        traitName = kotlinScope.declareName(TRAIT_OBJECT_NAME);

        definePackage = kotlin("definePackage");
        defineRootPackage = kotlin("defineRootPackage");

        className = kotlinScope.declareName(CLASS_OBJECT_NAME);
        enumEntriesName = kotlinScope.declareName(ENUM_ENTRIES_NAME);
        objectName = kotlinScope.declareName(OBJECT_OBJECT_NAME);

        isTypeName = kotlinScope.declareName("isType");
    }

    @NotNull
    public JsExpression classCreationMethodReference() {
        return kotlin(className);
    }

    @NotNull
    public JsExpression enumEntriesCreationMethodReference() {
        return kotlin(enumEntriesName);
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
    public JsExpression rootPackageDefinitionMethodReference() {
        return defineRootPackage;
    }

    @NotNull
    public JsExpression objectCreationMethodReference() {
        return kotlin(objectName);
    }

    @NotNull
    public JsExpression throwNPEFunctionRef() {
        return new JsNameRef(THROW_NPE_FUN_NAME, kotlinObject());
    }

    @NotNull
    public JsNameRef propertyMetadataRef() {
        return new JsNameRef(PROPERTY_METADATA, kotlinObject());
    }

    @NotNull
    private JsNameRef kotlin(@NotNull JsName name) {
        return new JsNameRef(name, kotlinObject());
    }

    @NotNull
    public JsExpression kotlin(@NotNull String name) {
        return kotlin(kotlinScope.declareName(name));
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
        /*package*/ JsScope getKotlinScope() {
        return kotlinScope;
    }

    @NotNull
    static String generateNamespaceName(DeclarationDescriptor descriptor) {
        if (DescriptorUtils.isRootNamespace((NamespaceDescriptor) descriptor)) {
            return getRootNamespaceName();
        }
        else {
            return descriptor.getName().asString();
        }
    }

    @NotNull
    public JsInvocation classCreateInvocation(@NotNull ClassDescriptor descriptor) {
        switch (descriptor.getKind()) {
            case TRAIT:
                return new JsInvocation(traitCreationMethodReference());

            case OBJECT:
            case CLASS_OBJECT:
            case ENUM_ENTRY:
                return new JsInvocation(objectCreationMethodReference());

            default:
                return new JsInvocation(classCreationMethodReference());
        }
    }

    @NotNull
    public JsInvocation enumEntriesObjectCreateInvocation() {
        return new JsInvocation(enumEntriesCreationMethodReference());
    }
}
