package org.jetbrains.k2js.intrinsic;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.operation.IntrinsicBinaryOperationTranslator;

/**
 * @author Talanov Pavel
 */
public enum BinaryOperationIntrinsic implements Intrinsic {

    INSTANCE;

    /*package*/ BinaryOperationIntrinsic() {
    }

    @NotNull
    @Override
    public JsExpression apply(@NotNull JetExpression expression,
                              @NotNull TranslationContext context) {
        assert expression instanceof JetBinaryExpression : "This intrinsic must be applied to binary operations.";
        return IntrinsicBinaryOperationTranslator.translate((JetBinaryExpression) expression, context);
    }
}
