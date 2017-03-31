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

package org.jetbrains.kotlin.js.translate.expression;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.operation.InOperationTranslator;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.not;

public final class WhenTranslator extends AbstractTranslator {
    @Nullable
    public static JsNode translate(@NotNull KtWhenExpression expression, @NotNull TranslationContext context) {
        return new WhenTranslator(expression, context).translate();
    }

    @NotNull
    private final KtWhenExpression whenExpression;

    @Nullable
    private final JsExpression expressionToMatch;

    private WhenTranslator(@NotNull KtWhenExpression expression, @NotNull TranslationContext context) {
        super(context);

        whenExpression = expression;

        KtExpression subject = expression.getSubjectExpression();
        expressionToMatch = subject != null ? context.defineTemporary(Translation.translateAsExpression(subject, context)) : null;
    }

    private JsNode translate() {
        JsIf currentIf = null;
        JsIf resultIf = null;
        for (KtWhenEntry entry : whenExpression.getEntries()) {
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

        if (currentIf != null && currentIf.getElseStatement() == null && isExhaustive()) {
            JsExpression noWhenMatchedInvocation = new JsInvocation(JsAstUtils.pureFqn("noWhenBranchMatched", Namer.kotlinObject()));
            currentIf.setElseStatement(JsAstUtils.asSyntheticStatement(noWhenMatchedInvocation));
        }

        return resultIf != null ? resultIf : JsLiteral.NULL;
    }

    private boolean isExhaustive() {
        KotlinType type = bindingContext().getType(whenExpression);
        boolean isStatement = type != null && KotlinBuiltIns.isUnit(type) && !type.isMarkedNullable();
        return CodegenUtil.isExhaustive(bindingContext(), whenExpression, isStatement);
    }

    @NotNull
    private static JsStatement translateEntryExpression(
            @NotNull KtWhenEntry entry,
            @NotNull TranslationContext context,
            @NotNull JsBlock block) {
        KtExpression expressionToExecute = entry.getExpression();
        assert expressionToExecute != null : "WhenEntry should have whenExpression to execute.";
        return Translation.translateAsStatement(expressionToExecute, context, block);
    }

    @NotNull
    private JsExpression translateConditions(@NotNull KtWhenEntry entry, @NotNull TranslationContext context) {
        KtWhenCondition[] conditions = entry.getConditions();

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
    private JsExpression translateOrCondition(
            @NotNull JsExpression leftExpression,
            @NotNull KtWhenCondition condition,
            @NotNull TranslationContext context
    ) {
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
    private JsExpression translateCondition(@NotNull KtWhenCondition condition, @NotNull TranslationContext context) {
        JsExpression patternMatchExpression = translateWhenConditionToBooleanExpression(condition, context);
        if (isNegated(condition)) {
            return not(patternMatchExpression);
        }
        return patternMatchExpression;
    }

    @NotNull
    private JsExpression translateWhenConditionToBooleanExpression(
            @NotNull KtWhenCondition condition,
            @NotNull TranslationContext context
    ) {
        if (condition instanceof KtWhenConditionIsPattern) {
            return translateIsCondition((KtWhenConditionIsPattern) condition, context);
        }
        else if (condition instanceof KtWhenConditionWithExpression) {
            return translateExpressionCondition((KtWhenConditionWithExpression) condition, context);
        }
        else if (condition instanceof KtWhenConditionInRange) {
            return translateRangeCondition((KtWhenConditionInRange) condition, context);
        }
        throw new AssertionError("Unsupported when condition " + condition.getClass());
    }

    @NotNull
    private JsExpression translateIsCondition(@NotNull KtWhenConditionIsPattern conditionIsPattern, @NotNull TranslationContext context) {
        JsExpression expressionToMatch = getExpressionToMatch();
        assert expressionToMatch != null : "An is-check is not allowed in when() without subject.";

        KtTypeReference typeReference = conditionIsPattern.getTypeReference();
        assert typeReference != null : "An is-check must have a type reference.";

        KtExpression expressionToMatchNonTranslated = whenExpression.getSubjectExpression();
        assert expressionToMatchNonTranslated != null : "expressionToMatch != null => expressionToMatchNonTranslated != null: " +
                                                        PsiUtilsKt.getTextWithLocation(conditionIsPattern);
        JsExpression result = Translation.patternTranslator(context).translateIsCheck(expressionToMatch, typeReference);
        return result != null ? result : JsLiteral.TRUE;
    }

    @NotNull
    private JsExpression translateExpressionCondition(@NotNull KtWhenConditionWithExpression condition, @NotNull TranslationContext context) {
        KtExpression patternExpression = condition.getExpression();
        assert patternExpression != null : "Expression pattern should have an expression.";

        JsExpression expressionToMatch = getExpressionToMatch();
        if (expressionToMatch == null) {
            return Translation.patternTranslator(context).translateExpressionForExpressionPattern(patternExpression);
        }
        else {
            KtExpression subject = whenExpression.getSubjectExpression();
            assert subject != null : "Subject must be non-null since expressionToMatch is non-null: " +
                                     PsiUtilsKt.getTextWithLocation(condition);
            KotlinType type = BindingUtils.getTypeForExpression(bindingContext(), whenExpression.getSubjectExpression());
            return Translation.patternTranslator(context).translateExpressionPattern(type, expressionToMatch, patternExpression);
        }
    }

    @NotNull
    private JsExpression translateRangeCondition(@NotNull KtWhenConditionInRange condition, @NotNull TranslationContext context) {
        KtExpression patternExpression = condition.getRangeExpression();
        assert patternExpression != null : "Expression pattern should have an expression: " +
                                           PsiUtilsKt.getTextWithLocation(condition);

        JsExpression expressionToMatch = getExpressionToMatch();
        assert expressionToMatch != null : "Range pattern is only available for 'when (C) { in ... }'  expressions: " +
                                           PsiUtilsKt.getTextWithLocation(condition);

        Map<KtExpression, JsExpression> subjectAliases = new HashMap<>();
        subjectAliases.put(whenExpression.getSubjectExpression(), expressionToMatch);
        TranslationContext callContext = context.innerContextWithAliasesForExpressions(subjectAliases);
        boolean negated = condition.getOperationReference().getReferencedNameElementType() == KtTokens.NOT_IN;
        return new InOperationTranslator(callContext, expressionToMatch, condition.getRangeExpression(), condition.getOperationReference(),
                                         negated).translate();
    }

    @Nullable
    private JsExpression getExpressionToMatch() {
        return expressionToMatch;
    }

    private static boolean isNegated(@NotNull KtWhenCondition condition) {
        if (condition instanceof KtWhenConditionIsPattern) {
            return ((KtWhenConditionIsPattern)condition).isNegated();
        }
        return false;
    }
}
