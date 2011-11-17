package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
//TODO: fix members order
public class WhenTranslator extends AbstractTranslator {

    @NotNull
    static public JsNode translateWhenExpression(@NotNull JetWhenExpression expression, @NotNull TranslationContext context) {
        WhenTranslator translator = new WhenTranslator(expression, context);
        return translator.translate();
    }

    @NotNull
    private final JetWhenExpression whenExpression;
    @NotNull
    private final JsExpression expressionToMatch;
    @NotNull
    private final JsName dummyCounterName;
    private int currentEntryNumber = 0;

    private WhenTranslator(@NotNull JetWhenExpression expression, @NotNull TranslationContext context) {
        super(context);
        this.whenExpression = expression;
        this.expressionToMatch = translateExpressionToMatch(whenExpression);
        this.dummyCounterName = context.enclosingScope().declareTemporary();
    }

    @NotNull
    JsNode translate() {
        JsFor resultingFor = generateDummyFor();
        List<JsStatement> entries = translateEntries();
        resultingFor.setBody(AstUtil.newBlock(entries));
        return resultingFor;
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
        JsExpression stepNumberEqualsCurrentEntryNumber = new JsBinaryOperation(JsBinaryOperator.EQ,
                dummyCounterName.makeRef(), translationContext().program().getNumberLiteral(currentEntryNumber));
        currentEntryNumber++;
        return new JsIf(stepNumberEqualsCurrentEntryNumber, entryStatement, null);
    }

    @NotNull
    private JsFor generateDummyFor() {
        JsFor result = new JsFor();
        result.setInitVars(generateInitStatement());
        result.setIncrExpr(generateIncrementStatement());
        result.setCondition(generateConditionStatement());
        return result;
    }

    @NotNull
    private JsBinaryOperation generateConditionStatement() {
        JsNumberLiteral entriesNumber = program().getNumberLiteral(whenExpression.getEntries().size());
        return new JsBinaryOperation(JsBinaryOperator.LT, dummyCounterName.makeRef(), entriesNumber);
    }

    @NotNull
    private JsPrefixOperation generateIncrementStatement() {
        return new JsPrefixOperation(JsUnaryOperator.INC, dummyCounterName.makeRef());
    }

    @NotNull
    private JsVars generateInitStatement() {
        return AstUtil.newVar(dummyCounterName, translationContext().program().getNumberLiteral(0));
    }

    @NotNull
    private JsStatement translateEntry(@NotNull JetWhenEntry entry) {
        JsStatement statementToExecute = translateExpressionToExecute(entry);
        if (entry.isElse()) {
            return statementToExecute;
        }
        JsExpression condition = translateConditions(entry);
        return new JsIf(condition, addDummyBreak(statementToExecute), null);
    }

    @NotNull
    private JsStatement translateExpressionToExecute(@NotNull JetWhenEntry entry) {
        JetExpression expressionToExecute = entry.getExpression();
        assert expressionToExecute != null : "WhenEntry should have whenExpression to execute.";
        return Translation.translateAsStatement(expressionToExecute, translationContext());
    }

    //TODO: ask what these conditions mean
    @NotNull
    private JsExpression translateConditions(@NotNull JetWhenEntry entry) {
        JetWhenCondition[] conditions = entry.getConditions();
//        for (JetWhenCondition condition : conditions) {
//            translateCondition(condition);
//        }
        return translateCondition(conditions[0]);
    }

    @NotNull
    private JsExpression translateCondition(@NotNull JetWhenCondition condition) {
        if (condition instanceof JetWhenConditionIsPattern) {
            return translatePatternCondition((JetWhenConditionIsPattern) condition);
        }
        throw new AssertionError("Unsupported when condition " + condition.getClass());
    }

    @NotNull
    private JsBlock addDummyBreak(@NotNull JsStatement statement) {
        return AstUtil.newBlock(statement, new JsBreak());
    }


    @NotNull
    private JsExpression translatePatternCondition(@NotNull JetWhenConditionIsPattern condition) {
        JsExpression patternMatchExpression = Translation.typeOperationTranslator(translationContext()).
                translatePattern(getPattern(condition), expressionToMatch);
        if (condition.isNegated()) {
            return AstUtil.negated(patternMatchExpression);
        }
        return patternMatchExpression;
    }

    @NotNull
    private JetPattern getPattern(@NotNull JetWhenConditionIsPattern condition) {
        JetPattern pattern = condition.getPattern();
        assert pattern != null : "Is condition should have a non null pattern.";
        return pattern;
    }

    @NotNull
    private JsExpression translateExpressionToMatch(@NotNull JetWhenExpression expression) {
        JetExpression subject = expression.getSubjectExpression();
        assert subject != null : "Subject should not be null.";
        return Translation.translateAsExpression(subject, translationContext());
    }
}
