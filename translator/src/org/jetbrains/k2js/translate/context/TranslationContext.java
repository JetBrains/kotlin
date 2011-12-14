package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyAccessorDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFQName;

/**
 * @author Talanov Pavel
 */
public final class TranslationContext {

    @NotNull
    private final DynamicContext dynamicContext;
    @NotNull
    private final StaticContext staticContext;

    @NotNull
    public static TranslationContext rootContext(@NotNull StaticContext staticContext) {
        JsProgram program = staticContext.getProgram();
        JsBlock globalBlock = program.getGlobalBlock();
        return new TranslationContext(staticContext,
                DynamicContext.rootContext(staticContext.getRootScope(), globalBlock));
    }

    private TranslationContext(@NotNull StaticContext staticContext, @NotNull DynamicContext dynamicContext) {
        this.dynamicContext = dynamicContext;
        this.staticContext = staticContext;
    }

    @NotNull
    public TranslationContext contextWithScope(@NotNull NamingScope newScope) {
        return new TranslationContext(staticContext, DynamicContext.contextWithScope(newScope));
    }

    // Note: Should be used if and only if scope has no corresponding descriptor
    @NotNull
    public TranslationContext innerJsScope(@NotNull JsScope enclosingScope) {
        return new TranslationContext(staticContext, dynamicContext.innerScope(enclosingScope));
    }

    @NotNull
    public TranslationContext newNamespace(@NotNull JetNamespace declaration) {
        return newDeclaration(BindingUtils.getNamespaceDescriptor(staticContext.getBindingContext(), declaration));
    }

    @NotNull
    public TranslationContext newDeclaration(@NotNull DeclarationDescriptor descriptor) {
        NamingScope declarationScope = staticContext.getDeclarations().getScope(descriptor);
        return contextWithScope(declarationScope);
    }

    @NotNull
    public TranslationContext newClass(@NotNull JetClass declaration) {
        return newDeclaration(BindingUtils.getClassDescriptor(staticContext.getBindingContext(), declaration));
    }

    @NotNull
    public TranslationContext newPropertyAccess(@NotNull JetPropertyAccessor declaration) {
        return newDeclaration(BindingUtils.getPropertyAccessorDescriptor(staticContext.getBindingContext(), declaration));
    }

    @NotNull
    public TranslationContext newPropertyAccess(@NotNull PropertyAccessorDescriptor descriptor) {
        return newDeclaration(descriptor);
    }

    @NotNull
    public TranslationContext newFunctionDeclaration(@NotNull JetNamedFunction declaration) {
        return newDeclaration(BindingUtils.getFunctionDescriptor(staticContext.getBindingContext(), declaration));
    }

    @NotNull
    public BindingContext bindingContext() {
        return staticContext.getBindingContext();
    }


    @NotNull
    public NamingScope getScopeForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.getScopeForDescriptor(descriptor);
    }

    @NotNull
    public NamingScope getScopeForElement(@NotNull JetElement element) {
        return staticContext.getScopeForElement(element);
    }

    @NotNull
    public JsName getNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (aliaser().hasAliasForDeclaration(descriptor)) {
            return aliaser().getAliasForDeclaration(descriptor);
        }
        if (standardClasses().isStandardObject(descriptor)) {
            return standardClasses().getStandardObjectName(descriptor);
        }
        if (dynamicContext.isDeclared(descriptor)) {
            return dynamicContext.getLocalName(descriptor);
        }
        if (staticContext.isDeclared(descriptor)) {
            return staticContext.getGlobalName(descriptor);
        }
        throw new AssertionError("Undefined descriptor: " + getFQName(descriptor));
    }

    @NotNull
    public JsNameRef getQualifierForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (standardClasses().isStandardObject(descriptor)) {
            return namer().kotlinObject();
        }
        return declarations().getQualifier(descriptor);
    }


    public boolean hasQualifierForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return (declarations().hasQualifier(descriptor) ||
                standardClasses().isStandardObject(descriptor));
    }

    @NotNull
    public JsName getNameForElement(@NotNull JetElement element) {
        DeclarationDescriptor descriptor = BindingUtils.getDescriptorForElement(bindingContext(), element);
        return getNameForDescriptor(descriptor);
    }

    public boolean isStandardObject(@NotNull DeclarationDescriptor descriptor) {
        return standardClasses().isStandardObject(descriptor);
    }

    @NotNull
    public JsName getNameForStandardObject(@NotNull DeclarationDescriptor descriptor) {
        return standardClasses().getStandardObjectName(descriptor);
    }

    @NotNull
    public TemporaryVariable declareTemporary(@NotNull JsExpression initExpression) {
        return dynamicContext.declareTemporary(initExpression);
    }

    @NotNull
    public JsName declareLocalVariable(@NotNull DeclarationDescriptor descriptor) {
        return dynamicContext.declareLocalVariable(descriptor);
    }

    @NotNull
    public JsName declareLocalVariable(@NotNull JetElement element) {
        DeclarationDescriptor declarationDescriptor =
                BindingUtils.getDescriptorForElement(bindingContext(), element);
        return dynamicContext.declareLocalVariable(declarationDescriptor);
    }

    @NotNull
    public TemporaryVariable newAliasForThis() {
        TemporaryVariable aliasForThis = dynamicContext.declareTemporary(new JsThisRef());
        aliaser().setAliasForThis(aliasForThis.name());
        return aliasForThis;
    }

    public void removeAliasForThis(@NotNull JsName aliasToRemove) {
        aliaser().removeAliasForThis(aliasToRemove);
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

    @NotNull
    public JsProgram program() {
        return staticContext.getProgram();
    }

    @NotNull
    private StandardClasses standardClasses() {
        return staticContext.getStandardClasses();
    }

    @NotNull
    public JsScope jsScope() {
        return dynamicContext.jsScope();
    }
}
