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
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant;
import org.jetbrains.kotlin.types.JetType;
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
            @NotNull JetClassOrObject declaration) {
        return BindingContextUtils.getNotNull(context, BindingContext.CLASS, declaration);
    }

    @NotNull
    public static FunctionDescriptor getFunctionDescriptor(@NotNull BindingContext context,
            @NotNull JetDeclarationWithBody declaration) {
        return getDescriptorForExpression(context, declaration, FunctionDescriptor.class);
    }

    @NotNull
    public static PropertyDescriptor getPropertyDescriptor(@NotNull BindingContext context,
            @NotNull JetProperty declaration) {
        return getDescriptorForExpression(context, declaration, PropertyDescriptor.class);
    }

    @NotNull
    private static JetParameter getParameterForDescriptor(@NotNull ValueParameterDescriptor descriptor) {
        PsiElement result = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
        assert result instanceof JetParameter : message(descriptor, "ValueParameterDescriptor should have corresponding JetParameter");
        return (JetParameter) result;
    }

    public static boolean hasAncestorClass(@NotNull BindingContext context, @NotNull JetClassOrObject classDeclaration) {
        ClassDescriptor classDescriptor = getClassDescriptor(context, classDeclaration);
        List<ClassDescriptor> superclassDescriptors = DescriptorUtils.getSuperclassDescriptors(classDescriptor);
        return (JsDescriptorUtils.findAncestorClass(superclassDescriptors) != null);
    }

    @NotNull
    public static JetType getTypeByReference(@NotNull BindingContext context,
            @NotNull JetTypeReference typeReference) {
        return BindingContextUtils.getNotNull(context, BindingContext.TYPE, typeReference);
    }

    @NotNull
    public static ClassDescriptor getClassDescriptorForTypeReference(@NotNull BindingContext context,
            @NotNull JetTypeReference typeReference) {
        return DescriptorUtils.getClassDescriptorForType(getTypeByReference(context, typeReference));
    }

    @Nullable
    public static PropertyDescriptor getPropertyDescriptorForConstructorParameter(@NotNull BindingContext context,
            @NotNull JetParameter parameter) {
        return context.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
    }

    @Nullable
    public static DeclarationDescriptor getDescriptorForReferenceExpression(@NotNull BindingContext context,
            @NotNull JetReferenceExpression reference) {
        if (BindingContextUtils.isExpressionWithValidReference(reference, context)) {
            return BindingContextUtils.getNotNull(context, BindingContext.REFERENCE_TARGET, reference);
        }
        return null;
    }

    @Nullable
    public static DeclarationDescriptor getNullableDescriptorForReferenceExpression(@NotNull BindingContext context,
            @NotNull JetReferenceExpression reference) {
        return context.get(BindingContext.REFERENCE_TARGET, reference);
    }

    public static boolean isVariableReassignment(@NotNull BindingContext context, @NotNull JetExpression expression) {
        return BindingContextUtils.getNotNull(context, BindingContext.VARIABLE_REASSIGNMENT, expression);
    }

    @Nullable
    public static CallableDescriptor getCallableDescriptorForOperationExpression(
            @NotNull BindingContext context,
            @NotNull JetOperationExpression expression
    ) {
        JetSimpleNameExpression operationReference = expression.getOperationReference();
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
    public static Object getCompileTimeValue(@NotNull BindingContext context, @NotNull JetExpression expression) {
        CompileTimeConstant<?> compileTimeValue = context.get(BindingContext.COMPILE_TIME_VALUE, expression);
        if (compileTimeValue != null) {
            return getCompileTimeValue(context, expression, compileTimeValue);
        }
        return null;
    }

    @Nullable
    public static Object getCompileTimeValue(@NotNull BindingContext context, @NotNull JetExpression expression, @NotNull CompileTimeConstant<?> constant) {
        if (constant != null) {
            if (constant instanceof IntegerValueTypeConstant) {
                JetType expectedType = context.get(BindingContext.EXPRESSION_TYPE, expression);
                return ((IntegerValueTypeConstant) constant).getValue(expectedType == null ? TypeUtils.NO_EXPECTED_TYPE : expectedType);
            }
            return constant.getValue();
        }
        return null;
    }

    @NotNull
    public static JetExpression getDefaultArgument(@NotNull ValueParameterDescriptor parameterDescriptor) {
        ValueParameterDescriptor descriptorWhichDeclaresDefaultValue =
                getOriginalDescriptorWhichDeclaresDefaultValue(parameterDescriptor);
        JetParameter psiParameter = getParameterForDescriptor(descriptorWhichDeclaresDefaultValue);
        JetExpression defaultValue = psiParameter.getDefaultValue();
        assert defaultValue != null : message(parameterDescriptor, "No default value found in PSI");
        return defaultValue;
    }

    private static ValueParameterDescriptor getOriginalDescriptorWhichDeclaresDefaultValue(
            @NotNull ValueParameterDescriptor parameterDescriptor
    ) {
        ValueParameterDescriptor result = parameterDescriptor;
        assert result.hasDefaultValue() : message(parameterDescriptor, "Unsupplied parameter must have default value");
        while (!result.declaresDefaultValue()) {
            result = result.getOverriddenDescriptors().iterator().next();
        }
        return result;
    }

    @NotNull
    public static ResolvedCall<FunctionDescriptor> getIteratorFunction(@NotNull BindingContext context,
            @NotNull JetExpression rangeExpression) {
        return BindingContextUtils.getNotNull(context, BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, rangeExpression);
    }

    @NotNull
    public static ResolvedCall<FunctionDescriptor> getNextFunction(@NotNull BindingContext context,
            @NotNull JetExpression rangeExpression) {
        return BindingContextUtils.getNotNull(context, BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, rangeExpression);
    }

    @NotNull
    public static ResolvedCall<FunctionDescriptor> getHasNextCallable(@NotNull BindingContext context,
            @NotNull JetExpression rangeExpression) {
        return BindingContextUtils.getNotNull(context, BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, rangeExpression);
    }

    @NotNull
    public static JetType getTypeForExpression(@NotNull BindingContext context,
            @NotNull JetExpression expression) {
        return BindingContextUtils.getNotNull(context, BindingContext.EXPRESSION_TYPE, expression);
    }

    @NotNull
    public static ResolvedCall<FunctionDescriptor> getResolvedCallForArrayAccess(@NotNull BindingContext context,
            @NotNull JetArrayAccessExpression arrayAccessExpression,
            boolean isGet) {
        return BindingContextUtils.getNotNull(context, isGet ? INDEXED_LVALUE_GET : INDEXED_LVALUE_SET, arrayAccessExpression);
    }

    public static ConstructorDescriptor getConstructor(@NotNull BindingContext bindingContext,
            @NotNull JetClassOrObject declaration) {
        ConstructorDescriptor primaryConstructor = getClassDescriptor(bindingContext, declaration).getUnsubstitutedPrimaryConstructor();
        assert primaryConstructor != null : message(declaration, "Traits do not have initialize methods");
        return primaryConstructor;
    }

    @Nullable
    public static SimpleFunctionDescriptor getNullableDescriptorForFunction(@NotNull BindingContext bindingContext,
            @NotNull JetNamedFunction function) {
        return bindingContext.get(BindingContext.FUNCTION, function);
    }
}
