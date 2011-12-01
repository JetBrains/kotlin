package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.general.TranslationContext;

/**
 * @author Talanov Pavel
 */
public final class IntrinsicUnaryOperationTranslator extends UnaryOperationTranslator {


    @NotNull
    public static JsExpression translate(@NotNull JetUnaryExpression expression,
                                         @NotNull TranslationContext context) {
        return (new IntrinsicUnaryOperationTranslator(expression, context))
                .translate();
    }

    protected IntrinsicUnaryOperationTranslator(@NotNull JetUnaryExpression expression,
                                                @NotNull TranslationContext context) {
        super(expression, context);
    }

    @Override
    @NotNull
    protected JsExpression translate() {
        if (isPropertyAccess) {
            return translateAsMethodCall();
        }
        return jsUnaryExpression();
    }

    @NotNull
    private JsExpression jsUnaryExpression() {
        JsUnaryOperator operator = OperatorTable.getUnaryOperator(getOperationToken());
        if (isPrefix) {
            return new JsPrefixOperation(operator, baseExpression);
        } else {
            return new JsPostfixOperation(operator, baseExpression);
        }
    }

    @Override
    @NotNull
    protected JsExpression operationExpression(@NotNull JsExpression receiver) {
        return unaryAsBinary(getOperationToken(), receiver);
    }

    public JsBinaryOperation unaryAsBinary(@NotNull JetToken token, @NotNull JsExpression expression) {
        JsNumberLiteral oneLiteral = context().program().getNumberLiteral(1);
        if (token.equals(JetTokens.PLUSPLUS)) {
            return new JsBinaryOperation(JsBinaryOperator.ADD, expression, oneLiteral);
        }
        if (token.equals(JetTokens.MINUSMINUS)) {
            return new JsBinaryOperation(JsBinaryOperator.SUB, expression, oneLiteral);
        }
        throw new AssertionError("This method should be called only for increment and decrement operators");
    }

}
