package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetToken;

/**
 * @author Talanov Pavel
 */
public final class UnaryOperationTranslator extends OperationTranslator {


    @NotNull
    static public JsExpression translate(@NotNull JetPrefixExpression expression,
                                         @NotNull TranslationContext context) {
        return (new UnaryOperationTranslator(context, expression, true)).translate();
    }

    @NotNull
    static public JsExpression translate(@NotNull JetPostfixExpression expression,
                                         @NotNull TranslationContext context) {
        return (new UnaryOperationTranslator(context, expression, false)).translate();
    }

    @NotNull
    private final JetUnaryExpression expression;
    private final boolean isPrefix;
    private final boolean isVariableReassignment;

    private UnaryOperationTranslator(@NotNull TranslationContext context, @NotNull JetUnaryExpression expression,
                                     boolean isPrefix) {
        super(context);
        this.expression = expression;
        this.isPrefix = isPrefix;
        this.isVariableReassignment = BindingUtils.isVariableReassignment
                (translationContext().bindingContext(), expression);
    }

    @NotNull
    JsExpression translate() {
        JsExpression baseExpression = translateBaseExpression();
        JsNameRef operationReference = getOverloadedOperationReference(expression);
        if (operationReference != null) {
            if (isPrefix && !isVariableReassignment) {
                return overloadedMethodInvocation(baseExpression, operationReference);
            } else if (isPrefix) {

            }
        }
        return jsUnaryExpression(baseExpression);
    }

    @NotNull
    private JsExpression jsUnaryExpression(@NotNull JsExpression baseExpression) {
        JsUnaryOperator operator = OperatorTable.getUnaryOperator(getOperationToken());
        if (isPrefix) {
            return new JsPrefixOperation(operator, baseExpression);
        } else {
            return new JsPostfixOperation(operator, baseExpression);
        }
    }

    @NotNull
    private JsExpression translateBaseExpression() {
        JetExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null : "Unary expression should have a base expression";
        return Translation.translateAsExpression(baseExpression, translationContext());
    }

    @NotNull
    private JsExpression overloadedMethodInvocation(@NotNull JsExpression receiver,
                                                    @NotNull JsNameRef operationReference) {
        AstUtil.setQualifier(operationReference, receiver);
        return AstUtil.newInvocation(operationReference);
    }

    @NotNull
    private JetToken getOperationToken() {
        JetSimpleNameExpression operationExpression = expression.getOperationSign();
        IElementType elementType = operationExpression.getReferencedNameElementType();
        assert elementType instanceof JetToken : "Unary expression should have IElementType of type JetToken";
        return (JetToken) elementType;
    }
}
