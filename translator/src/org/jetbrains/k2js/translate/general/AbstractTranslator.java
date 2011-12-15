package org.jetbrains.k2js.translate.general;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.translate.context.TranslationContext;

/**
 * @author Pavel Talanov
 */
//TODO: provide helper methods for different parts of context
public abstract class AbstractTranslator {

    @NotNull
    private final TranslationContext context;

    protected AbstractTranslator(@NotNull TranslationContext context) {
        this.context = context;
    }

    @NotNull
    protected JsProgram program() {
        return context.program();
    }

    @NotNull
    protected TranslationContext context() {
        return context;
    }
}
