package org.jetbrains.k2js.translate.general;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.declarations.Declarations;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.Namer;

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

        @NotNull
        public final JsScope enclosingScope;
        @NotNull
        public final JsScope classScope;
        @NotNull
        public final JsScope namespaceScope;
    }

    @NotNull
    public static TranslationContext rootContext(@NotNull StaticContext staticContext) {
        JsScope rootScope = staticContext.getProgram().getRootScope();
        Scopes scopes = new Scopes(rootScope, rootScope, rootScope);
        return new TranslationContext(staticContext, scopes);
    }

    @NotNull
    private final Scopes scopes;
    @NotNull
    private final StaticContext staticContext;


    private TranslationContext(@NotNull StaticContext staticContext, @NotNull Scopes scopes) {
        this.scopes = scopes;
        this.staticContext = staticContext;
    }

    @NotNull
    public TranslationContext newNamespace(@NotNull JetNamespace declaration) {
        return newNamespace(BindingUtils.getNamespaceDescriptor(staticContext.getBindingContext(), declaration));
    }

    @NotNull
    public TranslationContext newNamespace(@NotNull NamespaceDescriptor descriptor) {
        JsScope namespaceScope = staticContext.getDeclarations().getScope(descriptor);
        Scopes newScopes = new Scopes(namespaceScope, namespaceScope, namespaceScope);
        return new TranslationContext(staticContext, newScopes);
    }

    @NotNull
    public TranslationContext newClass(@NotNull JetClass declaration) {
        return newClass(BindingUtils.getClassDescriptor(staticContext.getBindingContext(), declaration));
    }

    @NotNull
    public TranslationContext newClass(@NotNull ClassDescriptor descriptor) {
        JsScope classScope = staticContext.getDeclarations().getScope(descriptor);
        Scopes newScopes = new Scopes(classScope, classScope, scopes.namespaceScope);
        return new TranslationContext(staticContext, newScopes);
    }

    @NotNull
    public TranslationContext newPropertyAccess(@NotNull JetPropertyAccessor declaration) {
        return newPropertyAccess(BindingUtils.getPropertyAccessorDescriptor(staticContext.getBindingContext(), declaration));
    }

    @NotNull
    public TranslationContext newPropertyAccess(@NotNull PropertyAccessorDescriptor descriptor) {
        return newFunctionDeclaration(descriptor);
    }

    @NotNull
    public TranslationContext newFunctionDeclaration(@NotNull JetNamedFunction declaration) {
        return newFunctionDeclaration(BindingUtils.getFunctionDescriptor(staticContext.getBindingContext(), declaration));
    }

    @NotNull
    public TranslationContext newFunctionDeclaration(@NotNull FunctionDescriptor descriptor) {
        JsScope functionScope = staticContext.getDeclarations().getScope(descriptor);
        Scopes newScopes = new Scopes(functionScope, scopes.classScope, scopes.namespaceScope);
        return new TranslationContext(staticContext, newScopes);
    }

    // Note: Should be used if and only if scope has no corresponding descriptor
    @NotNull
    public TranslationContext newEnclosingScope(@NotNull JsScope enclosingScope) {
        Scopes newScopes = new Scopes(enclosingScope, scopes.classScope, scopes.namespaceScope);
        return new TranslationContext(staticContext, newScopes);
    }

    @NotNull
    public BindingContext bindingContext() {
        return staticContext.getBindingContext();
    }

    @NotNull
    public JsProgram program() {
        return staticContext.getProgram();
    }

    @NotNull
    public JsScope enclosingScope() {
        return scopes.enclosingScope;
    }

    @NotNull
    public JsScope namespaceScope() {
        return scopes.namespaceScope;
    }

    @NotNull
    public JsScope classScope() {
        return scopes.classScope;
    }


    @NotNull
    public JsScope getScopeForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.getScopeForDescriptor(descriptor);
    }

    @NotNull
    public JsScope getScopeForElement(@NotNull JetElement element) {
        return staticContext.getScopeForElement(element);
    }

    @NotNull
    public JsName getNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.getNameForDescriptor(descriptor);
    }

    @NotNull
    public JsName getNameForElement(@NotNull JetElement element) {
        return staticContext.getNameForElement(element);
    }

    @NotNull
    private DeclarationDescriptor getDescriptorForElement(@NotNull JetElement element) {
        return staticContext.getDescriptorForElement(element);
    }

    public boolean isDeclared(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.isDeclared(descriptor);
    }

    @NotNull
    public Aliaser aliaser() {
        return staticContext.getAliaser();
    }

    @NotNull
    public Namer namer() {
        return staticContext.getNamer();
    }

    @NotNull
    public Declarations declarations() {
        return staticContext.getDeclarations();
    }

    @NotNull
    public Intrinsics intrinsics() {
        return staticContext.getIntrinsics();
    }
}
