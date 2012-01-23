package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.reference.CallTranslator;
import org.jetbrains.k2js.translate.reference.PropertyAccessTranslator;

import static org.jetbrains.k2js.translate.general.Translation.translateAsExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.notNullCheck;

/**
 * @author Pavel Talanov
 */
public final class QualifiedExpressionTranslator {

    private QualifiedExpressionTranslator() {
    }

    @NotNull
    public static JsExpression translateSafeQualifiedExpression(@NotNull JetSafeQualifiedExpression expression,
                                                                @NotNull TranslationContext context) {
        TemporaryVariable receiver = context.declareTemporary(translateReceiver(expression, context));
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        JsExpression selector = translateSelector(expression, context);
        JsExpression thenExpression = composeQualifiedExpression(receiver.nameReference(), selector);
        JsConditional callMethodIfNotNullConditional
                = new JsConditional(notNullCheck(context, receiver.nameReference()), thenExpression, nullLiteral);
        return AstUtil.newSequence(receiver.assignmentExpression(), callMethodIfNotNullConditional);
    }

    @NotNull
    public static JsExpression translateDotQualifiedExpression(@NotNull JetDotQualifiedExpression expression,
                                                               @NotNull TranslationContext context) {
        //TODO: problem with extension properties lies here
        //TODO: problem with extension properties lies here
        if (PropertyAccessTranslator.canBePropertyGetterCall(expression, context)) {
            return PropertyAccessTranslator.translateAsPropertyGetterCall(expression, context);
        }
        if (expression.getSelectorExpression() instanceof JetCallExpression) {
            return CallTranslator.translate(expression, context);
        }
        JsExpression receiver = translateReceiver(expression, context);
        JsExpression selector = translateSelector(expression, context);
        return composeQualifiedExpression(receiver, selector);
    }

    @NotNull
    private static JsExpression composeQualifiedExpression(@NotNull JsExpression receiver, @NotNull JsExpression selector) {
        //TODO: make sure that logic would not break for binary operation. check if there is a way to provide clearer logic
        assert (selector instanceof JsNameRef || selector instanceof JsInvocation || selector instanceof JsBinaryOperation)
                : "Selector should be a name reference or a method invocation in dot qualified expression.";
        if (selector instanceof JsInvocation) {
            return translateAsQualifiedInvocation(receiver, (JsInvocation) selector);
        } else if (selector instanceof JsNameRef) {
            return translateAsQualifiedNameReference(receiver, (JsNameRef) selector);
        } else {
            ((JsBinaryOperation) selector).setArg1(receiver);
            return selector;
        }
    }

    @NotNull
    private static JsExpression translateSelector(@NotNull JetQualifiedExpression expression,
                                                  @NotNull TranslationContext context) {
        JetExpression jetSelector = expression.getSelectorExpression();
        assert jetSelector != null : "Selector should not be null in dot qualified expression.";
        return translateAsExpression(jetSelector, context);
    }

    @NotNull
    private static JsExpression translateReceiver(@NotNull JetQualifiedExpression expression,
                                                  @NotNull TranslationContext context) {
        return translateAsExpression(expression.getReceiverExpression(), context);
    }

    @NotNull
    private static JsExpression translateAsQualifiedNameReference(@NotNull JsExpression receiver, @NotNull JsNameRef selector) {
        selector.setQualifier(receiver);
        return selector;
    }

    @NotNull
    private static JsExpression translateAsQualifiedInvocation(@NotNull JsExpression receiver, @NotNull JsInvocation selector) {
        JsExpression qualifier = selector.getQualifier();
        JsNameRef nameRef = (JsNameRef) qualifier;
        nameRef.setQualifier(receiver);
        return selector;
    }
}
