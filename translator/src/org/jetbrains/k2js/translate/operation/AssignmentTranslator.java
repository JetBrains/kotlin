package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.reference.AccessTranslator;

import static org.jetbrains.k2js.translate.utils.BindingUtils.isVariableReassignment;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.k2js.translate.utils.PsiUtils.isAssignment;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.isIntrinsicOperation;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateRightExpression;

/**
 * @author Pavel Talanov
 */
public abstract class AssignmentTranslator extends AbstractTranslator {

    public static boolean isAssignmentOperator(JetBinaryExpression expression) {
        JetToken operationToken = getOperationToken(expression);
        return (OperatorConventions.ASSIGNMENT_OPERATIONS.keySet().contains(operationToken)
                || isAssignment(operationToken));
    }

    @NotNull
    public static JsExpression translate(@NotNull JetBinaryExpression expression,
                                         @NotNull TranslationContext context) {
        if (isIntrinsicOperation(context, expression)) {
            return IntrinsicAssignmentTranslator.translate(expression, context);
        }
        return OverloadedAssignmentTranslator.translate(expression, context);
    }

    @NotNull
    protected final JetBinaryExpression expression;
    protected final AccessTranslator accessTranslator;
    protected final boolean isVariableReassignment;
    @NotNull
    protected final JsExpression right;

    protected AssignmentTranslator(@NotNull JetBinaryExpression expression,
                                   @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.isVariableReassignment = isVariableReassignment(context.bindingContext(), expression);
        this.accessTranslator = AccessTranslator.getAccessTranslator(expression.getLeft(), context());
        this.right = translateRightExpression(context(), expression);
    }
}
