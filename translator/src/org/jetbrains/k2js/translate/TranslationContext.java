package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.BindingContext;

/**
 * @author Talanov Pavel
 */
public final class TranslationContext {


    private static class Scopes {
        public Scopes(@NotNull JsScope enclosingScope, @NotNull JsScope functionScope, @NotNull JsScope namespaceScope) {
            this.enclosingScope = enclosingScope;
            this.classScope = functionScope;
            this.namespaceScope = namespaceScope;
        }

        @NotNull public final JsScope enclosingScope;
        @NotNull public final JsScope classScope;
        @NotNull public final JsScope namespaceScope;
    }

    @NotNull
    public static TranslationContext rootContext(JsProgram program, BindingContext bindingContext) {
        JsScope rootScope = program.getRootScope();
        Scopes scopes = new Scopes(rootScope, rootScope, rootScope);
        return new TranslationContext(null,
                program, bindingContext, scopes);
    }

    @NotNull private final JsProgram program;
    @NotNull private final BindingContext bindingContext;
    @NotNull private final Scopes scopes;
    @Nullable private final JsName currentNamespace;



    private TranslationContext(@Nullable JsName currentNamespace, @NotNull JsProgram program,
                               @NotNull BindingContext bindingContext, @NotNull Scopes scopes) {
        this.program = program;
        this.bindingContext = bindingContext;
        this.currentNamespace = currentNamespace;
        this.scopes = scopes;
    }

    @NotNull
    public TranslationContext newNamespace(@NotNull JsName namespaceName, @NotNull JsFunction namespaceDummyFunction) {
        JsScope newScope = namespaceDummyFunction.getScope();
        Scopes newScopes = new Scopes(newScope, newScope, newScope);
        return new TranslationContext(namespaceName, program,
                bindingContext, newScopes);
    }

    @NotNull
    public TranslationContext newBlock() {
        Scopes newScopes = new Scopes(new JsScope
                (scopes.enclosingScope, "dummy enclosingScope for a block"), scopes.classScope, scopes.namespaceScope);
        return new TranslationContext(currentNamespace, program,
                bindingContext, newScopes);
    }

    //TODO new Class?
    @NotNull
    public TranslationContext newFunction(@NotNull JsFunction function) {
        JsScope functionScope = function.getScope();
        Scopes newScopes = new Scopes(functionScope, functionScope, scopes.namespaceScope);
        return new TranslationContext(currentNamespace, program,
                bindingContext, newScopes);
    }

    @NotNull
    public JsNameRef getNamespaceQualifiedReference(JsName name) {
        if (currentNamespace != null) {
            return AstUtil.newNameRef(currentNamespace.makeRef(), name);
        }
        return new JsNameRef(name);
    }

    @NotNull
    public BindingContext bindingContext() {
        return bindingContext;
    }

    @NotNull
    public JsProgram program() {
        return program;
    }

    @NotNull
    JsScope enclosingScope() {
        return scopes.enclosingScope;
    }

    @NotNull
    JsScope namespaceScope() {
        return scopes.namespaceScope;
    }

    @NotNull
    JsScope classScope() {
        return scopes.classScope;
    }

    @NotNull
    JsName declareLocalName(@NotNull String name) {
        return scopes.enclosingScope.declareFreshName(name);
    }
}
