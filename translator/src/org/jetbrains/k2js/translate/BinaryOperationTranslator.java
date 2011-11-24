package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lexer.JetToken;

import java.util.Arrays;

/**
 * @author Talanov Pavel
 */
public final class BinaryOperationTranslator extends OperationTranslator {

    @NotNull
    static public JsExpression translate(@NotNull JetBinaryExpression expression,
                                         @NotNull TranslationContext context) {
        return (new BinaryOperationTranslator(expression, context)).translate();
    }

    @NotNull
    private final JetBinaryExpression expression;

    private BinaryOperationTranslator(@NotNull JetBinaryExpression expression,
                                      @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
    }

    @NotNull
    JsExpression translate() {
        JsInvocation setterCall = translateAsSetterCall();
        if (setterCall != null) {
            return setterCall;
        }
        return translateAsBinaryOperation();
    }

    //TODO: method too long
    @Nullable
    public JsInvocation translateAsSetterCall() {
        JetToken jetOperationToken = getOperationToken();
        if (!OperatorTable.isAssignment(jetOperationToken)) {
            return null;
        }
        JetExpression leftExpression = expression.getLeft();
        PropertyAccessTranslator propertyAccessTranslator = Translation.propertyAccessTranslator(translationContext());
        if (!propertyAccessTranslator.canBePropertySetterCall(leftExpression)) {
            return null;
        }
        JsInvocation setterCall = propertyAccessTranslator.translateAsPropertySetterCall(leftExpression);
        JsExpression right = translateRightExpression();
        setterCall.setArguments(Arrays.asList(right));
        return setterCall;
    }

    @NotNull
    private JsExpression translateAsBinaryOperation() {

        JsExpression left = Translation.translateAsExpression(expression.getLeft(), translationContext());
        JsExpression right = translateRightExpression();

        JsNameRef operationReference = getOverloadedOperationReference(expression);
        if (operationReference != null) {
            return overloadedMethodInvocation(left, right, operationReference);
        }
        JetToken token = getOperationToken();
        if (OperatorTable.hasCorrespondingBinaryOperator(token)) {
            return new JsBinaryOperation(OperatorTable.getBinaryOperator(token), left, right);
        }
        if (OperatorTable.hasCorrespondingFunctionInvocation(token)) {
            JsInvocation functionInvocation = OperatorTable.getCorrespondingFunctionInvocation(token);
            functionInvocation.setArguments(Arrays.asList(left, right));
            return functionInvocation;
        }
        throw new AssertionError("Unsupported token encountered: " + token.toString());
    }

    @NotNull
    private JsExpression overloadedMethodInvocation(@NotNull JsExpression left, @NotNull JsExpression right,
                                                    @NotNull JsNameRef operationReference) {
        AstUtil.setQualifier(operationReference, left);
        return AstUtil.newInvocation(operationReference, right);
    }

    @NotNull
    private JsExpression translateRightExpression() {
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Binary expression should have a right expression";
        return Translation.translateAsExpression(rightExpression, translationContext());
    }

    @NotNull
    private JetToken getOperationToken() {
        return (JetToken) expression.getOperationToken();
    }


}
