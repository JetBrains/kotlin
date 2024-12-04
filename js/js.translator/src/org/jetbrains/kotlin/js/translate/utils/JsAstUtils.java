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

package org.jetbrains.kotlin.js.translate.utils;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;

import java.util.List;

public final class JsAstUtils {
    private JsAstUtils() {
    }

    @NotNull
    public static JsExpression notOptimized(@NotNull JsExpression expression) {
        if (expression instanceof JsUnaryOperation) {
            JsUnaryOperation unary = (JsUnaryOperation) expression;
            if (unary.getOperator() == JsUnaryOperator.NOT) return unary.getArg();
        }
        else if (expression instanceof JsBinaryOperation) {
            JsBinaryOperation binary = (JsBinaryOperation) expression;
            switch (binary.getOperator()) {
                case AND:
                    return or(notOptimized(binary.getArg1()), notOptimized(binary.getArg2()));
                case OR:
                    return and(notOptimized(binary.getArg1()), notOptimized(binary.getArg2()));
                case EQ:
                    return new JsBinaryOperation(JsBinaryOperator.NEQ, binary.getArg1(), binary.getArg2());
                case NEQ:
                    return new JsBinaryOperation(JsBinaryOperator.EQ, binary.getArg1(), binary.getArg2());
                case REF_EQ:
                    return inequality(binary.getArg1(), binary.getArg2());
                case REF_NEQ:
                    return equality(binary.getArg1(), binary.getArg2());
                case LT:
                    return greaterThanEq(binary.getArg1(), binary.getArg2());
                case LTE:
                    return greaterThan(binary.getArg1(), binary.getArg2());
                case GT:
                    return lessThanEq(binary.getArg1(), binary.getArg2());
                case GTE:
                    return lessThan(binary.getArg1(), binary.getArg2());
                default:
                    break;
            }
        }

        return not(expression);
    }

    @NotNull
    public static JsBinaryOperation and(@NotNull JsExpression op1, @NotNull JsExpression op2) {
        return new JsBinaryOperation(JsBinaryOperator.AND, op1, op2);
    }

    @NotNull
    public static JsBinaryOperation or(@NotNull JsExpression op1, @NotNull JsExpression op2) {
        return new JsBinaryOperation(JsBinaryOperator.OR, op1, op2);
    }

    @NotNull
    public static JsBinaryOperation equality(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.REF_EQ, arg1, arg2);
    }

    @NotNull
    private static JsBinaryOperation inequality(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.REF_NEQ, arg1, arg2);
    }

    @NotNull
    private static JsBinaryOperation lessThanEq(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.LTE, arg1, arg2);
    }

    @NotNull
    private static JsBinaryOperation lessThan(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.LT, arg1, arg2);
    }

    @NotNull
    private static JsBinaryOperation greaterThan(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.GT, arg1, arg2);
    }

    @NotNull
    private static JsBinaryOperation greaterThanEq(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.GTE, arg1, arg2);
    }

    @NotNull
    public static JsBinaryOperation assignment(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsBinaryOperation(JsBinaryOperator.ASG, left, right);
    }

    public static JsStatement asSyntheticStatement(@NotNull JsExpression expression) {
        JsExpressionStatement statement = new JsExpressionStatement(expression);
        MetadataProperties.setSynthetic(statement, true);
        return statement;
    }

    @Nullable
    public static Pair<JsExpression, JsExpression> decomposeAssignment(@NotNull JsExpression expr) {
        if (!(expr instanceof JsBinaryOperation)) return null;

        JsBinaryOperation binary = (JsBinaryOperation) expr;
        if (binary.getOperator() != JsBinaryOperator.ASG) return null;

        return new Pair<>(binary.getArg1(), binary.getArg2());
    }

    @Nullable
    public static Pair<JsName, JsExpression> decomposeAssignmentToVariable(@NotNull JsExpression expr) {
        Pair<JsExpression, JsExpression> assignment = decomposeAssignment(expr);
        if (assignment == null || !(assignment.getFirst() instanceof JsNameRef)) return null;

        JsNameRef nameRef = (JsNameRef) assignment.getFirst();
        if (nameRef.getName() == null || nameRef.getQualifier() != null) return null;

        return new Pair<>(nameRef.getName(), assignment.getSecond());
    }

    @NotNull
    public static JsPrefixOperation not(@NotNull JsExpression expression) {
        return new JsPrefixOperation(JsUnaryOperator.NOT, expression);
    }

    @NotNull
    public static JsVars newVar(@NotNull JsName name, @Nullable JsExpression expr) {
        return new JsVars(new JsVars.JsVar(name, expr));
    }

    @NotNull
    public static JsExpression newSequence(@NotNull List<JsExpression> expressions) {
        assert !expressions.isEmpty();
        if (expressions.size() == 1) {
            return expressions.get(0);
        }
        JsExpression result = expressions.get(0);
        for (int i = 1; i < expressions.size(); i++) {
            result = new JsBinaryOperation(JsBinaryOperator.COMMA, result, expressions.get(i));
        }
        return result;
    }
}
