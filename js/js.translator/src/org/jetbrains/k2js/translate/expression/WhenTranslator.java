/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.mutator.AssignToExpressionMutator;
import org.jetbrains.k2js.translate.utils.mutator.LastExpressionMutator;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;

/**
 * @author Pavel Talanov
 */
public final class WhenTranslator extends AbstractTranslator {

    @NotNull
    private final JetWhenExpression whenExpression;
    @Nullable
    private final JsExpression expressionToMatch;

    @Nullable
    private final TemporaryVariable result;

    private int currentEntryNumber = 0;

    @NotNull
    private final Pair<JsVars.JsVar, JsNameRef> dummyCounter;

    private WhenTranslator(@NotNull JetWhenExpression expression, @NotNull TranslationContext context) {
        super(context);

        whenExpression = expression;
        expressionToMatch = translateExpressionToMatch(whenExpression);
        if (context.bindingContext().get(BindingContext.EXPRESSION_TYPE, whenExpression) == JetStandardClasses.getTuple(0)) {
            result = null;
        }
        else {
            result = context.declareTemporary(null);
        }

        dummyCounter = context.dynamicContext().createTemporary(program().getNumberLiteral(0));
    }

    @NotNull
    public static JsNode translate(@NotNull JetWhenExpression expression, @NotNull TranslationContext context) {
        return new WhenTranslator(expression, context).translate();
    }

    @NotNull
    private JsNode translate() {
        JsFor resultingFor = generateDummyFor();
        resultingFor.setBody(new JsBlock(translateEntries()));
        if (result == null) {
            return resultingFor;
        }
        else {
            context().addStatementToCurrentBlock(resultingFor);
            return result.reference();
        }
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
        JsNumberLiteral jsEntryNumber = program().getNumberLiteral(currentEntryNumber++);
        JsExpression stepNumberEqualsCurrentEntryNumber = equality(dummyCounter.second, jsEntryNumber);
        return new JsIf(stepNumberEqualsCurrentEntryNumber, entryStatement);
    }

    @NotNull
    private JsFor generateDummyFor() {
        return new JsFor(new JsVars(dummyCounter.first), generateConditionStatement(), generateIncrementStatement());
    }

    @NotNull
    private JsBinaryOperation generateConditionStatement() {
        JsNumberLiteral entriesNumber = program().getNumberLiteral(whenExpression.getEntries().size());
        return new JsBinaryOperation(JsBinaryOperator.LT, dummyCounter.second, entriesNumber);
    }

    @NotNull
    private JsUnaryOperation generateIncrementStatement() {
        return new JsPostfixOperation(JsUnaryOperator.INC, dummyCounter.second);
    }

    @NotNull
    private JsStatement translateEntry(@NotNull JetWhenEntry entry) {
        JsStatement statementToExecute = withReturnValueCaptured(translateEntryExpression(entry));
        if (entry.isElse()) {
            return statementToExecute;
        }
        return new JsIf(translateConditions(entry), addDummyBreakIfNeed(statementToExecute));
    }

    @NotNull
    JsStatement withReturnValueCaptured(@NotNull JsNode node) {
        return result == null
               ? asStatement(node)
               : LastExpressionMutator.mutateLastExpression(node, new AssignToExpressionMutator(result.reference()));
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
        JsExpression patternMatchExpression = Translation.patternTranslator(context()).
            translatePattern(getPattern(condition), expressionToMatch);
        if (isNegated(condition)) {
            return negated(patternMatchExpression);
        }
        return patternMatchExpression;
    }

    private static boolean isNegated(@NotNull JetWhenCondition condition) {
        if (condition instanceof JetWhenConditionIsPattern) {
            return ((JetWhenConditionIsPattern)condition).isNegated();
        }
        return false;
    }

    @NotNull
    private static JetPattern getPattern(@NotNull JetWhenCondition condition) {
        JetPattern pattern;
        if (condition instanceof JetWhenConditionIsPattern) {
            pattern = ((JetWhenConditionIsPattern)condition).getPattern();
        }
        else if (condition instanceof JetWhenConditionWithExpression) {
            pattern = ((JetWhenConditionWithExpression)condition).getPattern();
        }
        else {
            throw new AssertionError("Wrong type of JetWhenCondition");
        }
        assert pattern != null : "Condition should have a non null pattern.";
        return pattern;
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
