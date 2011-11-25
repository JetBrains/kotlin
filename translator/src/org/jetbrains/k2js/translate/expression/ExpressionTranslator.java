package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.JsNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.TranslationContext;

/**
 * @author Talanov Pavel
 */
public final class ExpressionTranslator extends AbstractTranslator {

    final private ExpressionVisitor visitor = new ExpressionVisitor();

    @NotNull
    public static ExpressionTranslator newInstance(@NotNull TranslationContext context) {
        return new ExpressionTranslator(context);
    }

    private ExpressionTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @NotNull
    public JsNode translate(@NotNull JetExpression jetExpression) {
        return jetExpression.accept(visitor, context());
    }
}
