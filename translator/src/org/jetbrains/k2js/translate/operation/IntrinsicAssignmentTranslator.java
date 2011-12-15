package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.reference.ReferenceAccessTranslator;

import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.k2js.translate.utils.PsiUtils.isAssignment;

/**
 * @author Pavel Talanov
 */
public final class IntrinsicAssignmentTranslator extends AssignmentTranslator {


    @NotNull
    public static JsExpression translate(@NotNull JetBinaryExpression expression,
                                         @NotNull TranslationContext context) {
        return (new IntrinsicAssignmentTranslator(expression, context)).translate();
    }

    private IntrinsicAssignmentTranslator(@NotNull JetBinaryExpression expression,
                                          @NotNull TranslationContext context) {
        super(expression, context);
    }

    @NotNull
    protected JsExpression translate() {
        if (isAssignment(getOperationToken(expression))) {
            return translateAsPlainAssignment();
        }
        return translateAsAssignmentOperation();
    }

    @NotNull
    private JsExpression translateAsAssignmentOperation() {
        if (accessTranslator instanceof ReferenceAccessTranslator) {
            return translateAsPlainAssignmentOperation();
        }
        return translateAsAssignToCounterpart();
    }

    @NotNull
    private JsExpression translateAsAssignToCounterpart() {
        JsBinaryOperator operator = getCounterpartOperator();
        JsBinaryOperation counterpartOperation =
                new JsBinaryOperation(operator, accessTranslator.translateAsGet(), right);
        return accessTranslator.translateAsSet(counterpartOperation);
    }

    @NotNull
    private JsBinaryOperator getCounterpartOperator() {
        JetToken assignmentOperationToken = getOperationToken(expression);
        assert OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(assignmentOperationToken);
        JetToken counterpartToken = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(assignmentOperationToken);
        assert OperatorTable.hasCorrespondingBinaryOperator(counterpartToken) :
                "Unsupported token encountered: " + counterpartToken.toString();
        return OperatorTable.getBinaryOperator(counterpartToken);
    }

    @NotNull
    private JsExpression translateAsPlainAssignmentOperation() {
        JsBinaryOperator operator = getAssignmentOperator();
        return new JsBinaryOperation(operator, accessTranslator.translateAsGet(), right);
    }

    @NotNull
    private JsBinaryOperator getAssignmentOperator() {
        JetToken token = getOperationToken(expression);
        assert OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(token);
        assert OperatorTable.hasCorrespondingBinaryOperator(token) :
                "Unsupported token encountered: " + token.toString();
        return OperatorTable.getBinaryOperator(token);
    }

    @NotNull
    private JsExpression translateAsPlainAssignment() {
        return accessTranslator.translateAsSet(right);
    }
}
