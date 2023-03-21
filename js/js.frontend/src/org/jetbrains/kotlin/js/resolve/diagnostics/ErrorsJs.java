/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.diagnostics.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.diagnostics.PositioningStrategies.*;
import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;
import static org.jetbrains.kotlin.diagnostics.Severity.WARNING;

public interface ErrorsJs {
    DiagnosticFactory1<KtElement, KotlinType> NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<KtElement, String> NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<KtElement, String> NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtDeclaration> NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE = DiagnosticFactory0.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory0<KtDeclaration> NATIVE_SETTER_WRONG_RETURN_TYPE = DiagnosticFactory0.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory2<KtElement, Integer, String> NATIVE_INDEXER_WRONG_PARAMETER_COUNT = DiagnosticFactory2.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<KtExpression, JsCallData> JSCODE_ERROR = DiagnosticFactory1.create(ERROR, JsCodePositioningStrategy.INSTANCE);
    DiagnosticFactory1<KtExpression, JsCallData> JSCODE_WARNING = DiagnosticFactory1.create(WARNING, JsCodePositioningStrategy.INSTANCE);
    DiagnosticFactory0<KtExpression> JSCODE_ARGUMENT_SHOULD_BE_CONSTANT = DiagnosticFactory0.create(ERROR, DEFAULT);
    DiagnosticFactory1<KtElement, KtElement> NOT_SUPPORTED = DiagnosticFactory1.create(ERROR, DEFAULT);
    DiagnosticFactory0<KtExpression> JSCODE_NO_JAVASCRIPT_PRODUCED = DiagnosticFactory0.create(ERROR, DEFAULT);
    DiagnosticFactory1<KtExpression, String> WRONG_EXTERNAL_DECLARATION = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtElement> EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtExpression> NESTED_EXTERNAL_DECLARATION = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtElement> INLINE_CLASS_IN_EXTERNAL_DECLARATION = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtElement> ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING = DiagnosticFactory0.create(WARNING, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtElement> INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING = DiagnosticFactory0.create(WARNING, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory2<KtElement, String, DeclarationDescriptor> JS_NAME_CLASH = DiagnosticFactory2.create(
            ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory3<KtElement, String, DeclarationDescriptor, DeclarationDescriptor> JS_FAKE_NAME_CLASH =
            DiagnosticFactory3.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<KtElement, String> JS_BUILTIN_NAME_CLASH = DiagnosticFactory1.create(
            ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<PsiElement> JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> JS_NAME_ON_ACCESSOR_AND_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> JS_NAME_IS_NOT_ON_ALL_ACCESSORS = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<PsiElement> JS_NAME_PROHIBITED_FOR_OVERRIDE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> JS_NAME_PROHIBITED_FOR_EXTENSION_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> JS_NAME_PROHIBITED_FOR_NAMED_NATIVE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> NAME_CONTAINS_ILLEGAL_CHARS = DiagnosticFactory0.create(
            ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory0<KtElement> JS_MODULE_PROHIBITED_ON_VAR = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtElement> JS_MODULE_PROHIBITED_ON_NON_NATIVE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtElement> NESTED_JS_MODULE_PROHIBITED = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<PsiElement, DeclarationDescriptor> CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM =
            DiagnosticFactory1.create(ERROR, DEFAULT);
    DiagnosticFactory1<PsiElement, DeclarationDescriptor> CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM =
            DiagnosticFactory1.create(ERROR, DEFAULT);
    DiagnosticFactory0<PsiElement> CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE = DiagnosticFactory0.create(ERROR, DEFAULT);

    DiagnosticFactory1<KtElement, KotlinType> NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE =
            DiagnosticFactory1.create(ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtValueArgument> WRONG_JS_QUALIFIER = DiagnosticFactory0.create(ERROR, PositioningStrategies.DEFAULT);

    DiagnosticFactory1<PsiElement, KotlinType> CANNOT_CHECK_FOR_EXTERNAL_INTERFACE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, KotlinType, KotlinType> UNCHECKED_CAST_TO_EXTERNAL_INTERFACE = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<PsiElement, KotlinType> EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> EXTERNAL_INTERFACE_AS_CLASS_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtElement> EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE = DiagnosticFactory0.create(
            ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory1<PsiElement, String> WRONG_OPERATION_WITH_DYNAMIC = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> SPREAD_OPERATOR_IN_DYNAMIC_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> DELEGATION_BY_DYNAMIC = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> PROPERTY_DELEGATION_BY_DYNAMIC = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION = DiagnosticFactory0.create(
            ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<PsiElement> RUNTIME_ANNOTATION_NOT_SUPPORTED = DiagnosticFactory0.create(WARNING, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtElement> OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS =
            DiagnosticFactory0.create(ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<KtElement, FunctionDescriptor> OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE =
            DiagnosticFactory1.create(ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtClassOrObject> IMPLEMENTING_FUNCTION_INTERFACE = DiagnosticFactory0.create(
            ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtDeclaration> INLINE_EXTERNAL_DECLARATION = DiagnosticFactory0.create(
            ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtExpression> NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE = DiagnosticFactory0.create(
            ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtExpression> NESTED_CLASS_IN_EXTERNAL_INTERFACE = DiagnosticFactory0.create(
            ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT);

    // Diagnostics about exposing implementation detail in external declarations
    DiagnosticFactory0<KtExpression> WRONG_BODY_OF_EXTERNAL_DECLARATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtElement> EXTERNAL_DELEGATED_CONSTRUCTOR_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtElement> EXTERNAL_DELEGATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnonymousInitializer> EXTERNAL_ANONYMOUS_INITIALIZER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtClassBody> EXTERNAL_ENUM_ENTRY_WITH_BODY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtParameter> EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, CallableMemberDescriptor> WRONG_MULTIPLE_INHERITANCE =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory0<PsiElement> NESTED_JS_EXPORT = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<KtExpression, String> WRONG_EXPORTED_DECLARATION = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory2<PsiElement, String, KotlinType> NON_EXPORTABLE_TYPE = DiagnosticFactory2.create(WARNING, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory1<PsiElement, String> NON_CONSUMABLE_EXPORTED_IDENTIFIER = DiagnosticFactory1.create(WARNING, DEFAULT);

    DiagnosticFactory2<PsiElement, DeclarationDescriptor, DeclarationDescriptor> JS_EXTERNAL_INHERITORS_ONLY = DiagnosticFactory2.create(
            ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory1<PsiElement, KotlinType> JS_EXTERNAL_ARGUMENT = DiagnosticFactory1.create(
            ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(ErrorsJs.class);
        }
    };
}
