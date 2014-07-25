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

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.callTranslator.CallTranslator;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.intrinsic.operation.BinaryOperationIntrinsic;
import org.jetbrains.k2js.translate.utils.TranslationUtils;
import org.jetbrains.k2js.translate.utils.mutator.AssignToExpressionMutator;
import org.jetbrains.k2js.translate.utils.mutator.LastExpressionMutator;

import static org.jetbrains.jet.lang.resolve.bindingContextUtil.BindingContextUtilPackage.getFunctionResolvedCallWithAssert;
import static org.jetbrains.k2js.translate.operation.AssignmentTranslator.isAssignmentOperator;
import static org.jetbrains.k2js.translate.operation.CompareToTranslator.isCompareToCall;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.convertToStatement;
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
    private final CallableDescriptor operationDescriptor;

    private BinaryOperationTranslator(@NotNull JetBinaryExpression expression,
            @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.operationDescriptor = getCallableDescriptorForOperationExpression(bindingContext(), expression);
    }

    @NotNull
    private JsExpression translate() {
        BinaryOperationIntrinsic intrinsic = getIntrinsicForExpression();
        if (intrinsic != null) {
            return applyIntrinsic(intrinsic);
        }
        if (getOperationToken(expression).equals(JetTokens.ELVIS)) {
            return translateElvis(expression);
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

    @NotNull
    private JsExpression translateElvis(JetBinaryExpression expression) {
        JsExpression leftExpression = translateLeftExpression(context(), expression);

        JetExpression rightJetExpression = expression.getRight();
        assert rightJetExpression != null : "Binary expression should have a right expression";
        JsNode rightNode = Translation.translateExpression(rightJetExpression, context());

        if (rightNode instanceof JsExpression) {
            return TranslationUtils.notNullConditional(leftExpression, (JsExpression) rightNode, context());
        }

        TemporaryVariable result = context().declareTemporary(null);
        AssignToExpressionMutator saveResultToTemporaryMutator = new AssignToExpressionMutator(result.reference());
        context().addStatementToCurrentBlock(LastExpressionMutator.mutateLastExpression(leftExpression, saveResultToTemporaryMutator));

        JsExpression testExpression = TranslationUtils.isNullCheck(result.reference());
        JsStatement thenStatement = convertToStatement(rightNode);
        JsIf ifStatement = new JsIf(testExpression, thenStatement);
        context().addStatementToCurrentBlock(ifStatement);

        return result.reference();
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
        ResolvedCall<? extends FunctionDescriptor> resolvedCall = getFunctionResolvedCallWithAssert(expression, bindingContext());
        JsExpression result = CallTranslator.INSTANCE$.translate(context(), resolvedCall, getReceiver());
        return mayBeWrapWithNegation(result);
    }

    @NotNull
    private JsExpression getReceiver() {
        if (isInOrNotInOperation(expression)) {
            return translateRightExpression(context(), expression);
        } else {
            return translateLeftExpression(context(), expression);
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
