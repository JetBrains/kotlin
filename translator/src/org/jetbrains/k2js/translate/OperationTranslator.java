package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetToken;

import java.util.Arrays;

/**
 * @author Talanov Pavel
 */
public final class OperationTranslator extends AbstractTranslator {

    @NotNull
    static public OperationTranslator newInstance(@NotNull TranslationContext context) {
        return new OperationTranslator(context);
    }

    private OperationTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @NotNull
    JsPostfixOperation translatePostfixOperation(@NotNull JetPostfixExpression expression) {
        JsUnaryOperator operator = OperatorTable.getUnaryOperator(getOperationToken(expression));
        JsExpression baseExpression = translateBaseExpression(expression);
        return new JsPostfixOperation(operator, baseExpression);
    }

    @NotNull
    JsPrefixOperation translatePrefixOperation(@NotNull JetPrefixExpression expression) {
        JsUnaryOperator operator = OperatorTable.getUnaryOperator(getOperationToken(expression));
        JsExpression baseExpression = translateBaseExpression(expression);
        return new JsPrefixOperation(operator, baseExpression);
    }

    @NotNull
    private JsExpression translateBaseExpression(@NotNull JetUnaryExpression expression) {
        JetExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null : "Unary operation should have not null base expression";
        return AstUtil.convertToExpression
                (Translation.translateExpression(baseExpression, translationContext()));
    }

    @NotNull
    private JetToken getOperationToken(@NotNull JetUnaryExpression expression) {
        JetSimpleNameExpression operationExpression = expression.getOperationSign();
        IElementType elementType = operationExpression.getReferencedNameElementType();
        assert elementType instanceof JetToken : "Unary operation should have IElementType of type JetToken";
        return (JetToken) elementType;
    }

    @NotNull
    JsExpression translate(@NotNull JetBinaryExpression expression) {
        JsInvocation setterCall = translateAsSetterCall(expression);
        if (setterCall != null) {
            return setterCall;
        }
        return translateAsBinaryOperation(expression);
    }

    @Nullable
    public JsInvocation translateAsSetterCall(@NotNull JetBinaryExpression expression) {
        JetToken jetOperationToken = getOperationToken(expression);
        if (!OperatorTable.isAssignment(jetOperationToken)) {
            return null;
        }
        JetExpression leftExpression = expression.getLeft();
        JsInvocation setterCall = Translation.propertyAccessTranslator(translationContext()).
                resolveAsPropertySetterCall(leftExpression);
        if (setterCall == null) {
            return null;
        }
        JsExpression right = translateRightExpression(expression);
        setterCall.setArguments(Arrays.asList(right));
        return setterCall;
    }

    @NotNull
    private JetToken getOperationToken(@NotNull JetBinaryExpression expression) {
        return (JetToken) expression.getOperationToken();
    }

    @NotNull
    private JsExpression translateAsBinaryOperation(@NotNull JetBinaryExpression expression) {
        JsExpression left = AstUtil.convertToExpression
                (Translation.translateExpression(expression.getLeft(), translationContext()));
        JsExpression right = translateRightExpression(expression);
        JetToken jetOperationToken = getOperationToken(expression);
        return new JsBinaryOperation(OperatorTable.getBinaryOperator(jetOperationToken), left, right);
    }

    @NotNull
    private JsExpression translateRightExpression(@NotNull JetBinaryExpression expression) {
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Binary expression should have a right expression";
        return AstUtil.convertToExpression
                (Translation.translateExpression(rightExpression, translationContext()));
    }


}
