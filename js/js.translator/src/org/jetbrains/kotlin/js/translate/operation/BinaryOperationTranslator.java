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

package org.jetbrains.kotlin.js.translate.operation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.TopLevelFIF;
import org.jetbrains.kotlin.js.translate.intrinsic.operation.BinaryOperationIntrinsic;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtPsiUtil;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import java.util.Collections;

import static org.jetbrains.kotlin.js.translate.operation.AssignmentTranslator.isAssignmentOperator;
import static org.jetbrains.kotlin.js.translate.operation.CompareToTranslator.isCompareToCall;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.not;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.isNegatedOperation;

public final class BinaryOperationTranslator extends AbstractTranslator {

    @NotNull
    public static JsExpression translate(@NotNull KtBinaryExpression expression,
            @NotNull TranslationContext context) {
        JsExpression jsExpression = new BinaryOperationTranslator(expression, context).translate();
        return jsExpression.source(expression);
    }

    @NotNull
    /*package*/ static JsExpression translateAsOverloadedCall(@NotNull KtBinaryExpression expression,
            @NotNull TranslationContext context) {
        JsExpression jsExpression = (new BinaryOperationTranslator(expression, context)).translateAsOverloadedBinaryOperation();
        return jsExpression.source(expression);
    }

    @NotNull
    private final KtBinaryExpression expression;

    @NotNull
    private final KtExpression leftKtExpression;

    @NotNull
    private final KtExpression rightKtExpression;

    @NotNull
    private final KtToken operationToken;

    @Nullable
    private final CallableDescriptor operationDescriptor;

    private BinaryOperationTranslator(@NotNull KtBinaryExpression expression,
            @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;

        assert expression.getLeft() != null : "Binary expression should have a left expression: " + expression.getText();
        leftKtExpression = expression.getLeft();

        assert expression.getRight() != null : "Binary expression should have a right expression: " + expression.getText();
        rightKtExpression = expression.getRight();

        this.operationToken = getOperationToken(expression);
        this.operationDescriptor = getCallableDescriptorForOperationExpression(bindingContext(), expression);
    }

    @NotNull
    private JsExpression translate() {
        BinaryOperationIntrinsic intrinsic = getIntrinsicForExpression();
        if (intrinsic.exists()) {
            return applyIntrinsic(intrinsic);
        }
        if (operationToken == KtTokens.ELVIS) {
            return translateElvis();
        }
        if (isAssignmentOperator(operationToken)) {
            return AssignmentTranslator.translate(expression, context());
        }
        if (isNotOverloadable()) {
            return translateAsUnOverloadableBinaryOperation();
        }
        if (isCompareToCall(operationToken, operationDescriptor)) {
            return CompareToTranslator.translate(expression, context());
        }
        if (isEquals()) {
            return translateEquals();
        }
        assert operationDescriptor != null :
                "Overloadable operations must have not null descriptor";
        return translateAsOverloadedBinaryOperation();
    }

    @NotNull
    private JsExpression translateElvis() {
        KotlinType expressionType = context().bindingContext().getType(expression);
        assert expressionType != null;

        JsExpression leftExpression = TranslationUtils.coerce(
                context(), Translation.translateAsExpression(leftKtExpression, context()), expressionType);

        JsBlock rightBlock = new JsBlock();
        JsExpression rightExpression = TranslationUtils.coerce(
                context(), Translation.translateAsExpression(rightKtExpression, context(), rightBlock), expressionType);

        if (rightBlock.isEmpty()) {
            return TranslationUtils.notNullConditional(leftExpression, rightExpression, context());
        }

        JsExpression result;
        JsIf ifStatement;
        if (BindingContextUtilsKt.isUsedAsExpression(expression, context().bindingContext())) {
            result = context().cacheExpressionIfNeeded(leftExpression);
            JsExpression testExpression = TranslationUtils.isNullCheck(result);
            rightBlock.getStatements().add(JsAstUtils.assignment(result, rightExpression).makeStmt());
            ifStatement = JsAstUtils.newJsIf(testExpression, rightBlock);
        }
        else {
            result = new JsNullLiteral();
            JsExpression testExpression = TranslationUtils.isNullCheck(leftExpression);
            ifStatement = JsAstUtils.newJsIf(testExpression, rightBlock);
        }
        ifStatement.setSource(expression);
        context().addStatementToCurrentBlock(ifStatement);
        return result;
    }

    @NotNull
    private BinaryOperationIntrinsic getIntrinsicForExpression() {
        return context().intrinsics().getBinaryOperationIntrinsic(expression, context());
    }

    @NotNull
    private JsExpression applyIntrinsic(@NotNull BinaryOperationIntrinsic intrinsic) {
        JsExpression leftExpression = Translation.translateAsExpression(leftKtExpression, context());

        JsBlock rightBlock = new JsBlock();
        JsExpression rightExpression = Translation.translateAsExpression(rightKtExpression, context(), rightBlock);

        if (rightBlock.isEmpty()) {
            return intrinsic.apply(expression, leftExpression, rightExpression, context());
        }

        leftExpression = context().cacheExpressionIfNeeded(leftExpression);
        context().addStatementsToCurrentBlockFrom(rightBlock);

        return intrinsic.apply(expression, leftExpression, rightExpression, context());
    }

    private boolean isNotOverloadable() {
        return OperatorConventions.NOT_OVERLOADABLE.contains(operationToken);
    }

    @NotNull
    private JsExpression translateAsUnOverloadableBinaryOperation() {
        assert OperatorConventions.NOT_OVERLOADABLE.contains(operationToken);
        JsBinaryOperator operator = OperatorTable.getBinaryOperator(operationToken);
        JsExpression leftExpression = Translation.translateAsExpression(leftKtExpression, context());

        JsBlock rightBlock = new JsBlock();
        JsExpression rightExpression = Translation.translateAsExpression(rightKtExpression, context(), rightBlock);

        if (rightBlock.isEmpty()) {
            return new JsBinaryOperation(operator, leftExpression, rightExpression);
        }
        else if (OperatorConventions.IDENTITY_EQUALS_OPERATIONS.contains(operationToken)) {
            context().addStatementsToCurrentBlockFrom(rightBlock);
            return new JsBinaryOperation(operator, leftExpression, rightExpression);
        }

        assert operationToken.equals(KtTokens.ANDAND) || operationToken.equals(KtTokens.OROR) : "Unsupported binary operation: " + expression.getText();
        boolean isOror = operationToken.equals(KtTokens.OROR);
        JsExpression literalResult = new JsBooleanLiteral(isOror).source(rightKtExpression);
        leftExpression = isOror ? not(leftExpression) : leftExpression;

        JsIf ifStatement;
        JsExpression result;
        if (BindingContextUtilsKt.isUsedAsExpression(expression, context().bindingContext())) {
            if (rightExpression instanceof JsNameRef) {
                result = rightExpression; // Reuse tmp variable
            } else {
                result = context().declareTemporary(null, rightKtExpression).reference();
                JsExpression rightAssignment = JsAstUtils.assignment(result.deepCopy(), rightExpression).source(rightKtExpression);
                rightBlock.getStatements().add(JsAstUtils.asSyntheticStatement(rightAssignment));
            }
            JsStatement assignmentStatement = JsAstUtils.asSyntheticStatement(
                    JsAstUtils.assignment(result.deepCopy(), literalResult).source(rightKtExpression));
            ifStatement = JsAstUtils.newJsIf(leftExpression, rightBlock, assignmentStatement);
            MetadataProperties.setSynthetic(ifStatement, true);
        }
        else {
            ifStatement = JsAstUtils.newJsIf(leftExpression, rightBlock);
            result = new JsNullLiteral();
        }
        ifStatement.source(expression);
        context().addStatementToCurrentBlock(ifStatement);
        return result;
    }

    private boolean isEquals() {
        return operationToken == KtTokens.EQEQ || operationToken == KtTokens.EXCLEQ;
    }

    private JsExpression translateEquals() {
        JsExpression left = Translation.translateAsExpression(leftKtExpression, context());
        JsExpression right = Translation.translateAsExpression(rightKtExpression, context());

        if (left instanceof JsNullLiteral || right instanceof JsNullLiteral) {
            JsBinaryOperator operator = operationToken == KtTokens.EXCLEQ ? JsBinaryOperator.NEQ : JsBinaryOperator.EQ;
            return new JsBinaryOperation(operator, left, right);
        }

        KotlinType leftType = context().bindingContext().getType(leftKtExpression);
        KotlinType rightType = context().bindingContext().getType(rightKtExpression);

        if (leftType != null && TypeUtils.isNullableType(leftType) || rightType != null && TypeUtils.isNullableType(rightType)) {
            return mayBeWrapWithNegation(TopLevelFIF.KOTLIN_EQUALS.apply(left, Collections.singletonList(right), context()));
        }

        return translateAsOverloadedBinaryOperation();
    }

    @NotNull
    private JsExpression translateAsOverloadedBinaryOperation() {
        ResolvedCall<? extends FunctionDescriptor> resolvedCall = CallUtilKt.getFunctionResolvedCallWithAssert(expression, bindingContext());
        JsExpression result = CallTranslator.translate(context(), resolvedCall, getReceiver());
        return mayBeWrapWithNegation(result);
    }

    @NotNull
    private JsExpression getReceiver() {
        if (KtPsiUtil.isInOrNotInOperation(expression)) {
            return Translation.translateAsExpression(rightKtExpression, context());
        } else {
            return Translation.translateAsExpression(leftKtExpression, context());
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
