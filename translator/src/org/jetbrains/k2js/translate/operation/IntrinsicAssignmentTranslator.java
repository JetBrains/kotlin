package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;

/**
 * @author Talanov Pavel
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
    @Override
    protected JsExpression translate() {
        return asBinaryOperation();
    }

    //TODO: refactor
    @NotNull
    private JsExpression asBinaryOperation() {
        if (OperatorTable.isAssignment(getOperationToken(expression))) {
            return accessTranslator.translateAsSet(right);
        }
        //TODO: logic broken
        JetToken token = getOperationToken(expression);
        if (OperatorTable.hasCorrespondingBinaryOperator(token)) {
            return new JsBinaryOperation(OperatorTable.getBinaryOperator(token),
                    accessTranslator.translateAsGet(), right);
        }
        throw new AssertionError("Unsupported token encountered: " + token.toString());
    }

}
