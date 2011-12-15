package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.reference.ReferenceAccessTranslator;

import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.k2js.translate.utils.PsiUtils.isPrefix;


/**
 * @author Pavel Talanov
 */
public final class IntrinsicIncrementTranslator extends IncrementTranslator {


    @NotNull
    public static JsExpression translate(@NotNull JetUnaryExpression expression,
                                         @NotNull TranslationContext context) {
        return (new IntrinsicIncrementTranslator(expression, context))
                .translate();
    }

    protected IntrinsicIncrementTranslator(@NotNull JetUnaryExpression expression,
                                           @NotNull TranslationContext context) {
        super(expression, context);
    }

    @Override
    @NotNull
    protected JsExpression translate() {
        if (isPrimitiveExpressionIncrement()) {
            return jsUnaryExpression();
        }
        return translateAsMethodCall();
    }

    private boolean isPrimitiveExpressionIncrement() {
        return accessTranslator instanceof ReferenceAccessTranslator;
    }

    @NotNull
    private JsExpression jsUnaryExpression() {
        JsUnaryOperator operator = OperatorTable.getUnaryOperator(getOperationToken(expression));
        JsExpression getExpression = accessTranslator.translateAsGet();
        if (isPrefix(expression)) {
            return new JsPrefixOperation(operator, getExpression);
        } else {
            return new JsPostfixOperation(operator, getExpression);
        }
    }

    @Override
    @NotNull
    protected JsExpression operationExpression(@NotNull JsExpression receiver) {
        return unaryAsBinary(receiver);
    }

    public JsBinaryOperation unaryAsBinary(@NotNull JsExpression leftExpression) {
        JsNumberLiteral oneLiteral = context().program().getNumberLiteral(1);
        JetToken token = getOperationToken(expression);
        if (token.equals(JetTokens.PLUSPLUS)) {
            return new JsBinaryOperation(JsBinaryOperator.ADD, leftExpression, oneLiteral);
        }
        if (token.equals(JetTokens.MINUSMINUS)) {
            return new JsBinaryOperation(JsBinaryOperator.SUB, leftExpression, oneLiteral);
        }
        throw new AssertionError("This method should be called only for increment and decrement operators");
    }

}
