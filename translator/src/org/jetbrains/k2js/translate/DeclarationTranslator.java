package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDeclaration;

/**
 * @author Talanov Pavel
 */
public final class DeclarationTranslator extends AbstractTranslator {

    private DeclarationVisitor visitor = new DeclarationVisitor();

    public DeclarationTranslator(TranslationContext context) {
        super(context);
    }

    @NotNull
    JsStatement translateDeclaration(JetDeclaration declaration) {
        return declaration.accept(visitor, translationContext());
    }




}
