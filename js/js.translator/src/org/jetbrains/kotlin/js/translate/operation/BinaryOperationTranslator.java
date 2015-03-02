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

package org.jetbrains.kotlin.js.translate.operation;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.intrinsic.operation.BinaryOperationIntrinsic;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.lexer.JetToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.JetBinaryExpression;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import static org.jetbrains.kotlin.js.translate.operation.AssignmentTranslator.isAssignmentOperator;
import static org.jetbrains.kotlin.js.translate.operation.CompareToTranslator.isCompareToCall;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.not;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.*;
import static org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage.getFunctionResolvedCallWithAssert;

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

    @NotNull
    private final JetExpression leftJetExpression;

    @NotNull
    private final JetExpression rightJetExpression;

    @NotNull
    private final JetToken operationToken;

    @Nullable
    private final CallableDescriptor operationDescriptor;

    private BinaryOperationTranslator(@NotNull JetBinaryExpression expression,
            @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;

        assert expression.getLeft() != null : "Binary expression should have a left expression: " + expression.getText();
        leftJetExpression = expression.getLeft();

        assert expression.getRight() != null : "Binary expression should have a right expression: " + expression.getText();
        rightJetExpression = expression.getRight();

        this.operationToken = getOperationToken(expression);
        this.operationDescriptor = getCallableDescriptorForOperationExpression(bindingContext(), expression);
    }

    @NotNull
    private JsExpression translate() {
        BinaryOperationIntrinsic intrinsic = getIntrinsicForExpression();
        if (intrinsic.exists()) {
            return applyIntrinsic(intrinsic);
        }
        if (operationToken == JetTokens.ELVIS) {
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
        assert operationDescriptor != null :
                "Overloadable operations must have not null descriptor";
        return translateAsOverloadedBinaryOperation();
    }

    @NotNull
    private JsExpression translateElvis() {
        JsExpression leftExpression = Translation.translateAsExpression(leftJetExpression, context());
        if (JsAstUtils.isEmptyExpression(leftExpression)) {
            return leftExpression;
        }

        JsBlock rightBlock = new JsBlock();
        JsExpression rightExpression = Translation.translateAsExpression(rightJetExpression, context(), rightBlock);

        if (rightBlock.isEmpty()) {
            return TranslationUtils.notNullConditional(leftExpression, rightExpression, context());
        }

        JsExpression result;
        JsIf ifStatement;
        if (BindingContextUtilPackage.isUsedAsExpression(expression, context().bindingContext())) {
            if (TranslationUtils.isCacheNeeded(leftExpression)) {
                TemporaryVariable resultVar = context().declareTemporary(leftExpression);
                result = resultVar.reference();
                context().addStatementToCurrentBlock(resultVar.assignmentExpression().makeStmt());
            }
            else {
                result = leftExpression;
            }
            JsExpression testExpression = TranslationUtils.isNullCheck(result);
            if (!JsAstUtils.isEmptyExpression(rightExpression)) {
                rightBlock.getStatements().add(JsAstUtils.assignment(result, rightExpression).makeStmt());
            }
            ifStatement = JsAstUtils.newJsIf(testExpression, rightBlock);
        }
        else {
            result = context().getEmptyExpression();
            JsExpression testExpression = TranslationUtils.isNullCheck(leftExpression);
            ifStatement = JsAstUtils.newJsIf(testExpression, rightBlock);
        }
        context().addStatementToCurrentBlock(ifStatement);
        return result;
    }

    @NotNull
    private BinaryOperationIntrinsic getIntrinsicForExpression() {
        return context().intrinsics().getBinaryOperationIntrinsic(expression, context());
    }

    @NotNull
    private JsExpression applyIntrinsic(@NotNull BinaryOperationIntrinsic intrinsic) {
        JsExpression leftExpression = Translation.translateAsExpression(leftJetExpression, context());
        if (JsAstUtils.isEmptyExpression(leftExpression)) {
            return leftExpression;
        }

        JsBlock rightBlock = new JsBlock();
        JsExpression rightExpression = Translation.translateAsExpression(rightJetExpression, context(), rightBlock);

        if (rightBlock.isEmpty()) {
            return intrinsic.apply(expression, leftExpression, rightExpression, context());
        }

        if (JsAstUtils.isEmptyExpression(rightExpression)) {
            if (TranslationUtils.isCacheNeeded(leftExpression)) {
                context().addStatementToCurrentBlock(leftExpression.makeStmt());
            }
            context().addStatementsToCurrentBlockFrom(rightBlock);
            return context().getEmptyExpression();
        }

        if (TranslationUtils.isCacheNeeded(leftExpression)) {
            TemporaryVariable temporaryVariable = context().declareTemporary(null);
            context().addStatementToCurrentBlock(JsAstUtils.assignment(temporaryVariable.reference(), leftExpression).makeStmt());
            leftExpression = temporaryVariable.reference();
        }
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
        JsExpression leftExpression = Translation.translateAsExpression(leftJetExpression, context());
        if (JsAstUtils.isEmptyExpression(leftExpression)) {
            return leftExpression;
        }

        JsBlock rightBlock = new JsBlock();
        JsExpression rightExpression = Translation.translateAsExpression(rightJetExpression, context(), rightBlock);

        if (rightBlock.isEmpty()) {
            return new JsBinaryOperation(operator, leftExpression, rightExpression);
        }
        else if (OperatorConventions.IDENTITY_EQUALS_OPERATIONS.contains(operationToken)) {
            context().addStatementsToCurrentBlockFrom(rightBlock);
            return new JsBinaryOperation(operator, leftExpression, rightExpression);
        }

        assert operationToken.equals(JetTokens.ANDAND) || operationToken.equals(JetTokens.OROR) : "Unsupported binary operation: " + expression.getText();
        boolean isOror = operationToken.equals(JetTokens.OROR);
        JsExpression literalResult = isOror ? JsLiteral.TRUE : JsLiteral.FALSE;
        leftExpression = isOror ? not(leftExpression) : leftExpression;

        JsIf ifStatement;
        JsExpression result;
        if (BindingContextUtilPackage.isUsedAsExpression(expression, context().bindingContext())) {
            if (!JsAstUtils.isEmptyExpression(rightExpression)) {
                if (rightExpression instanceof JsNameRef) {
                    result = rightExpression; // Reuse tmp variable
                } else {
                    TemporaryVariable resultVar = context().declareTemporary(rightExpression);
                    result = resultVar.reference();
                    rightBlock.getStatements().add(resultVar.assignmentExpression().makeStmt());
                }
                JsStatement assignmentStatement = JsAstUtils.assignment(result, literalResult).makeStmt();
                ifStatement = JsAstUtils.newJsIf(leftExpression, rightBlock, assignmentStatement);
            }
            else {
                ifStatement = JsAstUtils.newJsIf(leftExpression, rightBlock);
                result = literalResult;
            }
        }
        else {
            ifStatement = JsAstUtils.newJsIf(leftExpression, rightBlock);
            result = context().getEmptyExpression();
        }
        context().addStatementToCurrentBlock(ifStatement);
        return result;
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
            return Translation.translateAsExpression(rightJetExpression, context());
        } else {
            return Translation.translateAsExpression(leftJetExpression, context());
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
