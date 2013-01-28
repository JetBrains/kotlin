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

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.mutator.AssignToExpressionMutator;
import org.jetbrains.k2js.translate.utils.mutator.LastExpressionMutator;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;

public final class WhenTranslator extends AbstractTranslator {

    @NotNull
    public static JsNode translate(@NotNull JetWhenExpression expression, @NotNull TranslationContext context) {
        WhenTranslator translator = new WhenTranslator(expression, context);
        return translator.translate();
    }

    @NotNull
    private final JetWhenExpression whenExpression;
    @Nullable
    private final TemporaryVariable expressionToMatch;
    @NotNull
    private final TemporaryVariable dummyCounter;
    @NotNull
    private final TemporaryVariable result;

    private int currentEntryNumber = 0;

    private WhenTranslator(@NotNull JetWhenExpression expression, @NotNull TranslationContext context) {
        super(context);
        this.whenExpression = expression;
        JsExpression expressionToMatch = translateExpressionToMatch(whenExpression);
        this.expressionToMatch = expressionToMatch != null ? context.declareTemporary(expressionToMatch) : null;
        this.dummyCounter = context.declareTemporary(program().getNumberLiteral(0));
        this.result = context.declareTemporary(JsLiteral.NULL);
    }

    @NotNull
    JsNode translate() {
        JsFor resultingFor = generateDummyFor();
        resultingFor.setBody(new JsBlock(translateEntries()));
        context().addStatementToCurrentBlock(resultingFor);
        return result.reference();
    }

    @NotNull
    private List<JsStatement> translateEntries() {
        List<JsStatement> entries = new ArrayList<JsStatement>();
        for (JetWhenEntry entry : whenExpression.getEntries()) {
            entries.add(surroundWithDummyIf(translateEntry(entry)));
        }
        return entries;
    }

    @NotNull
    private JsStatement surroundWithDummyIf(@NotNull JsStatement entryStatement) {
        JsNumberLiteral jsEntryNumber = program().getNumberLiteral(this.currentEntryNumber);
        JsExpression stepNumberEqualsCurrentEntryNumber = equality(dummyCounter.reference(), jsEntryNumber);
        currentEntryNumber++;
        return new JsIf(stepNumberEqualsCurrentEntryNumber, entryStatement, null);
    }

    @NotNull
    private JsFor generateDummyFor() {
        return new JsFor(generateInitExpressions(), generateConditionStatement(), generateIncrementStatement());
    }

    @NotNull
    private JsExpression generateInitExpressions() {
        List<JsExpression> initExpressions = Lists.newArrayList(dummyCounter.assignmentExpression());
        if (expressionToMatch != null) {
            initExpressions.add(expressionToMatch.assignmentExpression());
        }
        return newSequence(initExpressions);
    }

    @NotNull
    private JsBinaryOperation generateConditionStatement() {
        JsNumberLiteral entriesNumber = program().getNumberLiteral(whenExpression.getEntries().size());
        return new JsBinaryOperation(JsBinaryOperator.LT, dummyCounter.reference(), entriesNumber);
    }

    @NotNull
    private JsPrefixOperation generateIncrementStatement() {
        return new JsPrefixOperation(JsUnaryOperator.INC, dummyCounter.reference());
    }

    @NotNull
    private JsStatement translateEntry(@NotNull JetWhenEntry entry) {
        JsStatement statementToExecute = withReturnValueCaptured(translateEntryExpression(entry));
        if (entry.isElse()) {
            return statementToExecute;
        }
        JsExpression condition = translateConditions(entry);
        return new JsIf(condition, addDummyBreakIfNeed(statementToExecute), null);
    }

    @NotNull
    JsStatement withReturnValueCaptured(@NotNull JsNode node) {
        return convertToStatement(LastExpressionMutator.mutateLastExpression(node,
                                                                             new AssignToExpressionMutator(result.reference())));
    }

    @NotNull
    private JsNode translateEntryExpression(@NotNull JetWhenEntry entry) {
        JetExpression expressionToExecute = entry.getExpression();
        assert expressionToExecute != null : "WhenEntry should have whenExpression to execute.";
        return Translation.translateExpression(expressionToExecute, context());
    }

    @NotNull
    private JsExpression translateConditions(@NotNull JetWhenEntry entry) {
        List<JsExpression> conditions = new ArrayList<JsExpression>();
        for (JetWhenCondition condition : entry.getConditions()) {
            conditions.add(translateCondition(condition));
        }
        return anyOfThemIsTrue(conditions);
    }

    @NotNull
    private static JsExpression anyOfThemIsTrue(List<JsExpression> conditions) {
        assert !conditions.isEmpty() : "When entry (not else) should have at least one condition";
        JsExpression current = null;
        for (JsExpression condition : conditions) {
            current = addCaseCondition(current, condition);
        }
        assert current != null;
        return current;
    }

    @NotNull
    private static JsExpression addCaseCondition(@Nullable JsExpression current, @NotNull JsExpression condition) {
        if (current == null) {
            return condition;
        }
        else {
            return or(current, condition);
        }
    }

    @NotNull
    private JsExpression translateCondition(@NotNull JetWhenCondition condition) {
        if ((condition instanceof JetWhenConditionIsPattern) || (condition instanceof JetWhenConditionWithExpression)) {
            return translatePatternCondition(condition);
        }
        throw new AssertionError("Unsupported when condition " + condition.getClass());
    }

    @NotNull
    private static JsStatement addDummyBreakIfNeed(@NotNull JsStatement statement) {
        return statement instanceof JsReturn ? statement : new JsBlock(statement, new JsBreak());
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
        return expressionToMatch != null ? expressionToMatch.reference() : null;
    }

    private static boolean isNegated(@NotNull JetWhenCondition condition) {
        if (condition instanceof JetWhenConditionIsPattern) {
            return ((JetWhenConditionIsPattern)condition).isNegated();
        }
        return false;
    }

    @Nullable
    private JsExpression translateExpressionToMatch(@NotNull JetWhenExpression expression) {
        JetExpression subject = expression.getSubjectExpression();
        if (subject == null) {
            return null;
        }
        return Translation.translateAsExpression(subject, context());
    }
}
