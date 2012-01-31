package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.JsConditional;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNullLiteral;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.reference.AccessTranslator;
import org.jetbrains.k2js.translate.reference.CallTranslator;
import org.jetbrains.k2js.translate.reference.PropertyAccessTranslator;

import static org.jetbrains.k2js.translate.general.Translation.translateAsExpression;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getNotNullSimpleNameSelector;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getSelector;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.notNullCheck;

/**
 * @author Pavel Talanov
 */
public final class QualifiedExpressionTranslator {

    private QualifiedExpressionTranslator() {
    }

    @NotNull
    public static AccessTranslator getAccessTranslator(@NotNull JetQualifiedExpression expression,
                                                       @NotNull TranslationContext context) {
        if (expression instanceof JetDotQualifiedExpression) {
            JsExpression receiver = translateReceiver(expression, context);
            return PropertyAccessTranslator.newInstance(getNotNullSimpleNameSelector(expression), receiver, context);
        }
        throw new UnsupportedOperationException();
    }

    @NotNull
    public static JsExpression translateSafeQualifiedExpression(@NotNull JetSafeQualifiedExpression expression,
                                                                @NotNull TranslationContext context) {
        TemporaryVariable receiver = context.declareTemporary(translateReceiver(expression, context));
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        JetExpression selector = getSelector(expression);
        JsExpression thenExpression = translate(receiver.nameReference(), selector, context);
        JsConditional callMethodIfNotNullConditional
                = new JsConditional(notNullCheck(context, receiver.nameReference()), thenExpression, nullLiteral);
        return AstUtil.newSequence(receiver.assignmentExpression(), callMethodIfNotNullConditional);
    }

    @NotNull
    public static JsExpression translateDotQualifiedExpression(@NotNull JetDotQualifiedExpression expression,
                                                               @NotNull TranslationContext context) {
        JsExpression receiver = translateReceiver(expression, context);
        JetExpression selector = getSelector(expression);
        return translate(receiver, selector, context);
    }

    @NotNull
    private static JsExpression translate(@NotNull JsExpression receiver, @NotNull JetExpression selector,
                                          @NotNull TranslationContext context) {
        if (PropertyAccessTranslator.canBePropertyGetterCall(selector, context)) {
            return PropertyAccessTranslator.translateAsPropertyGetterCall
                    ((JetSimpleNameExpression) selector, receiver, context);
        }
        if (selector instanceof JetCallExpression) {
            return CallTranslator.translate((JetCallExpression) selector, receiver, context);
        }
        throw new AssertionError("Unexpected qualified expression");
    }

    @NotNull
    private static JsExpression translateReceiver(@NotNull JetQualifiedExpression expression,
                                                  @NotNull TranslationContext context) {
        return translateAsExpression(expression.getReceiverExpression(), context);
    }
}
