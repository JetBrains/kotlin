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

package org.jetbrains.kotlin.js.translate.utils;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptorKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.List;

import static org.jetbrains.kotlin.js.translate.utils.ErrorReportingUtils.message;
import static org.jetbrains.kotlin.resolve.BindingContext.INDEXED_LVALUE_GET;
import static org.jetbrains.kotlin.resolve.BindingContext.INDEXED_LVALUE_SET;

/**
 * This class contains some code related to BindingContext use. Intention is not to pollute other classes.
 * Every call to BindingContext.get() is supposed to be wrapped by this utility class.
 */
public final class BindingUtils {

    private BindingUtils() {
    }

    @NotNull
    static private <E extends PsiElement, D extends DeclarationDescriptor>
    D getDescriptorForExpression(@NotNull BindingContext context, @NotNull E expression, Class<D> descriptorClass) {
        DeclarationDescriptor descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, expression);
        assert descriptor != null;
        assert descriptorClass.isInstance(descriptor)
                : message(expression, expression.toString() + " expected to have of type" + descriptorClass.toString());
        //noinspection unchecked
        return (D) descriptor;
    }

    @NotNull
    public static ClassDescriptor getClassDescriptor(@NotNull BindingContext context,
            @NotNull KtPureClassOrObject declaration) {
        return SyntheticClassOrObjectDescriptorKt.findClassDescriptor(declaration, context);
    }

    @NotNull
    public static FunctionDescriptor getFunctionDescriptor(@NotNull BindingContext context,
            @NotNull KtDeclarationWithBody declaration) {
        return getDescriptorForExpression(context, declaration, FunctionDescriptor.class);
    }

    @NotNull
    public static PropertyDescriptor getPropertyDescriptor(@NotNull BindingContext context,
            @NotNull KtProperty declaration) {
        return getDescriptorForExpression(context, declaration, PropertyDescriptor.class);
    }

    @NotNull
    private static KtParameter getParameterForDescriptor(@NotNull ValueParameterDescriptor descriptor) {
        PsiElement result = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
        assert result instanceof KtParameter : message(descriptor, "ValueParameterDescriptor should have corresponding JetParameter");
        return (KtParameter) result;
    }

    public static boolean hasAncestorClass(@NotNull BindingContext context, @NotNull KtPureClassOrObject classDeclaration) {
        ClassDescriptor classDescriptor = getClassDescriptor(context, classDeclaration);
        List<ClassDescriptor> superclassDescriptors = DescriptorUtils.getSuperclassDescriptors(classDescriptor);
        return (JsDescriptorUtils.findAncestorClass(superclassDescriptors) != null);
    }

    @NotNull
    public static KotlinType getTypeByReference(@NotNull BindingContext context,
            @NotNull KtTypeReference typeReference) {
        return BindingContextUtils.getNotNull(context, BindingContext.TYPE, typeReference);
    }

    @Nullable
    public static PropertyDescriptor getPropertyDescriptorForConstructorParameter(@NotNull BindingContext context,
            @NotNull KtParameter parameter) {
        return context.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
    }

    @Nullable
    public static DeclarationDescriptor getDescriptorForReferenceExpression(@NotNull BindingContext context,
            @NotNull KtReferenceExpression reference) {
        if (BindingContextUtils.isExpressionWithValidReference(reference, context)) {
            return resolveObjectViaTypeAlias(BindingContextUtils.getNotNull(context, BindingContext.REFERENCE_TARGET, reference));
        }
        return null;
    }

    @Nullable
    private static DeclarationDescriptor getNullableDescriptorForReferenceExpression(@NotNull BindingContext context,
            @NotNull KtReferenceExpression reference) {
        DeclarationDescriptor descriptor = context.get(BindingContext.REFERENCE_TARGET, reference);
        return descriptor != null ? resolveObjectViaTypeAlias(descriptor) : null;
    }

    @NotNull
    private static DeclarationDescriptor resolveObjectViaTypeAlias(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof TypeAliasDescriptor) {
            ClassDescriptor classDescriptor = ((TypeAliasDescriptor) descriptor).getClassDescriptor();
            assert classDescriptor != null : "Class descriptor must be non-null in resolved typealias: " + descriptor;
            if (classDescriptor.getKind() != ClassKind.OBJECT && classDescriptor.getKind() != ClassKind.ENUM_CLASS) {
                classDescriptor = classDescriptor.getCompanionObjectDescriptor();
                assert classDescriptor != null : "Resolved typealias must have non-null class descriptor: " + descriptor;
            }
            return classDescriptor;
        }
        else {
            return descriptor;
        }
    }

    public static boolean isVariableReassignment(@NotNull BindingContext context, @NotNull KtExpression expression) {
        return BindingContextUtils.getNotNull(context, BindingContext.VARIABLE_REASSIGNMENT, expression);
    }

    @Nullable
    public static CallableDescriptor getCallableDescriptorForOperationExpression(
            @NotNull BindingContext context,
            @NotNull KtOperationExpression expression
    ) {
        KtSimpleNameExpression operationReference = expression.getOperationReference();
        DeclarationDescriptor descriptorForReferenceExpression =
                getNullableDescriptorForReferenceExpression(context, operationReference);

        if (descriptorForReferenceExpression == null) return null;

        assert descriptorForReferenceExpression instanceof CallableDescriptor :
                message(operationReference, "Operation should resolve to callable descriptor");
        return (CallableDescriptor) descriptorForReferenceExpression;
    }

    @NotNull
    public static DeclarationDescriptor getDescriptorForElement(@NotNull BindingContext context,
            @NotNull PsiElement element) {
        return BindingContextUtils.getNotNull(context, BindingContext.DECLARATION_TO_DESCRIPTOR, element);
    }

    @Nullable
    public static Object getCompileTimeValue(@NotNull BindingContext context, @NotNull KtExpression expression) {
        CompileTimeConstant<?> compileTimeValue = ConstantExpressionEvaluator.getConstant(expression, context);
        if (compileTimeValue != null) {
            return getCompileTimeValue(context, expression, compileTimeValue);
        }
        return null;
    }

    @Nullable
    public static Object getCompileTimeValue(@NotNull BindingContext context, @NotNull KtExpression expression, @NotNull CompileTimeConstant<?> constant) {
        KotlinType expectedType = context.getType(expression);
        return constant.getValue(expectedType == null ? TypeUtils.NO_EXPECTED_TYPE : expectedType);
    }

    @NotNull
    public static KtExpression getDefaultArgument(@NotNull ValueParameterDescriptor parameterDescriptor) {
        KtParameter psiParameter = getParameterForDescriptor(parameterDescriptor);
        KtExpression defaultValue = psiParameter.getDefaultValue();
        assert defaultValue != null : message(parameterDescriptor, "No default value found in PSI");
        return defaultValue;
    }

    @NotNull
    public static ResolvedCall<FunctionDescriptor> getIteratorFunction(@NotNull BindingContext context,
            @NotNull KtExpression rangeExpression) {
        return BindingContextUtils.getNotNull(context, BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, rangeExpression);
    }

    @NotNull
    public static ResolvedCall<FunctionDescriptor> getNextFunction(@NotNull BindingContext context,
            @NotNull KtExpression rangeExpression) {
        return BindingContextUtils.getNotNull(context, BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, rangeExpression);
    }

    @NotNull
    public static ResolvedCall<FunctionDescriptor> getHasNextCallable(@NotNull BindingContext context,
            @NotNull KtExpression rangeExpression) {
        return BindingContextUtils.getNotNull(context, BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, rangeExpression);
    }

    @NotNull
    public static KotlinType getTypeForExpression(@NotNull BindingContext context,
            @NotNull KtExpression expression) {
        return BindingContextUtils.getTypeNotNull(context, expression);
    }

    @NotNull
    public static ResolvedCall<FunctionDescriptor> getResolvedCallForArrayAccess(@NotNull BindingContext context,
            @NotNull KtArrayAccessExpression arrayAccessExpression,
            boolean isGet) {
        return BindingContextUtils.getNotNull(context, isGet ? INDEXED_LVALUE_GET : INDEXED_LVALUE_SET, arrayAccessExpression);
    }


    @Nullable
    @SuppressWarnings("unchecked")
    public static ResolvedCall<FunctionDescriptor> getSuperCall(@NotNull BindingContext context, KtPureClassOrObject classDeclaration) {
        for (KtSuperTypeListEntry specifier : classDeclaration.getSuperTypeListEntries()) {
            if (specifier instanceof KtSuperTypeCallEntry) {
                KtSuperTypeCallEntry superCall = (KtSuperTypeCallEntry) specifier;
                return (ResolvedCall<FunctionDescriptor>) CallUtilKt.getResolvedCallWithAssert(superCall, context);
            }
        }
        return null;
    }
}
