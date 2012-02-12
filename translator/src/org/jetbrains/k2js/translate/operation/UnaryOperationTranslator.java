package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.reference.CallBuilder;
import org.jetbrains.k2js.translate.reference.CallType;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.Collections;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getResolvedCall;

/**
 * @author Pavel Talanov
 */

public final class UnaryOperationTranslator {

    @NotNull
    public static JsExpression translate(@NotNull JetUnaryExpression expression,
                                         @NotNull TranslationContext context) {
        if (IncrementTranslator.isIncrement(expression)) {
            return IncrementTranslator.translate(expression, context);
        }
        return translateAsCall(expression, context);
    }

    @NotNull
    private static JsExpression translateAsCall(@NotNull JetUnaryExpression expression,
                                                @NotNull TranslationContext context) {
        return CallBuilder.build(context)
                .receiver(TranslationUtils.translateBaseExpression(context, expression))
                .args(Collections.<JsExpression>emptyList())
                .resolvedCall(getResolvedCall(context.bindingContext(), expression.getOperationReference()))
                .type(CallType.NORMAL).translate();
    }
}
