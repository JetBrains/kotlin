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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation;
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator;
import org.jetbrains.kotlin.js.backend.ast.JsBlock;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.reference.AccessTranslator;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.lexer.KtSingleValueToken;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.isAssignment;
import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.isSimpleNameExpressionNotDelegatedLocalVar;

public final class IntrinsicAssignmentTranslator extends AssignmentTranslator {
    private final JsExpression right;
    private final AccessTranslator accessTranslator;
    private final boolean rightExpressionTrivial;
    private final JsBlock rightBlock = new JsBlock();

    @NotNull
    public static JsExpression doTranslate(@NotNull KtBinaryExpression expression, @NotNull TranslationContext context) {
        return (new IntrinsicAssignmentTranslator(expression, context)).translate();
    }

    private IntrinsicAssignmentTranslator(@NotNull KtBinaryExpression expression, @NotNull TranslationContext context) {
        super(expression, context);

        right = translateRightExpression(context, expression);
        rightExpressionTrivial = rightBlock.isEmpty();
        KtExpression left = expression.getLeft();
        assert left != null;
        accessTranslator = createAccessTranslator(left, !rightExpressionTrivial);
    }

    private JsExpression translateRightExpression(TranslationContext context, KtBinaryExpression expression) {
        JsExpression result = TranslationUtils.translateRightExpression(context, expression, rightBlock);
        KotlinType leftType = context.bindingContext().getType(expression.getLeft());
        KotlinType rightType = context.bindingContext().getType(expression.getRight());
        if (rightType != null && KotlinBuiltIns.isCharOrNullableChar(rightType)) {
            if (leftType != null && KotlinBuiltIns.isStringOrNullableString(leftType)) {
                result = JsAstUtils.charToString(result);
            }
            else if (leftType == null || !KotlinBuiltIns.isCharOrNullableChar(leftType)) {
                result = JsAstUtils.charToBoxedChar(result);
            }
        }
        return result;
    }

    @NotNull
    private JsExpression translate() {
        if (isAssignment(getOperationToken(expression))) {
            return translateAsPlainAssignment();
        }
        return translateAsAssignmentOperation();
    }

    @NotNull
    private JsExpression translateAsAssignmentOperation() {
        if (isSimpleNameExpressionNotDelegatedLocalVar(expression.getLeft(), context()) && rightExpressionTrivial) {
            return translateAsPlainAssignmentOperation();
        }
        return translateAsAssignToCounterpart();
    }

    @NotNull
    private JsExpression translateAsAssignToCounterpart() {
        JsBinaryOperator operator = getCounterpartOperator();
        JsExpression oldValue = accessTranslator.translateAsGet();
        if (!rightExpressionTrivial) {
            oldValue = context().defineTemporary(oldValue);
        }
        JsBinaryOperation counterpartOperation = new JsBinaryOperation(operator, oldValue, right);
        context().addStatementsToCurrentBlockFrom(rightBlock);
        return accessTranslator.translateAsSet(counterpartOperation);
    }

    @NotNull
    private JsBinaryOperator getCounterpartOperator() {
        KtToken assignmentOperationToken = getOperationToken(expression);
        assert assignmentOperationToken instanceof KtSingleValueToken;
        assert OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(assignmentOperationToken);
        KtToken counterpartToken = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(assignmentOperationToken);
        assert OperatorTable.hasCorrespondingBinaryOperator(counterpartToken) :
                "Unsupported token encountered: " + counterpartToken.toString();
        return OperatorTable.getBinaryOperator(counterpartToken);
    }

    @NotNull
    private JsExpression translateAsPlainAssignmentOperation() {
        context().addStatementsToCurrentBlockFrom(rightBlock);
        JsBinaryOperator operator = getAssignmentOperator();
        return new JsBinaryOperation(operator, accessTranslator.translateAsGet(), right);
    }

    @NotNull
    private JsBinaryOperator getAssignmentOperator() {
        KtToken token = getOperationToken(expression);
        assert token instanceof KtSingleValueToken;
        assert OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(token);
        assert OperatorTable.hasCorrespondingBinaryOperator(token) :
                "Unsupported token encountered: " + token.toString();
        return OperatorTable.getBinaryOperator(token);
    }

    @NotNull
    private JsExpression translateAsPlainAssignment() {
        context().addStatementsToCurrentBlockFrom(rightBlock);
        return accessTranslator.translateAsSet(right);
    }
}
