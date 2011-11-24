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
    private final boolean isPropertyOnTheLeft;
    private final boolean isVariableReassignment;
    @NotNull
    private final JsExpression left;
    @NotNull
    private final JsExpression right;
    @Nullable
    private final JsNameRef operationReference;

    private BinaryOperationTranslator(@NotNull JetBinaryExpression expression,
                                      @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.isPropertyOnTheLeft = isPropertyAccess(expression.getLeft());
        this.isVariableReassignment = isVariableReassignment(expression);
        this.operationReference = getOverloadedOperationReference(expression);
        this.right = translateRightExpression();
        //TODO: decide whether it is harmful to possibly translate left expression more than once
        this.left = translateLeftExpression();
    }

    @NotNull
    JsExpression translate() {
        if (operationReference != null) {
            return asOverloadedMethodCall();
        }
        return asBinaryOperation();
    }

    @NotNull
    private JsExpression asOverloadedMethodCall() {
        if (isPropertyOnTheLeft) {
            return overloadOnProperty();
        }
        return overloadedMethodInvocation();
    }

    @NotNull
    private JsExpression overloadOnProperty() {
        if (isVariableReassignment) {
            return setterCall(overloadedMethodInvocation());
        } else {
            return overloadedMethodInvocation();
        }
    }

    @NotNull
    private JsExpression asBinaryOperation() {
        if (isPropertyOnTheLeft && OperatorTable.isAssignment(getOperationToken())) {
            return setterCall(right);
        }
        JetToken token = getOperationToken();
        if (OperatorTable.hasCorrespondingBinaryOperator(token)) {
            return new JsBinaryOperation(OperatorTable.getBinaryOperator(token), left, right);
        }
        if (OperatorTable.hasCorrespondingFunctionInvocation(token)) {
            JsInvocation functionInvocation = OperatorTable.getCorrespondingFunctionInvocation(token);
            functionInvocation.setArguments(left, right);
            return functionInvocation;
        }
        throw new AssertionError("Unsupported token encountered: " + token.toString());
    }

    private JsExpression translateLeftExpression() {
        return Translation.translateAsExpression(expression.getLeft(), translationContext());
    }

    @NotNull
    private JsExpression overloadedMethodInvocation() {
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

    @NotNull
    public JsInvocation setterCall(@NotNull JsExpression assignTo) {
        PropertyAccessTranslator propertyAccessTranslator = Translation.propertyAccessTranslator(translationContext());
        JsInvocation setterCall = propertyAccessTranslator.translateAsPropertySetterCall(expression.getLeft());
        setterCall.setArguments(assignTo);
        return setterCall;
    }


}
