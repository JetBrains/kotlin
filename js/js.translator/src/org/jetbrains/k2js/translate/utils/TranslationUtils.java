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

package org.jetbrains.k2js.translate.utils;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyGetterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptorForOperationExpression;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;

public final class TranslationUtils {
    private TranslationUtils() {
    }

    @NotNull
    public static JsPropertyInitializer translateFunctionAsEcma5PropertyDescriptor(@NotNull JsFunction function,
            @NotNull FunctionDescriptor descriptor,
            @NotNull TranslationContext context) {
        if (JsDescriptorUtils.isExtension(descriptor)) {
            return translateExtensionFunctionAsEcma5DataDescriptor(function, descriptor, context);
        }
        else {
            JsStringLiteral getOrSet = context.program().getStringLiteral(descriptor instanceof PropertyGetterDescriptor ? "get" : "set");
            return new JsPropertyInitializer(getOrSet, function);
        }
    }

    @NotNull
    private static JsPropertyInitializer translateExtensionFunctionAsEcma5DataDescriptor(@NotNull JsFunction function,
            @NotNull FunctionDescriptor descriptor, @NotNull TranslationContext context) {
        JsObjectLiteral meta = JsAstUtils.createDataDescriptor(function, descriptor.getModality().isOverridable());
        return new JsPropertyInitializer(context.getNameForDescriptor(descriptor).makeRef(), meta);
    }

    @NotNull
    public static JsBinaryOperation isNullCheck(@NotNull JsExpression expressionToCheck) {
        return nullCheck(expressionToCheck, false);
    }

    @NotNull
    public static JsBinaryOperation notNullConditionalTestExpression(@NotNull TemporaryVariable cachedValue) {
        return and(inequality(cachedValue.assignmentExpression(), JsLiteral.NULL),
                   inequality(cachedValue.reference(), JsLiteral.UNDEFINED));
    }

    @NotNull
    public static JsBinaryOperation nullCheck(@NotNull JsExpression expressionToCheck, boolean isNegated) {
        JsBinaryOperator operator = isNegated ? JsBinaryOperator.REF_NEQ : JsBinaryOperator.REF_EQ;
        return new JsBinaryOperation(isNegated ? JsBinaryOperator.AND : JsBinaryOperator.OR,
                                     new JsBinaryOperation(operator, expressionToCheck, JsLiteral.NULL),
                                     new JsBinaryOperation(operator, expressionToCheck, JsLiteral.UNDEFINED));
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
        return new JsNameRef(backingFieldName, JsLiteral.THIS);
    }

    @NotNull
    public static JsExpression assignmentToBackingField(@NotNull TranslationContext context,
            @NotNull PropertyDescriptor descriptor,
            @NotNull JsExpression assignTo) {
        JsNameRef backingFieldReference = backingFieldReference(context, descriptor);
        return assignment(backingFieldReference, assignTo);
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
        JetExpression left = expression.getLeft();
        assert left != null : "Binary expression should have a left expression: " + expression.getText();
        return Translation.translateAsExpression(left, context);
    }

    @NotNull
    public static JsExpression translateRightExpression(@NotNull TranslationContext context,
            @NotNull JetBinaryExpression expression) {
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Binary expression should have a right expression";
        return Translation.translateAsExpression(rightExpression, context);
    }

    public static boolean hasCorrespondingFunctionIntrinsic(@NotNull TranslationContext context,
            @NotNull JetOperationExpression expression) {
        FunctionDescriptor operationDescriptor = getFunctionDescriptorForOperationExpression(context.bindingContext(), expression);

        if (operationDescriptor == null) return true;
        if (context.intrinsics().getFunctionIntrinsics().getIntrinsic(operationDescriptor).exists()) return true;

        return false;
    }
}
