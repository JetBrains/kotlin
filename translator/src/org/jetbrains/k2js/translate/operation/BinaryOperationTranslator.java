package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.reference.PropertyAccessTranslator;

/**
 * @author Talanov Pavel
 */
public abstract class BinaryOperationTranslator extends OperationTranslator {

    @NotNull
    private final JetBinaryExpression expression;
    protected final boolean isPropertyOnTheLeft;
    protected final boolean isVariableReassignment;
    @NotNull
    protected final JsExpression left;
    @NotNull
    protected final JsExpression right;

    protected BinaryOperationTranslator(@NotNull JetBinaryExpression expression,
                                        @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.isPropertyOnTheLeft = isPropertyAccess(expression.getLeft());
        this.isVariableReassignment = isVariableReassignment(expression);

        this.right = translateRightExpression();
        //TODO: decide whether it is harmful to possibly translateNamespace left expression more than once
        this.left = translateLeftExpression();
    }

    @NotNull
    abstract protected JsExpression translate();

    private JsExpression translateLeftExpression() {
        return Translation.translateAsExpression(expression.getLeft(), context());
    }

    @NotNull
    private JsExpression translateRightExpression() {
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Binary expression should have a right expression";
        return Translation.translateAsExpression(rightExpression, context());
    }

    @NotNull
    protected JetToken getOperationToken() {
        return (JetToken) expression.getOperationToken();
    }

    @NotNull
    protected JsInvocation setterCall(@NotNull JsExpression assignTo) {
        JsInvocation setterCall =
                PropertyAccessTranslator.translateAsPropertySetterCall(expression.getLeft(), context());
        setterCall.setArguments(assignTo);
        return setterCall;
    }

}
