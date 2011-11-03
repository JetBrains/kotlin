package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

/**
 * @author Talanov Pavel
 */
class ExpressionTranslator extends AbstractTranslator {

    final private ExpressionVisitor visitor = new ExpressionVisitor();

    public ExpressionTranslator(TranslationContext context) {
        super(context);
    }

    @NotNull
    public JsNode translate(JetExpression jetExpression) {
        assert jetExpression != null;
        return jetExpression.accept(visitor, translationContext());

    }
}
