package org.jetbrains.k2js.translate.general;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetVisitor;

/**
 * @author Talanov Pavel
 *         <p/>
 *         This class is a base class for all visitors. It contains code that is shared among them.
 */
public class TranslatorVisitor<T> extends JetVisitor<T, TranslationContext> {

    @Override
    @NotNull
    public T visitJetElement(JetElement expression, TranslationContext context) {
        throw new RuntimeException("Unsupported expression encountered:" + expression.toString());
    }

    @Nullable
    protected JsExpression translateInitializerForProperty(@NotNull JetProperty declaration,
                                                           @NotNull TranslationContext context) {
        JsExpression jsInitExpression = null;
        JetExpression initializer = declaration.getInitializer();
        if (initializer != null) {
            jsInitExpression = Translation.translateAsExpression(initializer, context);
        }
        return jsInitExpression;
    }
}
