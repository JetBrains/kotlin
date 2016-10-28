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
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.metadata.MetadataProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableAccessorDescriptor;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TemporaryConstVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.dart.compiler.backend.js.ast.JsBinaryOperator.*;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.assignment;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.createDataDescriptor;

public final class TranslationUtils {

    private TranslationUtils() {
    }

    @NotNull
    public static JsPropertyInitializer translateFunctionAsEcma5PropertyDescriptor(@NotNull JsFunction function,
            @NotNull FunctionDescriptor descriptor,
            @NotNull TranslationContext context) {
        if (DescriptorUtils.isExtension(descriptor) ||
            descriptor instanceof PropertyAccessorDescriptor &&
            shouldAccessViaFunctions(((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty())
        ) {
            return translateExtensionFunctionAsEcma5DataDescriptor(function, descriptor, context);
        }
        else {
            JsStringLiteral getOrSet = context.program().getStringLiteral(getAccessorFunctionName(descriptor));
            return new JsPropertyInitializer(getOrSet, function);
        }
    }

    @NotNull
    public static String getAccessorFunctionName(@NotNull FunctionDescriptor descriptor) {
        boolean isGetter = descriptor instanceof PropertyGetterDescriptor || descriptor instanceof LocalVariableAccessorDescriptor.Getter;
        return isGetter ? "get" : "set";
    }

    @NotNull
    public static JsFunction simpleReturnFunction(@NotNull JsScope functionScope, @NotNull JsExpression returnExpression) {
        return new JsFunction(functionScope, new JsBlock(new JsReturn(returnExpression)), "<simpleReturnFunction>");
    }

    @NotNull
    private static JsPropertyInitializer translateExtensionFunctionAsEcma5DataDescriptor(@NotNull JsFunction function,
            @NotNull FunctionDescriptor descriptor, @NotNull TranslationContext context) {
        JsObjectLiteral meta = createDataDescriptor(function, ModalityKt.isOverridable(descriptor), false);
        return new JsPropertyInitializer(context.getNameForDescriptor(descriptor).makeRef(), meta);
    }

    @NotNull
    public static JsExpression translateExclForBinaryEqualLikeExpr(@NotNull JsBinaryOperation baseBinaryExpression) {
        JsBinaryOperator negatedOperator = notOperator(baseBinaryExpression.getOperator());
        assert negatedOperator != null : "Can't negate operator: " + baseBinaryExpression.getOperator();
        return new JsBinaryOperation(negatedOperator, baseBinaryExpression.getArg1(), baseBinaryExpression.getArg2());
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
    public static JsNameRef backingFieldReference(@NotNull TranslationContext context, @NotNull PropertyDescriptor descriptor) {
        DeclarationDescriptor containingDescriptor = descriptor.getContainingDeclaration();
        JsName backingFieldName = containingDescriptor instanceof PackageFragmentDescriptor ?
                                  context.getInnerNameForDescriptor(descriptor) :
                                  context.getNameForDescriptor(descriptor);

        if (!JsDescriptorUtils.isSimpleFinalProperty(descriptor) && !(containingDescriptor instanceof PackageFragmentDescriptor)) {
            backingFieldName = context.getNameForBackingField(descriptor);
        }

        JsExpression receiver;
        if (containingDescriptor instanceof PackageFragmentDescriptor) {
            receiver = null;
        }
        else {
            receiver = context.getDispatchReceiver(JsDescriptorUtils.getReceiverParameterForDeclaration(containingDescriptor));
        }
        return new JsNameRef(backingFieldName, receiver);
    }

    @NotNull
    public static JsExpression assignmentToBackingField(@NotNull TranslationContext context,
            @NotNull PropertyDescriptor descriptor,
            @NotNull JsExpression assignTo) {
        JsNameRef backingFieldReference = backingFieldReference(context, descriptor);
        return assignment(backingFieldReference, assignTo);
    }

    @Nullable
    public static JsExpression translateInitializerForProperty(@NotNull KtProperty declaration,
            @NotNull TranslationContext context) {
        JsExpression jsInitExpression = null;
        KtExpression initializer = declaration.getInitializer();
        if (initializer != null) {
            jsInitExpression = Translation.translateAsExpression(initializer, context);
        }
        return jsInitExpression;
    }

    @NotNull
    public static JsExpression translateBaseExpression(@NotNull TranslationContext context,
            @NotNull KtUnaryExpression expression) {
        KtExpression baseExpression = PsiUtils.getBaseExpression(expression);
        return Translation.translateAsExpression(baseExpression, context);
    }

    @NotNull
    public static JsExpression translateLeftExpression(
            @NotNull TranslationContext context,
            @NotNull KtBinaryExpression expression,
            @NotNull JsBlock block
    ) {
        KtExpression left = expression.getLeft();
        assert left != null : "Binary expression should have a left expression: " + expression.getText();
        return Translation.translateAsExpression(left, context, block);
    }

    @NotNull
    public static JsExpression translateRightExpression(@NotNull TranslationContext context,
            @NotNull KtBinaryExpression expression) {
        return translateRightExpression(context, expression, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static JsExpression translateRightExpression(
            @NotNull TranslationContext context,
            @NotNull KtBinaryExpression expression,
            @NotNull JsBlock block) {
        KtExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Binary expression should have a right expression";
        return Translation.translateAsExpression(rightExpression, context, block);
    }

    public static boolean hasCorrespondingFunctionIntrinsic(@NotNull TranslationContext context,
            @NotNull KtOperationExpression expression) {
        CallableDescriptor operationDescriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression);

        if (operationDescriptor == null || !(operationDescriptor instanceof FunctionDescriptor)) return true;

        KotlinType returnType = operationDescriptor.getReturnType();
        if (returnType != null && (KotlinBuiltIns.isChar(returnType) || KotlinBuiltIns.isLong(returnType))) return false;

        if (context.intrinsics().getFunctionIntrinsic((FunctionDescriptor) operationDescriptor).exists()) return true;

        return false;
    }

    @NotNull
    public static List<JsExpression> generateInvocationArguments(@NotNull JsExpression receiver, @NotNull List<JsExpression> arguments) {
        List<JsExpression> argumentList = new ArrayList<JsExpression>(1 + arguments.size());
        argumentList.add(receiver);
        argumentList.addAll(arguments);
        return argumentList;
    }

    public static boolean isCacheNeeded(@NotNull JsExpression expression) {
        return !(expression instanceof JsLiteral.JsValueLiteral) &&
               (!(expression instanceof JsNameRef) || ((JsNameRef) expression).getQualifier() != null);
    }

    @NotNull
    public static JsConditional sure(@NotNull JsExpression expression, @NotNull TranslationContext context) {
        JsInvocation throwNPE = new JsInvocation(Namer.throwNPEFunctionRef());
        JsConditional ensureNotNull = notNullConditional(expression, throwNPE, context);

        JsExpression thenExpression = ensureNotNull.getThenExpression();
        if (thenExpression instanceof JsNameRef) {
            JsName name = ((JsNameRef) thenExpression).getName();
            if (name != null) {
                // associate(cache) ensureNotNull expression to new TemporaryConstVariable with same name.
                context.associateExpressionToLazyValue(ensureNotNull, new TemporaryConstVariable(name, ensureNotNull));
            }
        }

        return ensureNotNull;
    }

    public static boolean isSimpleNameExpressionNotDelegatedLocalVar(@Nullable KtExpression expression, @NotNull TranslationContext context) {
        if (!(expression instanceof KtSimpleNameExpression)) {
            return false;
        }
        DeclarationDescriptor descriptor = context.bindingContext().get(BindingContext.REFERENCE_TARGET, ((KtSimpleNameExpression) expression));
        return !(descriptor instanceof LocalVariableDescriptor) || !((LocalVariableDescriptor) descriptor).isDelegated();
    }

    public static boolean shouldAccessViaFunctions(@NotNull CallableDescriptor descriptor) {
        if (descriptor instanceof PropertyDescriptor) {
            return shouldAccessViaFunctions((PropertyDescriptor) descriptor);
        }
        else if (descriptor instanceof PropertyAccessorDescriptor) {
            return shouldAccessViaFunctions(((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty());
        }
        else {
            return false;
        }
    }

    private static boolean shouldAccessViaFunctions(@NotNull PropertyDescriptor property) {
        if (AnnotationsUtils.hasJsNameInAccessors(property)) return true;
        for (PropertyDescriptor overriddenProperty : property.getOverriddenDescriptors()) {
            if (shouldAccessViaFunctions(overriddenProperty)) return true;
        }
        return false;
    }

    @Nullable
    public static JsExpression tryTranslateHandleResult(
            @NotNull TranslationContext context,
            @NotNull KtExpression expression,
            @Nullable KtExpression returnExpression
    ) {
        ResolvedCall<? extends FunctionDescriptor> returnCall = context.bindingContext().get(
                BindingContext.RETURN_HANDLE_RESULT_RESOLVED_CALL, expression);
        if (returnCall == null) return null;

        Map<KtExpression, JsExpression> aliases = new HashMap<KtExpression, JsExpression>();
        List<ResolvedValueArgument> arguments = returnCall.getValueArgumentsByIndex();
        assert arguments != null : "Arguments should be defined here: " + PsiUtilsKt.getTextWithLocation(expression);

        KotlinType returnType = returnCall.getResultingDescriptor().getValueParameters().get(0).getType();

        ValueArgument returnValueArgument = arguments.get(0).getArguments().get(0);
        if (returnExpression != null) {
            aliases.put(returnValueArgument.getArgumentExpression(), Translation.translateAsExpression(returnExpression, context));
        }
        else if (KotlinBuiltIns.isUnit(returnType)) {
            aliases.put(returnValueArgument.getArgumentExpression(), JsLiteral.NULL);
        }

        ValueArgument continuationArgument = arguments.get(1).getArguments().get(0);
        aliases.put(continuationArgument.getArgumentExpression(), JsLiteral.THIS);

        TranslationContext returnContext = context.innerContextWithAliasesForExpressions(aliases);
        JsInvocation handleResultInvocation = (JsInvocation) CallTranslator.translate(returnContext, returnCall, JsLiteral.THIS);
        MetadataProperties.setHandleResult(handleResultInvocation, true);

        return handleResultInvocation;
    }
}
