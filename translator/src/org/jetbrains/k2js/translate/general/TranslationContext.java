package org.jetbrains.k2js.translate.general;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.backend.js.ast.JsScope;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.declarations.Declarations;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Talanov Pavel
 */
public final class TranslationContext {


    // TODO: extract different parts of this classes into separate classes
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
    public static TranslationContext rootContext(@NotNull JsProgram program, @NotNull BindingContext bindingContext,
                                                 @NotNull Declarations extractor) {
        JsScope rootScope = program.getRootScope();
        Scopes scopes = new Scopes(rootScope, rootScope, rootScope);
        return new TranslationContext(null, program, bindingContext,
                scopes, extractor, new HashMap<JetDeclaration, JsName>(), null);
    }

    @NotNull
    private final JsProgram program;
    @NotNull
    private final BindingContext bindingContext;
    @NotNull
    private final Scopes scopes;
    @Nullable
    private final JsName namespaceName;
    @NotNull
    private final Declarations declarations;
    @NotNull
    private final Map<JetDeclaration, JsName> aliases;
    @Nullable
    private JsName aliasForThis;


    private TranslationContext(@Nullable JsName namespaceName, @NotNull JsProgram program,
                               @NotNull BindingContext bindingContext, @NotNull Scopes scopes,
                               @NotNull Declarations declarations, @NotNull Map<JetDeclaration, JsName> aliases,
                               @Nullable JsName aliasForThis) {
        this.program = program;
        this.bindingContext = bindingContext;
        this.namespaceName = namespaceName;
        this.scopes = scopes;
        this.declarations = declarations;
        this.aliases = aliases;
        this.aliasForThis = aliasForThis;
    }

    @NotNull
    public TranslationContext newNamespace(@NotNull JetNamespace declaration) {
        return newNamespace(BindingUtils.getNamespaceDescriptor(bindingContext, declaration));
    }

    @NotNull
    public TranslationContext newNamespace(@NotNull NamespaceDescriptor descriptor) {
        JsScope namespaceScope = declarations.getScope(descriptor);
        JsName namespaceName = getNameForDescriptor(descriptor);
        Scopes newScopes = new Scopes(namespaceScope, namespaceScope, namespaceScope);
        return new TranslationContext(namespaceName, program, bindingContext, newScopes,
                declarations, aliases, aliasForThis);
    }

    @NotNull
    public TranslationContext newClass(@NotNull JetClass declaration) {
        return newClass(BindingUtils.getClassDescriptor(bindingContext, declaration));
    }

    @NotNull
    public TranslationContext newClass(@NotNull ClassDescriptor descriptor) {
        JsScope classScope = declarations.getScope(descriptor);
        Scopes newScopes = new Scopes(classScope, classScope, scopes.namespaceScope);
        return new TranslationContext(namespaceName, program, bindingContext,
                newScopes, declarations, aliases, aliasForThis);
    }

    @NotNull
    public TranslationContext newPropertyAccess(@NotNull JetPropertyAccessor declaration) {
        return newPropertyAccess(BindingUtils.getPropertyAccessorDescriptor(bindingContext, declaration));
    }

    @NotNull
    public TranslationContext newPropertyAccess(@NotNull PropertyAccessorDescriptor descriptor) {
        return newFunctionDeclaration(descriptor);
    }

    @NotNull
    public TranslationContext newFunctionDeclaration(@NotNull JetNamedFunction declaration) {
        return newFunctionDeclaration(BindingUtils.getFunctionDescriptor(bindingContext, declaration));
    }

    @NotNull
    public TranslationContext newFunctionDeclaration(@NotNull FunctionDescriptor descriptor) {
        JsScope functionScope = declarations.getScope(descriptor);
        Scopes newScopes = new Scopes(functionScope, scopes.classScope, scopes.namespaceScope);
        return new TranslationContext(namespaceName, program, bindingContext,
                newScopes, declarations, aliases, aliasForThis);
    }

//    @NotNull TranslationContext newAliases(Map<JetDeclaration, JsName> newAliases) {
//        Map<JetDeclaration, JsName> aliases = new HashMap<JetDeclaration, JsName>(this.aliases);
//        aliases.putAll(newAliases);
//        return new TranslationContext(namespaceName, program, bindingContext, scopes, declarations, aliases);
//    }

    // Note: Should be used if and only if scope has no corresponding descriptor
    @NotNull
    public TranslationContext newEnclosingScope(@NotNull JsScope enclosingScope) {
        Scopes newScopes = new Scopes(enclosingScope, scopes.classScope, scopes.namespaceScope);
        return new TranslationContext(namespaceName, program, bindingContext,
                newScopes, declarations, aliases, aliasForThis);
    }


    @NotNull
    public JsNameRef getNamespaceQualifiedReference(JsName name) {
        if (namespaceName != null) {
            return AstUtil.newNameRef(namespaceName.makeRef(), name);
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
    public Map<JetDeclaration, JsName> aliases() {
        return aliases;
    }

    @NotNull
    public JsName declareLocalName(@NotNull String name) {
        return scopes.enclosingScope.declareFreshName(name);
    }

    @NotNull
    public JsScope getScopeForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return declarations.getScope(descriptor);
    }

    @NotNull
    public JsScope getScopeForElement(@NotNull JetElement element) {
        DeclarationDescriptor descriptor = getDescriptorForElement(element);
        return getScopeForDescriptor(descriptor);
    }

    @NotNull
    public JsName getNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return declarations.getName(descriptor);
    }

    @NotNull
    public JsName getNameForElement(@NotNull JetElement element) {
        DeclarationDescriptor descriptor = getDescriptorForElement(element);
        return getNameForDescriptor(descriptor);
    }

    @NotNull
    private DeclarationDescriptor getDescriptorForElement(@NotNull JetElement element) {
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        assert descriptor != null : "Element should have a descriptor";
        return descriptor;
    }

    @NotNull
    public JsNameRef getAliasForThis() {
        assert aliasForThis != null : "Alias is null. Use hasAliasForThis function to check.";
        return aliasForThis.makeRef();
    }

    public void setAliasForThis(@NotNull JsName alias) {
        aliasForThis = alias;
    }

    public void removeAliasForThis() {
        aliasForThis = null;
    }

    public boolean hasAliasForThis() {
        return (aliasForThis != null);
    }

    public boolean isDeclared(@NotNull DeclarationDescriptor descriptor) {
        return declarations.isDeclared(descriptor);
    }
}
