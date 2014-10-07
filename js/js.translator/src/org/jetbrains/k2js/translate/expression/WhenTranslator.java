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

package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.JsAstUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.negated;

public final class WhenTranslator extends AbstractTranslator {
    @Nullable
    public static JsNode translate(@NotNull JetWhenExpression expression, @NotNull TranslationContext context) {
        return new WhenTranslator(expression, context).translate();
    }

    @NotNull
    private final JetWhenExpression whenExpression;

    @Nullable
    private final JsExpression expressionToMatch;

    private WhenTranslator(@NotNull JetWhenExpression expression, @NotNull TranslationContext context) {
        super(context);

        whenExpression = expression;

        JetExpression subject = expression.getSubjectExpression();
        if (subject != null) {
            JsExpression subjectExpression = Translation.translateAsExpression(subject, context);
            if (TranslationUtils.isCacheNeeded(subjectExpression)) {
                TemporaryVariable subjectVar = context.declareTemporary(null);
                context.addStatementToCurrentBlock(JsAstUtils.assignment(subjectVar.reference(), subjectExpression).makeStmt());
                subjectExpression = subjectVar.reference();
            }
            expressionToMatch = subjectExpression;
        }
        else {
            expressionToMatch = null;
        }
    }

    private JsStatement translate() {
        if (expressionToMatch != null && JsAstUtils.isEmptyExpression(expressionToMatch)) {
            return context().getEmptyStatement();
        }

        JsIf currentIf = null;
        JsIf resultIf = null;
        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JsBlock statementBlock = new JsBlock();
            JsStatement statement = translateEntryExpression(entry, context(), statementBlock);

            if (resultIf == null && entry.isElse()) {
                context().addStatementsToCurrentBlockFrom(statementBlock);
                return statement;
            }
            statement = JsAstUtils.mergeStatementInBlockIfNeeded(statement, statementBlock);

            if (resultIf == null) {
                currentIf = JsAstUtils.newJsIf(translateConditions(entry, context()), statement);
                resultIf = currentIf;
            }
            else {
                if (entry.isElse()) {
                    currentIf.setElseStatement(statement);
                    return resultIf;
                }
                JsBlock conditionsBlock = new JsBlock();
                JsIf nextIf = JsAstUtils.newJsIf(translateConditions(entry, context().innerBlock(conditionsBlock)), statement);
                JsStatement statementToAdd = JsAstUtils.mergeStatementInBlockIfNeeded(nextIf, conditionsBlock);
                currentIf.setElseStatement(statementToAdd);
                currentIf = nextIf;
            }
        }
        return resultIf;
    }

    @NotNull
    private static JsStatement translateEntryExpression(
            @NotNull JetWhenEntry entry,
            @NotNull TranslationContext context,
            @NotNull JsBlock block) {
        JetExpression expressionToExecute = entry.getExpression();
        assert expressionToExecute != null : "WhenEntry should have whenExpression to execute.";
        return Translation.translateAsStatement(expressionToExecute, context, block);
    }

    @NotNull
    private JsExpression translateConditions(@NotNull JetWhenEntry entry, @NotNull TranslationContext context) {
        JetWhenCondition[] conditions = entry.getConditions();

        assert conditions.length > 0 : "When entry (not else) should have at least one condition";

        if (conditions.length == 1) {
            return translateCondition(conditions[0], context);
        }

        JsExpression result = translateCondition(conditions[0], context);
        for (int i = 1; i < conditions.length; i++) {
            result = translateOrCondition(result, conditions[i], context);
        }

        return result;
    }

    @NotNull
    private JsExpression translateOrCondition(@NotNull JsExpression leftExpression, @NotNull JetWhenCondition condition, @NotNull TranslationContext context) {
        TranslationContext rightContext = context.innerBlock();
        JsExpression rightExpression = translateCondition(condition, rightContext);
        context.moveVarsFrom(rightContext);
        if (rightContext.currentBlockIsEmpty()) {
            return new JsBinaryOperation(JsBinaryOperator.OR, leftExpression, rightExpression);
        } else {
            assert rightExpression instanceof JsNameRef : "expected JsNameRef, but: " + rightExpression;
            JsNameRef result = (JsNameRef) rightExpression;
            JsIf ifStatement = JsAstUtils.newJsIf(leftExpression, JsAstUtils.assignment(result, JsLiteral.TRUE).makeStmt(),
                                                  rightContext.getCurrentBlock());
            context.addStatementToCurrentBlock(ifStatement);
            return result;
        }
    }

    @NotNull
    private JsExpression translateCondition(@NotNull JetWhenCondition condition, @NotNull TranslationContext context) {
        if ((condition instanceof JetWhenConditionIsPattern) || (condition instanceof JetWhenConditionWithExpression)) {
            return translatePatternCondition(condition, context);
        }
        throw new AssertionError("Unsupported when condition " + condition.getClass());
    }

    @NotNull
    private JsExpression translatePatternCondition(@NotNull JetWhenCondition condition, @NotNull TranslationContext context) {
        JsExpression patternMatchExpression = translateWhenConditionToBooleanExpression(condition, context);
        if (isNegated(condition)) {
            return negated(patternMatchExpression);
        }
        return patternMatchExpression;
    }

    @NotNull
    private JsExpression translateWhenConditionToBooleanExpression(@NotNull JetWhenCondition condition, @NotNull TranslationContext context) {
        if (condition instanceof JetWhenConditionIsPattern) {
            return translateIsCondition((JetWhenConditionIsPattern) condition, context);
        }
        else if (condition instanceof JetWhenConditionWithExpression) {
            return translateExpressionCondition((JetWhenConditionWithExpression) condition, context);
        }
        throw new AssertionError("Wrong type of JetWhenCondition");
    }

    @NotNull
    private JsExpression translateIsCondition(@NotNull JetWhenConditionIsPattern conditionIsPattern, @NotNull TranslationContext context) {
        JsExpression expressionToMatch = getExpressionToMatch();
        assert expressionToMatch != null : "An is-check is not allowed in when() without subject.";

        JetTypeReference typeReference = conditionIsPattern.getTypeReference();
        assert typeReference != null : "An is-check must have a type reference.";

        return Translation.patternTranslator(context).translateIsCheck(expressionToMatch, typeReference);
    }

    @NotNull
    private JsExpression translateExpressionCondition(@NotNull JetWhenConditionWithExpression condition, @NotNull TranslationContext context) {
        JetExpression patternExpression = condition.getExpression();
        assert patternExpression != null : "Expression pattern should have an expression.";

        JsExpression expressionToMatch = getExpressionToMatch();
        if (expressionToMatch == null) {
            return Translation.patternTranslator(context).translateExpressionForExpressionPattern(patternExpression);
        }
        else {
            return Translation.patternTranslator(context).translateExpressionPattern(expressionToMatch, patternExpression);
        }
    }

    @Nullable
    private JsExpression getExpressionToMatch() {
        return expressionToMatch;
    }

    private static boolean isNegated(@NotNull JetWhenCondition condition) {
        if (condition instanceof JetWhenConditionIsPattern) {
            return ((JetWhenConditionIsPattern)condition).isNegated();
        }
        return false;
    }
}
