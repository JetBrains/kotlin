package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDeclaration;

/**
 * @author Talanov Pavel
 */
public final class NamespaceDeclarationTranslator extends AbstractTranslator {

    private NamespaceDeclarationVisitor visitor = new NamespaceDeclarationVisitor();

    @NotNull
    public static NamespaceDeclarationTranslator newInstance(@NotNull TranslationContext context) {
        return new NamespaceDeclarationTranslator(context);
    }

    private NamespaceDeclarationTranslator(TranslationContext context) {
        super(context);
    }

    @NotNull
    JsStatement translateDeclaration(JetDeclaration declaration) {
        return declaration.accept(visitor, translationContext());
    }


}
