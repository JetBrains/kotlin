package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;

/**
 * @author Talanov Pavel
 */
public final class OverloadedAssignmentTranslator extends AssignmentTranslator {

    @NotNull
    public static JsExpression translate(@NotNull JetBinaryExpression expression,
                                         @NotNull TranslationContext context) {
        return (new OverloadedAssignmentTranslator(expression, context)).translate();
    }

    @NotNull
    private final JsNameRef operationReference;

    private OverloadedAssignmentTranslator(@NotNull JetBinaryExpression expression,
                                           @NotNull TranslationContext context) {
        super(expression, context);
        //TODO: util
        DeclarationDescriptor overloadedOperationDescriptor = getDescriptorForReferenceExpression
                (context.bindingContext(), expression.getOperation());
        assert overloadedOperationDescriptor != null;
        JsNameRef overloadedOperationReference = context().getNameForDescriptor(overloadedOperationDescriptor).makeRef();
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
        JetToken operationToken = getOperationToken(expression);
        assert (OperatorConventions.COMPARISON_OPERATIONS.contains(operationToken));
        JsNumberLiteral zeroLiteral = program().getNumberLiteral(0);
        JsBinaryOperator correspondingOperator = OperatorTable.getBinaryOperator(operationToken);
        return new JsBinaryOperation(correspondingOperator, overloadedMethodInvocation(), zeroLiteral);
    }

    private boolean isCompareTo() {
        //util
        JetToken operationToken = getOperationToken(expression);
        String nameForOperationSymbol = OperatorConventions.getNameForOperationSymbol(operationToken);
        assert nameForOperationSymbol != null : "Must have a name for overloaded operator";
        return (nameForOperationSymbol.equals("compareTo"));
    }

    @NotNull
    private JsExpression asOverloadedMethodCall() {
        if (isVariableReassignment) {
            return reassignment();
        }
        return overloadedMethodInvocation();
    }

    @NotNull
    private JsExpression reassignment() {
        return accessTranslator.translateAsSet(overloadedMethodInvocation());
    }

    @NotNull
    private JsExpression overloadedMethodInvocation() {
        AstUtil.setQualifier(operationReference, accessTranslator.translateAsGet());
        return AstUtil.newInvocation(operationReference, right);
    }

}
