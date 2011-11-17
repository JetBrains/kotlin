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

    private WhenTranslator(@NotNull JetWhenExpression expression, @NotNull TranslationContext context) {
        super(context);
        this.whenExpression = expression;
        this.expressionToMatch = translateExpressionToMatch(whenExpression);
        this.dummyCounterName = context.enclosingScope().declareTemporary();
    }

    @NotNull
    JsNode translate() {
        return translateEntries();
    }

    private JsBlock translateEntries() {
        //JsFor resultingFor = generateDummyFor();
        List<JsStatement> entries = new ArrayList<JsStatement>();
        for (JetWhenEntry entry : whenExpression.getEntries()) {
            entries.add(translateEntry(entry));
        }
        return AstUtil.newBlock(entries);
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
        return new JsIf(condition, statementToExecute, null);
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
    private JsExpression translatePatternCondition(@NotNull JetWhenConditionIsPattern condition) {
        return Translation.typeOperationTranslator(translationContext()).
                translatePattern(getPattern(condition), expressionToMatch);
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
