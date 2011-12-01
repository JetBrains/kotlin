package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.general.TranslationContext;

/**
 * @author Talanov Pavel
 */
public final class OverloadedBinaryOperationTranslator extends BinaryOperationTranslator {

    @NotNull
    public static JsExpression translate(@NotNull JetBinaryExpression expression,
                                         @NotNull TranslationContext context) {
        return (new OverloadedBinaryOperationTranslator(expression, context)).translate();
    }

    @NotNull
    private final JsNameRef operationReference;

    private OverloadedBinaryOperationTranslator(@NotNull JetBinaryExpression expression,
                                                @NotNull TranslationContext context) {
        super(expression, context);
        JsNameRef overloadedOperationReference = getOverloadedOperationReference(expression, context);
        assert overloadedOperationReference != null;
        this.operationReference = overloadedOperationReference;
    }

    @Override
    @NotNull
    protected JsExpression translate() {
        if (isCompareTo()) {
            return asCompareToOverload();
        }
        return asOverloadedMethodCall();
    }

    @NotNull
    private JsExpression asCompareToOverload() {
        JetToken operationToken = getOperationToken();
        assert (OperatorConventions.COMPARISON_OPERATIONS.contains(operationToken));
        JsNumberLiteral zeroLiteral = program().getNumberLiteral(0);
        JsBinaryOperator correspondingOperator = OperatorTable.getBinaryOperator(operationToken);
        return new JsBinaryOperation(correspondingOperator, overloadedMethodInvocation(), zeroLiteral);
    }

    private boolean isCompareTo() {
        String nameForOperationSymbol = OperatorConventions.getNameForOperationSymbol(getOperationToken());
        assert nameForOperationSymbol != null : "Must have a name for overloaded operator";
        return (nameForOperationSymbol.equals("compareTo"));
    }

    @NotNull
    private JsExpression asOverloadedMethodCall() {
        if (isPropertyOnTheLeft && isVariableReassignment) {
            return propertyReassignment();
        }
        if (isVariableReassignment) {
            return nonPropertyReassignment();
        }
        return overloadedMethodInvocation();
    }

    @NotNull
    private JsInvocation propertyReassignment() {
        return setterCall(overloadedMethodInvocation());
    }

    @NotNull
    private JsExpression nonPropertyReassignment() {
        assert left instanceof JsNameRef : "Reassignment should be called on l-value.";
        return AstUtil.newAssignment((JsNameRef) left, overloadedMethodInvocation());
    }

    @NotNull
    private JsExpression overloadedMethodInvocation() {
        AstUtil.setQualifier(operationReference, left);
        return AstUtil.newInvocation(operationReference, right);
    }

}
