package org.jetbrains.k2js.translate.general;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetVisitor;
import org.jetbrains.k2js.translate.context.TranslationContext;

/**
 * @author Pavel Talanov
 *         <p/>
 *         This class is a base class for all visitors.
 */
public class TranslatorVisitor<T> extends JetVisitor<T, TranslationContext> {

    @Override
    @NotNull
    public T visitJetElement(JetElement expression, TranslationContext context) {
        throw new UnsupportedOperationException("Unsupported expression encountered:" + expression.toString());
    }

}
