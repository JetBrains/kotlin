package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.reference.CallTranslator;

/**
 * @author Talanov Pavel
 */

//TODO: merge with binary operation
public final class UnaryOperationTranslator {

    @NotNull
    public static JsExpression translate(@NotNull JetUnaryExpression expression,
                                         @NotNull TranslationContext context) {
        if (IncrementTranslator.isIncrement(expression)) {
            return IncrementTranslator.translate(expression, context);
        }
        return CallTranslator.translate(expression, context);
    }
}
