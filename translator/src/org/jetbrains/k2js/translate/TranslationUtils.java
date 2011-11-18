package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNullLiteral;
import org.jetbrains.annotations.NotNull;

/**
 * @author Talanov Pavel
 */
public final class TranslationUtils {

    @NotNull
    static public JsBinaryOperation notNullCheck(@NotNull JsExpression expressionToCheck,
                                                 @NotNull TranslationContext context) {
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        return new JsBinaryOperation
                (JsBinaryOperator.NEQ, expressionToCheck, nullLiteral);
    }

    @NotNull
    static public JsBinaryOperation isNullCheck(@NotNull JsExpression expressionToCheck,
                                                @NotNull TranslationContext context) {
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        return new JsBinaryOperation
                (JsBinaryOperator.REF_EQ, expressionToCheck, nullLiteral);
    }

}
