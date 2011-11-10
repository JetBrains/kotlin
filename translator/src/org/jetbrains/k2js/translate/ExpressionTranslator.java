package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

/**
 * @author Talanov Pavel
 */
public final class ExpressionTranslator extends AbstractTranslator {

    final private ExpressionVisitor visitor = new ExpressionVisitor(translationContext());

    public ExpressionTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @NotNull
    public JsNode translate(@NotNull JetExpression jetExpression) {
        return jetExpression.accept(visitor, translationContext());
    }
}
