/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind;
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction;
import org.jetbrains.kotlin.js.backend.ast.metadata.TypeCheck;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.naming.NameSuggestion;
import org.jetbrains.kotlin.js.naming.SuggestedName;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.ArrayFIF;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn;
import static org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getModuleName;

/**
 * Encapsulates different types of constants and naming conventions.
 */
public final class Namer {
    public static final String KOTLIN_NAME = KotlinLanguage.NAME;
    public static final String KOTLIN_LOWER_NAME = KOTLIN_NAME.toLowerCase();

    public static final String EQUALS_METHOD_NAME = "equals";
    public static final String COMPARE_TO_METHOD_NAME = "compareTo_11rb$";
    public static final String LONG_FROM_NUMBER = "fromNumber";
    public static final String LONG_TO_NUMBER = "toNumber";
    public static final String LONG_FROM_INT = "fromInt";
    public static final String UINT_FROM_INT = "toUInt";
    public static final String LONG_ZERO = "ZERO";
    public static final String LONG_ONE = "ONE";
    public static final String LONG_NEG_ONE = "NEG_ONE";
    public static final String LONG_MAX_VALUE = "MAX_VALUE";
    public static final String LONG_MIN_VALUE = "MIN_VALUE";
    public static final String PRIMITIVE_COMPARE_TO = "primitiveCompareTo";
    public static final String IS_CHAR = "isChar";
    public static final String IS_NUMBER = "isNumber";
    private static final String IS_CHAR_SEQUENCE = "isCharSequence";
    public static final String GET_KCLASS = "getKClass";
    public static final String GET_KCLASS_FROM_EXPRESSION = "getKClassFromExpression";

    public static final String CREATE_KTYPE = "createKType";
    public static final String CREATE_DYNAMIC_KTYPE = "createDynamicKType";
    public static final String MARK_KTYPE_NULLABLE = "markKTypeNullable";
    public static final String CREATE_KTYPE_PARAMETER = "createKTypeParameter";
    public static final String CREATE_REIFIED_KTYPE_PARAMETER = "getReifiedTypeParameterKType";
    public static final String GET_START_KTYPE_PROJECTION = "getStarKTypeProjection";
    public static final String CREATE_COVARIANT_KTYPE_PROJECTION = "createCovariantKTypeProjection";
    public static final String CREATE_INVARIANT_KTYPE_PROJECTION = "createInvariantKTypeProjection";
    public static final String CREATE_CONTRAVARIANT_KTYPE_PROJECTION = "createContravariantKTypeProjection";

    public static final String CALLEE_NAME = "$fun";

    public static final String CALL_FUNCTION = "call";
    private static final String APPLY_FUNCTION = "apply";

    public static final String OUTER_FIELD_NAME = "$outer";

    private static final String DELEGATE = "$delegate";

    private static final String ROOT_PACKAGE = "_";

    private static final String RECEIVER_PARAMETER_NAME = "$receiver";
    public static final String ANOTHER_THIS_PARAMETER_NAME = "$this";

    public static final String THROW_CLASS_CAST_EXCEPTION_FUN_NAME = "throwCCE";
    public static final String THROW_ILLEGAL_ARGUMENT_EXCEPTION_FUN_NAME = "throwIAE";
    public static final String THROW_UNINITIALIZED_PROPERTY_ACCESS_EXCEPTION = "throwUPAE";
    public static final String NULL_CHECK_INTRINSIC_NAME = "ensureNotNull";
    private static final String PROTOTYPE_NAME = "prototype";
    private static final String CAPTURED_VAR_FIELD = "v";

    public static final JsNameRef IS_ARRAY_FUN_REF = new JsNameRef("isArray", "Array");
    public static final String DEFINE_INLINE_FUNCTION = "defineInlineFunction";
    public static final String DEFAULT_PARAMETER_IMPLEMENTOR_SUFFIX = "$default";

    private static final JsNameRef JS_OBJECT = new JsNameRef("Object");
    private static final JsNameRef JS_OBJECT_CREATE_FUNCTION = new JsNameRef("create", JS_OBJECT);

    public static final String LOCAL_MODULE_PREFIX = "$module$";
    public static final String METADATA = "$metadata$";
    public static final String METADATA_SUPERTYPES = "interfaces";
    public static final String METADATA_SIMPLE_NAME = "simpleName";
    public static final String METADATA_CLASS_KIND = "kind";
    public static final String CLASS_KIND_ENUM = "Kind";
    public static final String CLASS_KIND_CLASS = "CLASS";
    public static final String CLASS_KIND_INTERFACE = "INTERFACE";
    public static final String CLASS_KIND_OBJECT = "OBJECT";

    public static final String OBJECT_INSTANCE_VAR_SUFFIX = "_instance";
    public static final String OBJECT_INSTANCE_FUNCTION_SUFFIX = "_getInstance";

    public static final String ENUM_NAME_FIELD = "name$";
    public static final String ENUM_ORDINAL_FIELD = "ordinal$";

    public static final String IMPORTS_FOR_INLINE_PROPERTY = "$$importsForInline$$";

    public static final String SAM_FIELD_NAME = "function$";

    @NotNull
    public static String getFunctionTag(
            @NotNull CallableDescriptor functionDescriptor,
            @NotNull JsConfig config,
            @NotNull BindingContext bindingContext
    ) {
        String intrinsicTag = ArrayFIF.INSTANCE.getTag(functionDescriptor, config);
        if (intrinsicTag != null) return intrinsicTag;

        functionDescriptor = (CallableDescriptor) JsDescriptorUtils.findRealInlineDeclaration(functionDescriptor);
        String moduleName = getModuleName(functionDescriptor);
        FqNameUnsafe fqNameParent = DescriptorUtils.getFqName(functionDescriptor).parent();
        String qualifier = null;

        if (!fqNameParent.isRoot()) {
            qualifier = fqNameParent.asString();
        }

        SuggestedName suggestedName = new NameSuggestion().suggest(functionDescriptor, bindingContext);
        assert suggestedName != null : "Suggested name can be null only for module descriptors: " + functionDescriptor;
        String mangledName = suggestedName.getNames().get(0);
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
    public static String getPrototypeName() {
        return PROTOTYPE_NAME;
    }

    @NotNull
    public static String getDelegatePrefix() {
        return DELEGATE;
    }

    @NotNull
    public static JsNameRef getFunctionCallRef(@NotNull JsExpression functionExpression) {
        JsNameRef result = pureFqn(CALL_FUNCTION, functionExpression);
        MetadataProperties.setJsCall(result, true);
        return result;
    }

    @NotNull
    public static JsNameRef getFunctionApplyRef(@NotNull JsExpression functionExpression) {
        return pureFqn(APPLY_FUNCTION, functionExpression);
    }

    @NotNull
    public static JsInvocation createObjectWithPrototypeFrom(@NotNull JsExpression referenceToClass) {
        return new JsInvocation(JS_OBJECT_CREATE_FUNCTION.deepCopy(), JsAstUtils.prototypeOf(referenceToClass));
    }

    @NotNull
    public static JsNameRef getCapturedVarAccessor(@NotNull JsExpression ref) {
        JsNameRef result = new JsNameRef(CAPTURED_VAR_FIELD, ref);
        MetadataProperties.setSideEffects(result, SideEffectKind.DEPENDS_ON_STATE);
        return result;
    }

    @NotNull
    public static String isInstanceSuggestedName(@NotNull TypeParameterDescriptor descriptor) {
        return "is" + NameSuggestion.sanitizeName(descriptor.getName().getIdentifier());
    }

    @NotNull
    public static Namer newInstance(@NotNull JsScope rootScope) {
        return new Namer(rootScope);
    }

    @NotNull
    private final JsObjectScope kotlinScope;
    @NotNull
    public static final String FUNCTION_CALLABLE_REF = "getCallableRef";
    @NotNull
    public static final String PROPERTY_CALLABLE_REF = "getPropertyCallableRef";
    @NotNull
    private final JsExpression callGetProperty;
    @NotNull
    private final JsExpression callSetProperty;

    private Namer(@NotNull JsScope rootScope) {
        kotlinScope = new JsObjectScope(rootScope, "Kotlin standard object");

        callGetProperty = kotlin("callGetter");
        callSetProperty = kotlin("callSetter");
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
    public JsExpression orNull(@NotNull JsExpression callable) {
        return invokeFunctionAndSetTypeCheckMetadata("orNull", callable, TypeCheck.OR_NULL);
    }

    @NotNull
    public JsExpression andPredicate(@NotNull JsExpression a, @NotNull JsExpression b) {
        return invokeFunctionAndSetTypeCheckMetadata("andPredicate", Arrays.asList(a, b), TypeCheck.AND_PREDICATE);
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
    public JsExpression isArray() {
        return kotlin("isArray");
    }

    @NotNull
    public JsExpression isPrimitiveArray(@NotNull PrimitiveType type) {
        return kotlin("is" + type.getArrayTypeName().asString());
    }

    @NotNull
    private JsExpression invokeFunctionAndSetTypeCheckMetadata(
            @NotNull String functionName,
            @Nullable JsExpression argument,
            @NotNull TypeCheck metadata
    ) {
        List<JsExpression> arguments = argument != null ? Collections.singletonList(argument) : Collections.emptyList();
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
    public static JsExpression isInstanceOf(@NotNull JsExpression instance, @NotNull JsExpression type) {
        JsInvocation result = new JsInvocation(new JsNameRef("isType", KOTLIN_NAME), instance, type);
        MetadataProperties.setSideEffects(result, SideEffectKind.PURE);
        return result;
    }

    @NotNull
    public static JsExpression getUndefinedExpression() {
        return new JsPrefixOperation(JsUnaryOperator.VOID, new JsIntLiteral(0));
    }

    @NotNull
    public JsExpression getCallGetProperty() {
        return callGetProperty.deepCopy();
    }

    @NotNull
    public JsExpression getCallSetProperty() {
        return callSetProperty.deepCopy();
    }

    public static JsNameRef kotlinLong() {
        return pureFqn("Long", kotlinObject());
    }

    @NotNull
    public static JsExpression createSpecialFunction(@NotNull SpecialFunction specialFunction) {
        return pureFqn(specialFunction.getSuggestedName(), kotlinObject());
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

    public static JsNameRef imul() {
        return pureFqn("imul", kotlinObject());
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
