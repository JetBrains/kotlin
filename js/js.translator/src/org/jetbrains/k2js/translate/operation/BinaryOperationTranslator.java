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

package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.intrinsic.operation.BinaryOperationIntrinsic;
import org.jetbrains.k2js.translate.reference.CallBuilder;
import org.jetbrains.k2js.translate.reference.CallType;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import static org.jetbrains.k2js.translate.operation.AssignmentTranslator.isAssignmentOperator;
import static org.jetbrains.k2js.translate.operation.CompareToTranslator.isCompareToCall;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptorForOperationExpression;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getResolvedCall;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.not;
import static org.jetbrains.k2js.translate.utils.PsiUtils.*;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateLeftExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateRightExpression;


public final class BinaryOperationTranslator extends AbstractTranslator {

    @NotNull
    public static JsExpression translate(@NotNull JetBinaryExpression expression,
            @NotNull TranslationContext context) {
        JsExpression jsExpression = new BinaryOperationTranslator(expression, context).translate();
        return jsExpression.source(expression);
    }

    @NotNull
    /*package*/ static JsExpression translateAsOverloadedCall(@NotNull JetBinaryExpression expression,
            @NotNull TranslationContext context) {
        JsExpression jsExpression = (new BinaryOperationTranslator(expression, context)).translateAsOverloadedBinaryOperation();
        return jsExpression.source(expression);
    }

    @NotNull
    private final JetBinaryExpression expression;

    @Nullable
    private final FunctionDescriptor operationDescriptor;

    private BinaryOperationTranslator(@NotNull JetBinaryExpression expression,
            @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.operationDescriptor =
                getFunctionDescriptorForOperationExpression(bindingContext(), expression);
    }

    @NotNull
    private JsExpression translate() {
        BinaryOperationIntrinsic intrinsic = getIntrinsicForExpression();
        if (intrinsic != null) {
            return applyIntrinsic(intrinsic);
        }
        if (getOperationToken(expression).equals(JetTokens.ELVIS)) {
            return TranslationUtils.notNullConditional(translateLeftExpression(context(), expression),
                                                       translateRightExpression(context(), expression), context());
        }
        if (isAssignmentOperator(expression)) {
            return AssignmentTranslator.translate(expression, context());
        }
        if (isNotOverloadable()) {
            return translateAsUnOverloadableBinaryOperation();
        }
        if (isCompareToCall(expression, context())) {
            return CompareToTranslator.translate(expression, context());
        }
        assert operationDescriptor != null :
                "Overloadable operations must have not null descriptor";
        return translateAsOverloadedBinaryOperation();
    }

    @Nullable
    private BinaryOperationIntrinsic getIntrinsicForExpression() {
        return context().intrinsics().getBinaryOperationIntrinsics().getIntrinsic(expression, context());
    }

    @NotNull
    private JsExpression applyIntrinsic(@NotNull BinaryOperationIntrinsic intrinsic) {
        return intrinsic.apply(expression,
                               translateLeftExpression(context(), expression),
                               translateRightExpression(context(), expression),
                               context());
    }

    private boolean isNotOverloadable() {
        return operationDescriptor == null;
    }

    @NotNull
    private JsExpression translateAsUnOverloadableBinaryOperation() {
        JetToken token = getOperationToken(expression);
        JsBinaryOperator operator = OperatorTable.getBinaryOperator(token);
        assert OperatorConventions.NOT_OVERLOADABLE.contains(token);
        JsExpression left = translateLeftExpression(context(), expression);
        JsExpression right = translateRightExpression(context(), expression);
        return new JsBinaryOperation(operator, left, right);
    }


    @NotNull
    private JsExpression translateAsOverloadedBinaryOperation() {
        CallBuilder callBuilder = setReceiverAndArguments();
        ResolvedCall<?> resolvedCall = getResolvedCall(bindingContext(), expression.getOperationReference());
        JsExpression result = callBuilder.resolvedCall(resolvedCall).type(CallType.NORMAL).translate();
        return mayBeWrapWithNegation(result);
    }

    @NotNull
    private CallBuilder setReceiverAndArguments() {
        CallBuilder callBuilder = CallBuilder.build(context());

        JsExpression leftExpression = translateLeftExpression(context(), expression);
        JsExpression rightExpression = translateRightExpression(context(), expression);

        if (isInOrNotInOperation(expression)) {
            return callBuilder.receiver(rightExpression).args(leftExpression);
        }
        else {
            return callBuilder.receiver(leftExpression).args(rightExpression);
        }
    }

    @NotNull
    private JsExpression mayBeWrapWithNegation(@NotNull JsExpression result) {
        if (isNegatedOperation(expression)) {
            return not(result);
        }
        else {
            return result;
        }
    }
}
