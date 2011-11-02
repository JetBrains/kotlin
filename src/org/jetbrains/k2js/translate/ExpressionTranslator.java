package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import groovyjarjarantlr.collections.AST;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lexer.JetToken;

import java.util.List;

/**
 * Talanov Pavel
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
