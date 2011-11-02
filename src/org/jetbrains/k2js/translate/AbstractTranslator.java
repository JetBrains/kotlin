package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsBlock;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingContext;

/**
 * @author Talanov Pavel
 */
public abstract class AbstractTranslator {

    private final TranslationContext context;

    public AbstractTranslator(TranslationContext context) {
        assert context != null;
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

    @NotNull
    protected BindingContext bindingContext() {
        return context.bindingContext();
    }

    @NotNull
    protected JsName getJSName(String jetName) {
        //TODO dummy implemetation
        return new JsName(program().getScope(), jetName, jetName, jetName);
    }

    @NotNull
    protected JsScope scope() {
        return context.scope();
    }

    @NotNull
    protected JsBlock block() {
        return context.block();
    }
}
