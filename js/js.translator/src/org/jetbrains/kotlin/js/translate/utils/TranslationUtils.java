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

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.js.translate.context.TemporaryConstVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.types.JetType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.dart.compiler.backend.js.ast.JsBinaryOperator.*;
import static org.jetbrains.kotlin.js.translate.context.Namer.getKotlinBackingFieldName;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.assignment;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.createDataDescriptor;
import static org.jetbrains.kotlin.js.translate.utils.ManglingUtils.getMangledName;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isAnonymousObject;

public final class TranslationUtils {

    private TranslationUtils() {
    }

    @NotNull
    public static JsPropertyInitializer translateFunctionAsEcma5PropertyDescriptor(@NotNull JsFunction function,
            @NotNull FunctionDescriptor descriptor,
            @NotNull TranslationContext context) {
        if (DescriptorUtils.isExtension(descriptor)) {
            return translateExtensionFunctionAsEcma5DataDescriptor(function, descriptor, context);
        }
        else {
            JsStringLiteral getOrSet = context.program().getStringLiteral(descriptor instanceof PropertyGetterDescriptor ? "get" : "set");
            return new JsPropertyInitializer(getOrSet, function);
        }
    }

    @NotNull
    public static JsFunction simpleReturnFunction(@NotNull JsScope functionScope, @NotNull JsExpression returnExpression) {
        return new JsFunction(functionScope, new JsBlock(new JsReturn(returnExpression)), "<simpleReturnFunction>");
    }

    @NotNull
    private static JsPropertyInitializer translateExtensionFunctionAsEcma5DataDescriptor(@NotNull JsFunction function,
            @NotNull FunctionDescriptor descriptor, @NotNull TranslationContext context) {
        JsObjectLiteral meta = createDataDescriptor(function, descriptor.getModality().isOverridable(), false);
        return new JsPropertyInitializer(context.getNameForDescriptor(descriptor).makeRef(), meta);
    }

    @NotNull
    public static JsExpression translateExclForBinaryEqualLikeExpr(@NotNull JsBinaryOperation baseBinaryExpression) {
        return new JsBinaryOperation(notOperator(baseBinaryExpression.getOperator()), baseBinaryExpression.getArg1(), baseBinaryExpression.getArg2());
    }

    public static boolean isEqualLikeOperator(@NotNull JsBinaryOperator operator) {
        return notOperator(operator) != null;
    }

    @Nullable
    private static JsBinaryOperator notOperator(@NotNull JsBinaryOperator operator) {
        switch (operator) {
            case REF_EQ:
                return REF_NEQ;
            case REF_NEQ:
                return REF_EQ;
            case EQ:
                return NEQ;
            case NEQ:
                return EQ;
            default:
                return null;
        }
    }

    @NotNull
    public static JsBinaryOperation isNullCheck(@NotNull JsExpression expressionToCheck) {
        return nullCheck(expressionToCheck, false);
    }

    @NotNull
    public static JsBinaryOperation isNotNullCheck(@NotNull JsExpression expressionToCheck) {
        return nullCheck(expressionToCheck, true);
    }

    @NotNull
    public static JsBinaryOperation nullCheck(@NotNull JsExpression expressionToCheck, boolean isNegated) {
        JsBinaryOperator operator = isNegated ? JsBinaryOperator.NEQ : JsBinaryOperator.EQ;
        return new JsBinaryOperation(operator, expressionToCheck, JsLiteral.NULL);
    }

    @NotNull
    public static JsConditional notNullConditional(
            @NotNull JsExpression expression,
            @NotNull JsExpression elseExpression,
            @NotNull TranslationContext context
    ) {
        JsExpression testExpression;
        JsExpression thenExpression;
        if (isCacheNeeded(expression)) {
            TemporaryConstVariable tempVar = context.getOrDeclareTemporaryConstVariable(expression);
            testExpression = isNotNullCheck(tempVar.value());
            thenExpression = tempVar.value();
        }
        else {
            testExpression = isNotNullCheck(expression);
            thenExpression = expression;
        }

        return new JsConditional(testExpression, thenExpression, elseExpression);
    }

    @NotNull
    public static JsNameRef backingFieldReference(@NotNull TranslationContext context,
            @NotNull PropertyDescriptor descriptor) {
        JsName backingFieldName = context.getNameForDescriptor(descriptor);
        if(!JsDescriptorUtils.isSimpleFinalProperty(descriptor)) {
            String backingFieldMangledName;
            if (!Visibilities.isPrivate(descriptor.getVisibility())) {
                backingFieldMangledName = getMangledName(descriptor, getKotlinBackingFieldName(backingFieldName.getIdent()));
            } else {
                backingFieldMangledName = getKotlinBackingFieldName(backingFieldName.getIdent());
            }
            backingFieldName = context.declarePropertyOrPropertyAccessorName(descriptor, backingFieldMangledName, false);
        }
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
    public static JsExpression translateBaseExpression(@NotNull TranslationContext context,
            @NotNull JetUnaryExpression expression) {
        JetExpression baseExpression = PsiUtils.getBaseExpression(expression);
        return Translation.translateAsExpression(baseExpression, context);
    }

    @NotNull
    public static JsExpression translateLeftExpression(@NotNull TranslationContext context,
            @NotNull JetBinaryExpression expression) {
        return translateLeftExpression(context, expression, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static JsExpression translateLeftExpression(
            @NotNull TranslationContext context,
            @NotNull JetBinaryExpression expression,
            @NotNull JsBlock block
    ) {
        JetExpression left = expression.getLeft();
        assert left != null : "Binary expression should have a left expression: " + expression.getText();
        return Translation.translateAsExpression(left, context, block);
    }

    @NotNull
    public static JsExpression translateRightExpression(@NotNull TranslationContext context,
            @NotNull JetBinaryExpression expression) {
        return translateRightExpression(context, expression, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static JsExpression translateRightExpression(
            @NotNull TranslationContext context,
            @NotNull JetBinaryExpression expression,
            @NotNull JsBlock block) {
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Binary expression should have a right expression";
        return Translation.translateAsExpression(rightExpression, context, block);
    }

    public static boolean hasCorrespondingFunctionIntrinsic(@NotNull TranslationContext context,
            @NotNull JetOperationExpression expression) {
        CallableDescriptor operationDescriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression);

        if (operationDescriptor == null || !(operationDescriptor instanceof FunctionDescriptor)) return true;

        JetType returnType = operationDescriptor.getReturnType();
        if (returnType != null &&
            KotlinBuiltIns.getInstance().getCharType().equals(returnType) || (KotlinBuiltIns.getInstance().getLongType().equals(returnType))) return false;

        if (context.intrinsics().getFunctionIntrinsic((FunctionDescriptor) operationDescriptor).exists()) return true;

        return false;
    }

    @NotNull
    public static List<JsExpression> generateInvocationArguments(@NotNull JsExpression receiver, @NotNull List<JsExpression> arguments) {
        if (arguments.isEmpty()) {
            return Collections.singletonList(receiver);
        }

        List<JsExpression> argumentList = new ArrayList<JsExpression>(1 + arguments.size());
        argumentList.add(receiver);
        argumentList.addAll(arguments);
        return argumentList;
    }

    public static boolean isCacheNeeded(@NotNull JsExpression expression) {
        return !(expression instanceof JsLiteral.JsValueLiteral) &&
               !JsAstUtils.isEmptyExpression(expression) &&
               (!(expression instanceof JsNameRef) || ((JsNameRef) expression).getQualifier() != null);
    }

    @NotNull
    public static JsConditional sure(@NotNull JsExpression expression, @NotNull TranslationContext context) {
        JsInvocation throwNPE = new JsInvocation(context.namer().throwNPEFunctionRef());
        JsConditional ensureNotNull = notNullConditional(expression, throwNPE, context);

        JsExpression thenExpression = ensureNotNull.getThenExpression();
        if (thenExpression instanceof JsNameRef) {
            // associate (cache) ensureNotNull expression to new TemporaryConstVariable with same name.
            context.associateExpressionToLazyValue(ensureNotNull,
                                                   new TemporaryConstVariable(((JsNameRef) thenExpression).getName(), ensureNotNull));
        }

        return ensureNotNull;
    }

    @NotNull
    public static String getSuggestedNameForInnerDeclaration(TranslationContext context, DeclarationDescriptor descriptor) {
        String suggestedName = "";
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        //noinspection ConstantConditions
        if (containingDeclaration != null &&
            !(containingDeclaration instanceof ClassOrPackageFragmentDescriptor) &&
            !(containingDeclaration instanceof AnonymousFunctionDescriptor) &&
            !(containingDeclaration instanceof ConstructorDescriptor && isAnonymousObject(containingDeclaration.getContainingDeclaration()))) {
            suggestedName = context.getNameForDescriptor(containingDeclaration).getIdent();
        }

        if (!suggestedName.isEmpty() && !suggestedName.endsWith("$")) {
            suggestedName += "$";
        }

        if (descriptor.getName().isSpecial()) {
            suggestedName += "f";
        }
        else {
            suggestedName += context.getNameForDescriptor(descriptor).getIdent();
        }
        return suggestedName;
    }
}
