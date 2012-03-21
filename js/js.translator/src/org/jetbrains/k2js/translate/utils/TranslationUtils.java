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

package org.jetbrains.k2js.translate.utils;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.intrinsic.Intrinsic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.dart.compiler.util.AstUtil.newAssignment;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptorForOperationExpression;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptor;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getExpectedReceiverDescriptor;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;

/**
 * @author Pavel Talanov
 */
public final class TranslationUtils {

    private TranslationUtils() {
    }

    @NotNull
    public static JsBinaryOperation notNullCheck(@NotNull TranslationContext context,
                                                 @NotNull JsExpression expressionToCheck) {
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        return inequality(expressionToCheck, nullLiteral);
    }

    @NotNull
    public static JsBinaryOperation isNullCheck(@NotNull TranslationContext context,
                                                @NotNull JsExpression expressionToCheck) {
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        return equality(expressionToCheck, nullLiteral);
    }

    @NotNull
    public static List<JsExpression> translateArgumentList(@NotNull TranslationContext context,
                                                           @NotNull List<? extends ValueArgument> jetArguments) {
        List<JsExpression> jsArguments = new ArrayList<JsExpression>();
        for (ValueArgument argument : jetArguments) {
            jsArguments.add(translateArgument(context, argument));
        }
        return jsArguments;
    }

    @NotNull
    private static JsExpression translateArgument(@NotNull TranslationContext context, @NotNull ValueArgument argument) {
        JetExpression jetExpression = argument.getArgumentExpression();
        assert jetExpression != null : "Argument with no expression";
        return Translation.translateAsExpression(jetExpression, context);
    }

    @NotNull
    public static JsNameRef backingFieldReference(@NotNull TranslationContext context,
                                                  @NotNull PropertyDescriptor descriptor) {
        JsName backingFieldName = context.getNameForDescriptor(descriptor);
        return qualified(backingFieldName, new JsThisRef());
    }

    @NotNull
    public static JsExpression assignmentToBackingField(@NotNull TranslationContext context,
                                                        @NotNull PropertyDescriptor descriptor,
                                                        @NotNull JsExpression assignTo) {
        JsNameRef backingFieldReference = backingFieldReference(context, descriptor);
        return newAssignment(backingFieldReference, assignTo);
    }

    @Nullable
    public static JsExpression translateInitializerForProperty(@NotNull JetProperty declaration,
                                                               @NotNull TranslationContext context) {
        JsExpression jsInitExpression = null;
        JetExpression initializer = declaration.getInitializer();
        if (initializer != null) {
            jsInitExpression = Translation.translateAsExpression(initializer, context);
        }
        return jsInitExpression;
    }

    @NotNull
    public static JsNameRef getQualifiedReference(@NotNull TranslationContext context,
                                                  @NotNull DeclarationDescriptor descriptor) {
        JsName name = context.getNameForDescriptor(descriptor);
        JsNameRef reference = name.makeRef();
        JsNameRef qualifier = context.getQualifierForDescriptor(descriptor);
        if (qualifier != null) {
            setQualifier(reference, qualifier);
        }
        return reference;
    }

    //TODO: refactor
    @NotNull
    public static JsExpression getThisObject(@NotNull TranslationContext context,
                                             @NotNull DeclarationDescriptor correspondingDeclaration) {
        if (correspondingDeclaration instanceof ClassDescriptor) {
            JsName alias = context.aliasingContext().getAliasForThis(correspondingDeclaration);
            if (alias != null) {
                return alias.makeRef();
            }
        }
        if (correspondingDeclaration instanceof CallableDescriptor) {
            DeclarationDescriptor receiverDescriptor =
                getExpectedReceiverDescriptor((CallableDescriptor)correspondingDeclaration);
            assert receiverDescriptor != null;
            JsName alias = context.aliasingContext().getAliasForThis(receiverDescriptor);
            if (alias != null) {
                return alias.makeRef();
            }
        }
        return new JsThisRef();
    }

    @NotNull
    public static List<JsExpression> translateExpressionList(@NotNull TranslationContext context,
                                                             @NotNull List<JetExpression> expressions) {
        List<JsExpression> result = new ArrayList<JsExpression>();
        for (JetExpression expression : expressions) {
            result.add(Translation.translateAsExpression(expression, context));
        }
        return result;
    }

    @NotNull
    public static JsExpression translateBaseExpression(@NotNull TranslationContext context,
                                                       @NotNull JetUnaryExpression expression) {
        JetExpression baseExpression = PsiUtils.getBaseExpression(expression);
        return Translation.translateAsExpression(baseExpression, context);
    }

    @NotNull
    public static JsExpression translateLeftExpression(@NotNull TranslationContext context,
                                                       @NotNull JetBinaryExpression expression) {
        return Translation.translateAsExpression(expression.getLeft(), context);
    }

    @NotNull
    public static JsExpression translateRightExpression(@NotNull TranslationContext context,
                                                        @NotNull JetBinaryExpression expression) {
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Binary expression should have a right expression";
        return Translation.translateAsExpression(rightExpression, context);
    }

    public static boolean isIntrinsicOperation(@NotNull TranslationContext context,
                                               @NotNull JetOperationExpression expression) {
        FunctionDescriptor operationDescriptor =
            BindingUtils.getFunctionDescriptorForOperationExpression(context.bindingContext(), expression);

        if (operationDescriptor == null) return true;
        if (context.intrinsics().isIntrinsic(operationDescriptor)) return true;

        return false;
    }

    @NotNull
    public static JsNameRef getMethodReferenceForOverloadedOperation(@NotNull TranslationContext context,
                                                                     @NotNull JetOperationExpression expression) {
        FunctionDescriptor overloadedOperationDescriptor = getFunctionDescriptorForOperationExpression
            (context.bindingContext(), expression);
        assert overloadedOperationDescriptor != null;
        JsNameRef overloadedOperationReference = context.getNameForDescriptor(overloadedOperationDescriptor).makeRef();
        assert overloadedOperationReference != null;
        return overloadedOperationReference;
    }

    @NotNull
    public static JsNumberLiteral zeroLiteral(@NotNull TranslationContext context) {
        return context.program().getNumberLiteral(0);
    }

    @NotNull
    public static JsExpression applyIntrinsicToBinaryExpression(@NotNull TranslationContext context,
                                                                @NotNull Intrinsic intrinsic,
                                                                @NotNull JetBinaryExpression binaryExpression) {
        JsExpression left = translateLeftExpression(context, binaryExpression);
        JsExpression right = translateRightExpression(context, binaryExpression);
        return intrinsic.apply(left, Arrays.asList(right), context);
    }

    @NotNull
    public static JsExpression assignmentToBackingField(@NotNull TranslationContext context, @NotNull JetProperty property,
                                                        @NotNull JsExpression initExpression) {
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(context.bindingContext(), property);
        return assignmentToBackingField(context, propertyDescriptor, initExpression);
    }
}
