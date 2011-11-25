package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
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
        this.operationReference = getOverloadedOperationReference(expression.getOperationReference());
        this.right = translateRightExpression();
        //TODO: decide whether it is harmful to possibly translateNamespace left expression more than once
        this.left = translateLeftExpression();
    }

    @NotNull
    JsExpression translate() {
        if (isCompareTo()) {
            return asCompareToOverload();
        }
        if (isOverloadedCall()) {
            return asOverloadedMethodCall();
        }
        return asBinaryOperation();
    }

    @NotNull
    private JsExpression asCompareToOverload() {
        JetToken operationToken = getOperationToken();
        assert (OperatorConventions.COMPARISON_OPERATIONS.contains(operationToken));
        JsNumberLiteral zetoLiteral = program().getNumberLiteral(0);
        JsBinaryOperator correspondingOperator = OperatorTable.getBinaryOperator(operationToken);
        return new JsBinaryOperation(correspondingOperator, overloadedMethodInvocation(), zetoLiteral);
    }

    private boolean isOverloadedCall() {
        return operationReference != null;
    }

    private boolean isCompareTo() {
        if (operationReference == null) {
            return false;
        }
        String nameForOperationSymbol = OperatorConventions.getNameForOperationSymbol(getOperationToken());
        assert nameForOperationSymbol != null : "Must have a name for overloaded operator";
        return (nameForOperationSymbol.equals("compareTo"));
    }

    @NotNull
    private JsExpression asOverloadedMethodCall() {
        if (isPropertyOnTheLeft) {
            return overloadOnProperty();
        }
        if (isVariableReassignment) {
            return nonPropertyReassignment();
        }
        return overloadedMethodInvocation();
    }

    private JsExpression nonPropertyReassignment() {
        assert left instanceof JsNameRef : "Reassignment should be called on l-value.";
        return AstUtil.newAssignment((JsNameRef) left, overloadedMethodInvocation());
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
