package org.jetbrains.k2js.translate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetVisitor;

/**
 * @author Talanov Pavel
 */
public class K2JsVisitor<T> extends JetVisitor<T, TranslationContext> {

    @Override
    @NotNull
    public T visitJetElement(JetElement expression, TranslationContext context) {
        throw new RuntimeException("Unexpected expression encountered:" + expression.toString());
    }
}
