/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.google.dart.compiler.backend.js.ast.metadata.SideEffectKind;
import com.google.dart.compiler.backend.js.ast.metadata.TypeCheck;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.js.naming.NameSuggestion;
import org.jetbrains.kotlin.js.naming.SuggestedName;
import org.jetbrains.kotlin.js.resolve.JsPlatform;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.dart.compiler.backend.js.ast.JsScopesKt.JsObjectScope;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn;
import static org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getModuleName;

/**
 * Encapsulates different types of constants and naming conventions.
 */
public final class Namer {
    public static final String KOTLIN_NAME = KotlinLanguage.NAME;
    public static final String KOTLIN_LOWER_NAME = KOTLIN_NAME.toLowerCase();

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
    public static final String IS_CHAR_SEQUENCE = "isCharSequence";

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
    private static final String THROW_CLASS_CAST_EXCEPTION_FUN_NAME = "throwCCE";
    private static final String PROTOTYPE_NAME = "prototype";
    private static final String CAPTURED_VAR_FIELD = "v";

    public static final JsNameRef IS_ARRAY_FUN_REF = new JsNameRef("isArray", "Array");
    public static final String DEFINE_INLINE_FUNCTION = "defineInlineFunction";

    private static final JsNameRef JS_OBJECT = new JsNameRef("Object");
    private static final JsNameRef JS_OBJECT_CREATE_FUNCTION = new JsNameRef("create", JS_OBJECT);

    public static final String LOCAL_MODULE_PREFIX = "$module$";

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

        String mangledName = new NameSuggestion().suggest(functionDescriptor).getNames().get(0);
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
        return pureFqn(SUPER_METHOD_NAME, superClassJsName.makeRef());
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
    public static String getPrototypeName() {
        return PROTOTYPE_NAME;
    }

    @NotNull
    public static JsNameRef getRefToPrototype(@NotNull JsExpression classOrTraitExpression) {
        return pureFqn(getPrototypeName(), classOrTraitExpression);
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
        return pureFqn(CALL_FUNCTION, functionExpression);
    }

    @NotNull
    public static JsNameRef getFunctionApplyRef(@NotNull JsExpression functionExpression) {
        return pureFqn(APPLY_FUNCTION, functionExpression);
    }

    @NotNull
    public static JsInvocation createObjectWithPrototypeFrom(JsNameRef referenceToClass) {
        return new JsInvocation(JS_OBJECT_CREATE_FUNCTION, getRefToPrototype(referenceToClass));
    }

    @NotNull
    public static JsNameRef getCapturedVarAccessor(@NotNull JsExpression ref) {
        return pureFqn(CAPTURED_VAR_FIELD, ref);
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

    // TODO: get rid of this function
    @NotNull
    public static String getStableMangledNameForDescriptor(@NotNull ClassDescriptor descriptor, @NotNull String functionName) {
        Collection<SimpleFunctionDescriptor> functions = descriptor.getDefaultType().getMemberScope().getContributedFunctions(
                Name.identifier(functionName), NoLookupLocation.FROM_BACKEND);
        assert functions.size() == 1 : "Can't select a single function: " + functionName + " in " + descriptor;
        SuggestedName suggested = new NameSuggestion().suggest(functions.iterator().next());
        assert suggested != null : "Suggested name for class members is always non-null: " + functions.iterator().next();
        return suggested.getNames().get(0);
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
    public static JsExpression throwNPEFunctionRef() {
        return new JsNameRef(THROW_NPE_FUN_NAME, kotlinObject());
    }

    @NotNull
    public JsExpression throwClassCastExceptionFunRef() {
        return new JsNameRef(THROW_CLASS_CAST_EXCEPTION_FUN_NAME, kotlinObject());
    }

    @NotNull
    public static JsNameRef kotlin(@NotNull JsName name) {
        return pureFqn(name, kotlinObject());
    }

    @NotNull
    public JsNameRef kotlin(@NotNull String name) {
        return kotlin(kotlinScope.declareName(name));
    }

    @NotNull
    public static JsNameRef kotlinObject() {
        return pureFqn(KOTLIN_NAME, null);
    }

    @NotNull
    public JsExpression isTypeOf(@NotNull JsExpression type) {
        return invokeFunctionAndSetTypeCheckMetadata("isTypeOf", type, TypeCheck.TYPEOF);
    }

    @NotNull
    public JsExpression isInstanceOf(@NotNull JsExpression type) {
        return invokeFunctionAndSetTypeCheckMetadata("isInstanceOf", type, TypeCheck.INSTANCEOF);
    }

    @NotNull
    public JsExpression isInstanceOfObject(@NotNull JsExpression type) {
        return invokeFunctionAndSetTypeCheckMetadata("isInstanceOf", type, TypeCheck.SAME_AS);
    }

    @NotNull
    public JsExpression orNull(@NotNull JsExpression callable) {
        return invokeFunctionAndSetTypeCheckMetadata("orNull", callable, TypeCheck.OR_NULL);
    }

    @NotNull
    public JsExpression andPredicate(@NotNull JsExpression a, @NotNull JsExpression b) {
        return invokeFunctionAndSetTypeCheckMetadata("andPredicate", Arrays.asList(a, b), TypeCheck.AND_PREDICATE);
    }

    @NotNull
    public JsExpression isAny() {
        return invokeFunctionAndSetTypeCheckMetadata("isAny", Collections.<JsExpression>emptyList(), TypeCheck.IS_ANY);
    }

    @NotNull
    public JsExpression isComparable() {
        return kotlin("isComparable");
    }

    @NotNull
    public JsExpression isCharSequence() {
        return kotlin(IS_CHAR_SEQUENCE);
    }

    @NotNull
    private JsExpression invokeFunctionAndSetTypeCheckMetadata(
            @NotNull String functionName,
            @Nullable JsExpression argument,
            @NotNull TypeCheck metadata
    ) {
        List<JsExpression> arguments = argument != null ? Collections.singletonList(argument) : Collections.<JsExpression>emptyList();
        return invokeFunctionAndSetTypeCheckMetadata(functionName, arguments, metadata);
    }

    @NotNull
    private JsExpression invokeFunctionAndSetTypeCheckMetadata(
            @NotNull String functionName,
            @NotNull List<JsExpression> arguments,
            @NotNull TypeCheck metadata
    ) {
        JsInvocation invocation = new JsInvocation(kotlin(functionName));
        invocation.getArguments().addAll(arguments);
        MetadataProperties.setTypeCheck(invocation, metadata);
        MetadataProperties.setSideEffects(invocation, SideEffectKind.PURE);
        return invocation;
    }

    @NotNull
    public JsExpression isInstanceOf(@NotNull JsExpression instance, @NotNull JsExpression type) {
        JsInvocation result = new JsInvocation(kotlin(isTypeName), instance, type);
        MetadataProperties.setSideEffects(result, SideEffectKind.PURE);
        return result;
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
                return classCreationMethodReference();
            default:
                throw new UnsupportedOperationException("Unsupported class kind: " + descriptor);
        }
    }

    @NotNull
    public static JsExpression getUndefinedExpression() {
        return new JsPrefixOperation(JsUnaryOperator.VOID, JsNumberLiteral.ZERO);
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
        JsArrayAccess result = new JsArrayAccess(modulesMap, moduleName);
        MetadataProperties.setSideEffects(result, SideEffectKind.PURE);
        return result;
    }

    public static JsNameRef kotlinLong() {
        return pureFqn("Long", kotlinObject());
    }

    @NotNull
    public static JsNameRef createInlineFunction() {
        return pureFqn(DEFINE_INLINE_FUNCTION, kotlinObject());
    }

    @NotNull
    public static String suggestedModuleName(@NotNull String id) {
        if (id.isEmpty()) {
            return "_";
        }

        StringBuilder sb = new StringBuilder(id.length());
        char c = id.charAt(0);
        if (Character.isJavaIdentifierStart(c)) {
            sb.append(c);
        }
        else {
            sb.append('_');
            if (Character.isJavaIdentifierPart(c)) {
                sb.append(c);
            }
        }

        for (int i = 1; i < id.length(); ++i) {
            c = id.charAt(i);
            sb.append(Character.isJavaIdentifierPart(c) ? c : '_');
        }

        return sb.toString();
    }

    public static boolean requiresEscaping(@NotNull String name) {
        // TODO: remove if there is existing implementation of this method
        // TODO: handle JavaScript keywords
        if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0))) return true;
        for (int i = 1; i < name.length(); ++i) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) return true;
        }
        return false;
    }
}
