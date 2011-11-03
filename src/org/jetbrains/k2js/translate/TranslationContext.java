package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.apache.velocity.runtime.directive.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.awt.peer.ScrollbarPeer;

/**
 * @author Talanov Pavel
 */
public class TranslationContext {

    private final JsProgram program;
    private final BindingContext bindingContext;
    private final ContextType type;
    private final Scopes scopes;
    private final JsName currentNamespace;


    private static class Scopes {

        public Scopes(JsScope enclosing, JsScope function, JsScope namespace) {
            this.enclosingScope = enclosing;
            this.functionScope = function;
            this.namespaceScope = function;
        }

        public Scopes(Scopes other) {
            this(other.enclosingScope, other.functionScope, other.namespaceScope);
        }

        public final JsScope enclosingScope;
        public final JsScope functionScope;
        public final JsScope namespaceScope;
    }

    private TranslationContext(JsName currentNamespace, JsProgram program,
                               BindingContext bindingContext, Scopes scopes, ContextType type) {
        assert program != null;
        assert bindingContext != null;
        assert scopes != null;
        assert currentNamespace != null;
        assert type != null;
        this.program = program;
        this.bindingContext = bindingContext;
        this.type = type;
        this.currentNamespace = currentNamespace;
        this.scopes = scopes;
    }

    @NotNull
    public static TranslationContext rootContext(JsProgram program, BindingContext bindingContext) {
        JsScope rootScope = program.getRootScope();
        Scopes scopes = new Scopes(rootScope, rootScope, rootScope);
        return new TranslationContext(null,
                program, bindingContext, scopes, ContextType.NAMESPACE_BODY);
    }

    //TODO implement correct factories
    @NotNull
    public TranslationContext newNamespace(JsName namespaceName, JsFunction namespaceDummyFunction) {
        JsScope newScope = namespaceDummyFunction.getScope();
        Scopes newScopes = new Scopes(newScope, newScope, newScope);
        return new TranslationContext(namespaceName, program,
                bindingContext, newScopes, ContextType.NAMESPACE_BODY);
    }

    @NotNull
    public TranslationContext newBlock() {
        Scopes newScopes = new Scopes(new JsScope
                (scopes.enclosingScope, "dummy enclosingScope for a block"), scopes.functionScope, scopes.namespaceScope);
        return new TranslationContext(currentNamespace, program,
                bindingContext, newScopes, type);
    }

    @NotNull
    public TranslationContext newFunction(JsFunction function) {
        JsScope functionScope = function.getScope();
        Scopes newScopes = new Scopes(functionScope, functionScope, scopes.namespaceScope);
        return new TranslationContext(currentNamespace, program,
                bindingContext, newScopes, ContextType.FUNCTION_BODY);
    }

    @NotNull
    public JsNameRef getNamespaceQualifiedReference(JsName name) {
        if (currentNamespace != null) {
            return AstUtil.newNameRef(currentNamespace.makeRef(), name);
        }
        return new JsNameRef(name);
    }

    @NotNull
    public JsName getJSName(String jetName) {
        return new JsName(program().getScope(), jetName, jetName, jetName);
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
    ContextType type() {
        return type;
    }
}
