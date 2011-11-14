package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
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

    private OperationTranslator(TranslationContext context) {
        super(context);
    }

    @NotNull
    JsExpression translate(JetUnaryExpression expression) {
        return translationContext().program().getBooleanLiteral(false);
    }

    @NotNull
    JsExpression translate(@NotNull JetBinaryExpression expression) {
        JsInvocation setterCall = translateAsSetterCall(expression);
        if (setterCall != null) {
            return setterCall;
        }
        return translateAsBinaryOperation(expression);
    }


    //TODO method too long
    @Nullable
    public JsInvocation translateAsSetterCall(@NotNull JetBinaryExpression expression) {
        JetToken jetOperationToken = (JetToken) expression.getOperationToken();
        if (!OperatorTable.isAssignment(jetOperationToken)) {
            return null;
        }
        JetExpression leftExpression = expression.getLeft();
        JsInvocation setterCall = Translation.propertyAccessTranslator(translationContext()).
                resolveAsPropertySetterCall(leftExpression);
        if (setterCall == null) {
            return null;
        }
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Selector should not be null";
        JsExpression right = AstUtil.convertToExpression
                (Translation.translateExpression(rightExpression, translationContext()));
        setterCall.setArguments(Arrays.asList(right));
        return setterCall;
    }

    @NotNull
    private JsExpression translateAsBinaryOperation(@NotNull JetBinaryExpression expression) {
        JsExpression left = AstUtil.convertToExpression
                (Translation.translateExpression(expression.getLeft(), translationContext()));
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Binary expression should have a right expression";
        JsExpression right = AstUtil.convertToExpression
                (Translation.translateExpression(rightExpression, translationContext()));
        JetToken jetOperationToken = (JetToken) expression.getOperationToken();
        return new JsBinaryOperation(OperatorTable.getBinaryOperator(jetOperationToken), left, right);
    }


}
