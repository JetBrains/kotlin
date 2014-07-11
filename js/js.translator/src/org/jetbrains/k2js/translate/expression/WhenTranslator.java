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
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.TranslationUtils;
import org.jetbrains.k2js.translate.utils.mutator.AssignToExpressionMutator;
import org.jetbrains.k2js.translate.utils.mutator.LastExpressionMutator;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.convertToStatement;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.negated;

public final class WhenTranslator extends AbstractTranslator {
    @Nullable
    public static JsNode translate(@NotNull JetWhenExpression expression, @NotNull TranslationContext context) {
        WhenTranslator translator = new WhenTranslator(expression, context);

        if (BindingContextUtilPackage.isUsedAsStatement(expression, context.bindingContext())) {
            JsBlock jsBlock = new JsBlock();
            translator.translateAsStatement(jsBlock.getStatements());
            return jsBlock;
        }

        return translator.translateAsExpression();
    }

    @NotNull
    private final JetWhenExpression whenExpression;

    @Nullable
    private final Pair<JsVars.JsVar, JsExpression> expressionToMatch;

    @Nullable
    private Pair<JsVars.JsVar, JsExpression> result;

    private WhenTranslator(@NotNull JetWhenExpression expression, @NotNull TranslationContext context) {
        super(context);

        whenExpression = expression;

        JetExpression subject = expression.getSubjectExpression();
        if (subject != null) {
            expressionToMatch = TranslationUtils.createTemporaryIfNeed(Translation.translateAsExpression(subject, context()), context);
        }
        else {
            expressionToMatch = null;
        }
    }

    @Nullable
    private JsNode translateAsExpression() {
        result = context().dynamicContext().createTemporary(null);
        translateAsStatement(context().dynamicContext().jsBlock().getStatements());
        return result.second;
    }

    private void translateAsStatement(List<JsStatement> statements) {
        addTempVarsStatement(statements);

        JsIf prevIf = null;
        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JsBlock entryBody = new JsBlock();
            JsStatement statement = withReturnValueCaptured(translateEntryExpression(entry, context().innerBlock(entryBody)));
            if (!entryBody.isEmpty()) {
                entryBody.getStatements().add(statement);
                statement = entryBody;
            }

            JsStatement statementToAdd = entry.isElse() ? statement : new JsIf(translateConditions(entry), statement);
            if (prevIf == null) {
                statements.add(statementToAdd);
            }
            else {
                prevIf.setElseStatement(statementToAdd);
            }

            if (entry.isElse()) break;

            assert statementToAdd instanceof JsIf : "Non-else entry expected: " + entry.getText();
            prevIf = (JsIf) statementToAdd;
        }
    }

    private void addTempVarsStatement(List<JsStatement> statements) {
        JsVars vars = new JsVars();
        if (expressionToMatch != null && expressionToMatch.first != null) {
            vars.add(expressionToMatch.first);
        }
        if (result != null) {
            vars.add(result.first);
        }

        if (!vars.isEmpty()) {
            statements.add(vars);
        }
    }

    @NotNull
    private JsStatement withReturnValueCaptured(@NotNull JsNode node) {
        return result == null
               ? convertToStatement(node)
               : LastExpressionMutator.mutateLastExpression(node, new AssignToExpressionMutator(result.second));
    }

    @NotNull
    private JsNode translateEntryExpression(@NotNull JetWhenEntry entry, @NotNull TranslationContext context) {
        JetExpression expressionToExecute = entry.getExpression();
        assert expressionToExecute != null : "WhenEntry should have whenExpression to execute.";
        return Translation.translateExpression(expressionToExecute, context);
    }

    @NotNull
    private JsExpression translateConditions(@NotNull JetWhenEntry entry) {
        JetWhenCondition[] conditions = entry.getConditions();

        assert conditions.length > 0 : "When entry (not else) should have at least one condition";

        if (conditions.length == 1) {
            return translateCondition(conditions[0]);
        }

        JsExpression result = translateCondition(conditions[0]);
        for (int i = 1; i < conditions.length; i++) {
            result = new JsBinaryOperation(JsBinaryOperator.OR, translateCondition(conditions[i]), result);
        }

        return result;
    }

    @NotNull
    private JsExpression translateCondition(@NotNull JetWhenCondition condition) {
        if ((condition instanceof JetWhenConditionIsPattern) || (condition instanceof JetWhenConditionWithExpression)) {
            return translatePatternCondition(condition);
        }
        throw new AssertionError("Unsupported when condition " + condition.getClass());
    }

    @NotNull
    private JsExpression translatePatternCondition(@NotNull JetWhenCondition condition) {
        JsExpression patternMatchExpression = translateWhenConditionToBooleanExpression(condition);
        if (isNegated(condition)) {
            return negated(patternMatchExpression);
        }
        return patternMatchExpression;
    }

    @NotNull
    private JsExpression translateWhenConditionToBooleanExpression(@NotNull JetWhenCondition condition) {
        if (condition instanceof JetWhenConditionIsPattern) {
            return translateIsCondition((JetWhenConditionIsPattern) condition);
        }
        else if (condition instanceof JetWhenConditionWithExpression) {
            return translateExpressionCondition((JetWhenConditionWithExpression) condition);
        }
        throw new AssertionError("Wrong type of JetWhenCondition");
    }

    @NotNull
    private JsExpression translateIsCondition(@NotNull JetWhenConditionIsPattern conditionIsPattern) {
        JsExpression expressionToMatch = getExpressionToMatch();
        assert expressionToMatch != null : "An is-check is not allowed in when() without subject.";

        JetTypeReference typeReference = conditionIsPattern.getTypeRef();
        assert typeReference != null : "An is-check must have a type reference.";

        return Translation.patternTranslator(context()).translateIsCheck(expressionToMatch, typeReference);
    }

    @NotNull
    private JsExpression translateExpressionCondition(@NotNull JetWhenConditionWithExpression condition) {
        JetExpression patternExpression = condition.getExpression();
        assert patternExpression != null : "Expression pattern should have an expression.";

        JsExpression expressionToMatch = getExpressionToMatch();
        if (expressionToMatch == null) {
            return Translation.patternTranslator(context()).translateExpressionForExpressionPattern(patternExpression);
        }
        else {
            return Translation.patternTranslator(context()).translateExpressionPattern(expressionToMatch, patternExpression);
        }
    }

    @Nullable
    private JsExpression getExpressionToMatch() {
        return expressionToMatch != null ? expressionToMatch.second : null;
    }

    private static boolean isNegated(@NotNull JetWhenCondition condition) {
        if (condition instanceof JetWhenConditionIsPattern) {
            return ((JetWhenConditionIsPattern)condition).isNegated();
        }
        return false;
    }
}
