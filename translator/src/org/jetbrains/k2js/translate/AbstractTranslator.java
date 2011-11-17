package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import org.jetbrains.annotations.NotNull;

/**
 * @author Talanov Pavel
 */
public abstract class AbstractTranslator {

    @NotNull
    private final TranslationContext context;

    public AbstractTranslator(@NotNull TranslationContext context) {
        this.context = context;
    }

    @NotNull
    protected JsProgram program() {
        return context.program();
    }

    @NotNull
    protected TranslationContext translationContext() {
        return context;
    }
}
