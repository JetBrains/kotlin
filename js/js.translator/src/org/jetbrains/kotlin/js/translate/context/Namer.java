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

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.metadata.MetadataProperties;
import com.google.dart.compiler.backend.js.ast.metadata.TypeCheck;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.js.resolve.JsPlatform;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import java.util.Arrays;

import static com.google.dart.compiler.backend.js.ast.JsScopesKt.JsObjectScope;
import static org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getModuleName;
import static org.jetbrains.kotlin.js.translate.utils.ManglingUtils.getStableMangledNameForDescriptor;
import static org.jetbrains.kotlin.js.translate.utils.ManglingUtils.getSuggestedName;

/**
 * Encapsulates different types of constants and naming conventions.
 */
public final class Namer {
    public static final String KOTLIN_NAME = KotlinLanguage.NAME;
    public static final String KOTLIN_LOWER_NAME = KOTLIN_NAME.toLowerCase();
    public static final JsNameRef KOTLIN_OBJECT_REF = new JsNameRef(KOTLIN_NAME);
    public static final JsNameRef KOTLIN_LONG_NAME_REF = new JsNameRef("Long", KOTLIN_OBJECT_REF);

    public static final String EQUALS_METHOD_NAME = getStableMangledNameForDescriptor(JsPlatform.INSTANCE.getBuiltIns().getAny(), "equals");
    public static final String COMPARE_TO_METHOD_NAME = getStableMangledNameForDescriptor(JsPlatform.INSTANCE.getBuiltIns().getComparable(), "compareTo");
    public static final String NUMBER_RANGE = "NumberRange";
    public static final String CHAR_RANGE = "CharRange";
    public static final String LONG_FROM_NUMBER = "fromNumber";
    public static final String LONG_TO_NUMBER = "toNumber";
    public static final String LONG_FROM_INT = "fromInt";
    public static final String LONG_ZERO = "ZERO";
    public static final String LONG_ONE = "ONE";
    public static final String LONG_NEG_ONE = "NEG_ONE";
    public static final String PRIMITIVE_COMPARE_TO = "primitiveCompareTo";
    public static final String IS_CHAR = "isChar";
    public static final String IS_NUMBER = "isNumber";

    public static final String CALLEE_NAME = "$fun";

    public static final String CALL_FUNCTION = "call";
    private static final String APPLY_FUNCTION = "apply";

    public static final String OUTER_FIELD_NAME = "$outer";

    private static final String CLASS_OBJECT_NAME = "createClass";
    private static final String ENUM_CLASS_OBJECT_NAME = "createEnumClass";
    private static final String TRAIT_OBJECT_NAME = "createTrait";
    private static final String OBJECT_OBJECT_NAME = "createObject";
    private static final String CALLABLE_REF_FOR_MEMBER_FUNCTION_NAME = "getCallableRefForMemberFunction";
    private static final String CALLABLE_REF_FOR_EXTENSION_FUNCTION_NAME = "getCallableRefForExtensionFunction";
    private static final String CALLABLE_REF_FOR_LOCAL_EXTENSION_FUNCTION_NAME = "getCallableRefForLocalExtensionFunction";
    private static final String CALLABLE_REF_FOR_CONSTRUCTOR_NAME = "getCallableRefForConstructor";
    private static final String CALLABLE_REF_FOR_TOP_LEVEL_PROPERTY = "getCallableRefForTopLevelProperty";
    private static final String CALLABLE_REF_FOR_MEMBER_PROPERTY = "getCallableRefForMemberProperty";
    private static final String CALLABLE_REF_FOR_EXTENSION_PROPERTY = "getCallableRefForExtensionProperty";

    private static final String SETTER_PREFIX = "set_";
    private static final String GETTER_PREFIX = "get_";
    private static final String BACKING_FIELD_PREFIX = "$";
    private static final String DELEGATE = "$delegate";

    private static final String SUPER_METHOD_NAME = "baseInitializer";

    private static final String ROOT_PACKAGE = "_";

    private static final String RECEIVER_PARAMETER_NAME = "$receiver";
    public static final String ANOTHER_THIS_PARAMETER_NAME = "$this";

    private static final String THROW_NPE_FUN_NAME = "throwNPE";
    private static final String COMPANION_OBJECT_GETTER = "object";
    private static final String COMPANION_OBJECT_INITIALIZER = "object_initializer$";
    private static final String PROTOTYPE_NAME = "prototype";
    public static final String CAPTURED_VAR_FIELD = "v";

    public static final JsNameRef CREATE_INLINE_FUNCTION = new JsNameRef("defineInlineFunction", KOTLIN_OBJECT_REF);

    @NotNull
    public static final JsExpression UNDEFINED_EXPRESSION = new JsPrefixOperation(JsUnaryOperator.VOID, JsNumberLiteral.ZERO);

    private static final JsNameRef JS_OBJECT = new JsNameRef("Object");
    private static final JsNameRef JS_OBJECT_CREATE_FUNCTION = new JsNameRef("create", JS_OBJECT);

    public static boolean isUndefined(@NotNull JsExpression expr) {
        if (expr instanceof JsPrefixOperation) {
            JsUnaryOperator op = ((JsPrefixOperation) expr).getOperator();

            return op == JsUnaryOperator.VOID;
        }

        return false;
    }

    @NotNull
    public static String getFunctionTag(@NotNull CallableDescriptor functionDescriptor) {
        String moduleName = getModuleName(functionDescriptor);
        FqNameUnsafe fqNameParent = DescriptorUtils.getFqName(functionDescriptor).parent();
        String qualifier = null;

        if (!fqNameParent.isRoot()) {
            qualifier = fqNameParent.asString();
        }

        String mangledName = getSuggestedName(functionDescriptor);
        return StringUtil.join(Arrays.asList(moduleName, qualifier, mangledName), ".");
    }

    @NotNull
    public static String getReceiverParameterName() {
        return RECEIVER_PARAMETER_NAME;
    }

    @NotNull
    public static String getRootPackageName() {
        return ROOT_PACKAGE;
    }

    @NotNull
    public static JsNameRef superMethodNameRef(@NotNull JsName superClassJsName) {
        return new JsNameRef(SUPER_METHOD_NAME, superClassJsName.makeRef());
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
    public static JsExpression getCompanionObjectAccessor(@NotNull JsExpression referenceToClass) {
        return new JsNameRef(COMPANION_OBJECT_GETTER, referenceToClass);
    }

    @NotNull
    public static String getNameForCompanionObjectInitializer() {
        return COMPANION_OBJECT_INITIALIZER;
    }

    @NotNull
    public static String getPrototypeName() {
        return PROTOTYPE_NAME;
    }

    @NotNull
    public static JsNameRef getRefToPrototype(@NotNull JsExpression classOrTraitExpression) {
        return new JsNameRef(getPrototypeName(), classOrTraitExpression);
    }

    @NotNull
    public static String getDelegatePrefix() {
        return DELEGATE;
    }

    @NotNull
    public static String getDelegateName(@NotNull String propertyName) {
        return propertyName + DELEGATE;
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
    public static JsNameRef getFunctionApplyRef(@NotNull JsExpression functionExpression) {
        return new JsNameRef(APPLY_FUNCTION, functionExpression);
    }

    @NotNull
    public static JsInvocation createObjectWithPrototypeFrom(JsNameRef referenceToClass) {
        return new JsInvocation(JS_OBJECT_CREATE_FUNCTION, getRefToPrototype(referenceToClass));
    }

    @NotNull
    public static JsNameRef getCapturedVarAccessor(@NotNull JsExpression ref) {
        return new JsNameRef(CAPTURED_VAR_FIELD, ref);
    }

    @NotNull
    public static String isInstanceSuggestedName(@NotNull TypeParameterDescriptor descriptor) {
        return "is" + descriptor.getName().getIdentifier();
    }

    @NotNull
    public static Namer newInstance(@NotNull JsScope rootScope) {
        return new Namer(rootScope);
    }

    @NotNull
    private final JsName kotlinName;
    @NotNull
    private final JsObjectScope kotlinScope;
    @NotNull
    private final JsName className;
    @NotNull
    private final JsName enumClassName;
    @NotNull
    private final JsName traitName;
    @NotNull
    private final JsExpression definePackage;
    @NotNull
    private final JsExpression defineRootPackage;
    @NotNull
    private final JsName objectName;
    @NotNull
    private final JsName callableRefForMemberFunctionName;
    @NotNull
    private final JsName callableRefForExtensionFunctionName;
    @NotNull
    private final JsName callableRefForLocalExtensionFunctionName;
    @NotNull
    private final JsName callableRefForConstructorName;
    @NotNull
    private final JsName callableRefForTopLevelProperty;
    @NotNull
    private final JsName callableRefForMemberProperty;
    @NotNull
    private final JsName callableRefForExtensionProperty;
    @NotNull
    private final JsExpression callGetProperty;
    @NotNull
    private final JsExpression callSetProperty;

    @NotNull
    private final JsName isTypeName;

    @NotNull
    private final JsExpression modulesMap;

    private Namer(@NotNull JsScope rootScope) {
        kotlinName = rootScope.declareName(KOTLIN_NAME);
        kotlinScope = JsObjectScope(rootScope, "Kotlin standard object");
        traitName = kotlinScope.declareName(TRAIT_OBJECT_NAME);

        definePackage = kotlin("definePackage");
        defineRootPackage = kotlin("defineRootPackage");

        callGetProperty = kotlin("callGetter");
        callSetProperty = kotlin("callSetter");

        className = kotlinScope.declareName(CLASS_OBJECT_NAME);
        enumClassName = kotlinScope.declareName(ENUM_CLASS_OBJECT_NAME);
        objectName = kotlinScope.declareName(OBJECT_OBJECT_NAME);
        callableRefForMemberFunctionName = kotlinScope.declareName(CALLABLE_REF_FOR_MEMBER_FUNCTION_NAME);
        callableRefForExtensionFunctionName = kotlinScope.declareName(CALLABLE_REF_FOR_EXTENSION_FUNCTION_NAME);
        callableRefForLocalExtensionFunctionName = kotlinScope.declareName(CALLABLE_REF_FOR_LOCAL_EXTENSION_FUNCTION_NAME);
        callableRefForConstructorName = kotlinScope.declareName(CALLABLE_REF_FOR_CONSTRUCTOR_NAME);
        callableRefForTopLevelProperty = kotlinScope.declareName(CALLABLE_REF_FOR_TOP_LEVEL_PROPERTY);
        callableRefForMemberProperty = kotlinScope.declareName(CALLABLE_REF_FOR_MEMBER_PROPERTY);
        callableRefForExtensionProperty = kotlinScope.declareName(CALLABLE_REF_FOR_EXTENSION_PROPERTY);

        isTypeName = kotlinScope.declareName("isType");
        modulesMap = kotlin("modules");
    }

    @NotNull
    public JsExpression classCreationMethodReference() {
        return kotlin(className);
    }

    @NotNull
    public JsExpression enumClassCreationMethodReference() {
        return kotlin(enumClassName);
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
    public JsExpression callableRefForMemberFunctionReference() {
        return kotlin(callableRefForMemberFunctionName);
    }

    @NotNull
    public JsExpression callableRefForExtensionFunctionReference() {
        return kotlin(callableRefForExtensionFunctionName);
    }

    @NotNull
    public JsExpression callableRefForLocalExtensionFunctionReference() {
        return kotlin(callableRefForLocalExtensionFunctionName);
    }

    @NotNull
    public JsExpression callableRefForConstructorReference() {
        return kotlin(callableRefForConstructorName);
    }

    @NotNull
    public JsExpression callableRefForTopLevelPropertyReference() {
        return kotlin(callableRefForTopLevelProperty);
    }

    @NotNull
    public JsExpression callableRefForMemberPropertyReference() {
        return kotlin(callableRefForMemberProperty);
    }

    @NotNull
    public JsExpression callableRefForExtensionPropertyReference() {
        return kotlin(callableRefForExtensionProperty);
    }

    @NotNull
    public JsExpression throwNPEFunctionRef() {
        return new JsNameRef(THROW_NPE_FUN_NAME, kotlinObject());
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
    public JsExpression isTypeOf(@NotNull JsExpression type) {
        JsInvocation invocation = new JsInvocation(kotlin("isTypeOf"), type);
        MetadataProperties.setTypeCheck(invocation, TypeCheck.TYPEOF);
        return invocation;
    }

    @NotNull
    public JsExpression isInstanceOf(@NotNull JsExpression type) {
        JsInvocation invocation = new JsInvocation(kotlin("isInstanceOf"), type);
        MetadataProperties.setTypeCheck(invocation, TypeCheck.INSTANCEOF);
        return invocation;
    }

    @NotNull
    public JsExpression isInstanceOf(@NotNull JsExpression instance, @NotNull JsExpression type) {
        return new JsInvocation(kotlin(isTypeName), instance, type);
    }

    @NotNull
    /*package*/ JsObjectScope getKotlinScope() {
        return kotlinScope;
    }

    @NotNull
    static String generatePackageName(@NotNull FqName packageFqName) {
        return packageFqName.isRoot() ? getRootPackageName() : packageFqName.shortName().asString();
    }

    @NotNull
    public JsExpression classCreateInvocation(@NotNull ClassDescriptor descriptor) {
        switch (descriptor.getKind()) {
            case INTERFACE:
                return traitCreationMethodReference();
            case ENUM_CLASS:
                return enumClassCreationMethodReference();
            case ENUM_ENTRY:
            case OBJECT:
                return objectCreationMethodReference();
            case ANNOTATION_CLASS:
            case CLASS:
                return DescriptorUtils.isAnonymousObject(descriptor)
                       ? objectCreationMethodReference()
                       : classCreationMethodReference();
            default:
                throw new UnsupportedOperationException("Unsupported class kind: " + descriptor);
        }
    }

    @NotNull
    public JsExpression getUndefinedExpression() {
        return UNDEFINED_EXPRESSION;
    }

    @NotNull
    public JsExpression getCallGetProperty() {
        return callGetProperty;
    }

    @NotNull
    public JsExpression getCallSetProperty() {
        return callSetProperty;
    }

    @NotNull
    public JsExpression getModuleReference(@NotNull JsStringLiteral moduleName) {
        return new JsArrayAccess(modulesMap, moduleName);
    }
}
