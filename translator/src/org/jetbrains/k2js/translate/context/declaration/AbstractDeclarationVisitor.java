package org.jetbrains.k2js.translate.context.declaration;

import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.NamingScope;

import java.util.Set;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getNameForNamespace;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getOwnDeclarations;

/**
 * @author Pavel Talanov
 */
public abstract class AbstractDeclarationVisitor extends DeclarationDescriptorVisitor<Void, DeclarationContext> {

    @NotNull
    private final Declarations declarations;

    /*package*/ AbstractDeclarationVisitor(@NotNull Declarations declarations) {
        this.declarations = declarations;
    }

    abstract protected NamingScope doDeclareScope(@NotNull DeclarationDescriptor descriptor,
                                                  @NotNull DeclarationContext context,
                                                  @NotNull String recommendedName);

    @NotNull
    abstract protected JsName doDeclareName(@NotNull DeclarationDescriptor descriptor,
                                            @NotNull DeclarationContext context,
                                            @NotNull String recommendedName);

    abstract protected boolean accept(@NotNull DeclarationDescriptor descriptor);

    @NotNull
    protected Declarations declarations() {
        return declarations;
    }

    @Nullable
    private JsName declareName(@NotNull DeclarationDescriptor descriptor,
                               @NotNull DeclarationContext context) {
        return declareName(descriptor, context, descriptor.getName());
    }

    @Nullable
    private JsName declareName(@NotNull DeclarationDescriptor descriptor,
                               @NotNull DeclarationContext context,
                               @NotNull String name) {
        if (!accept(descriptor)) {
            return null;
        }
        return doDeclareName(descriptor, context, name);
    }

    @Nullable
    private NamingScope declareScope(@NotNull DeclarationDescriptor descriptor,
                                     @NotNull DeclarationContext context,
                                     @NotNull String name) {
        if (!accept(descriptor)) {
            return null;
        }
        return doDeclareScope(descriptor, context, name);
    }

    @Override
    public Void visitClassDescriptor(@NotNull ClassDescriptor descriptor, @NotNull DeclarationContext context) {
        if (!accept(descriptor)) {
            return null;
        }
        DeclarationContext classContext = declareClass(descriptor, context);
        declareClassConstructor(descriptor, context);
        declareClassMembers(descriptor, classContext);
        return null;
    }

    @NotNull
    private DeclarationContext declareClass(@NotNull ClassDescriptor descriptor,
                                            @NotNull DeclarationContext context) {
        NamingScope classScope = doDeclareScope(descriptor, context, "class " + descriptor.getName());
        JsName className = doDeclareName(descriptor, context, descriptor.getName());
        return context.innerDeclaration(classScope, className);
    }

    private void declareClassMembers(@NotNull ClassDescriptor descriptor, @NotNull DeclarationContext context) {
        for (DeclarationDescriptor memberDescriptor : getOwnDeclarations(descriptor)) {
            memberDescriptor.accept(this, context);
        }
    }

    private void declareClassConstructor(@NotNull ClassDescriptor descriptor, @NotNull DeclarationContext context) {
        for (ConstructorDescriptor constructorDescriptor : descriptor.getConstructors()) {
            constructorDescriptor.accept(this, context);
        }
    }

    @Override
    public Void visitConstructorDescriptor(@NotNull ConstructorDescriptor descriptor,
                                           @NotNull DeclarationContext context) {
        if (!accept(descriptor)) {
            return null;
        }
        JsName alreadyDeclaredClassName = declarations.getName(descriptor.getContainingDeclaration());
        //already defined in
        declarations.putName(descriptor, alreadyDeclaredClassName);
        declarations.putQualifier(descriptor, context.getQualifier());
        return null;
    }

    @Override
    public Void visitFunctionDescriptor(@NotNull FunctionDescriptor descriptor, @NotNull DeclarationContext context) {
        boolean overridesDeclaredDescriptor = declareAsOverridden(descriptor, context);
        if (overridesDeclaredDescriptor) {
            return null;
        }
        declareName(descriptor, context);
        declareScope(descriptor, context, "function " + descriptor.getName());
        return null;
    }

    private boolean declareAsOverridden(@NotNull FunctionDescriptor descriptor, @NotNull DeclarationContext context) {
        Set<? extends FunctionDescriptor> overriddenDescriptors = descriptor.getOverriddenDescriptors();
        for (FunctionDescriptor overriddenDescriptor : overriddenDescriptors) {
            if (declarations.hasDeclaredName(overriddenDescriptor)) {
                declarations.putName(descriptor, declarations.getName(overriddenDescriptor));
                declareScope(descriptor, context, "function " + descriptor.getName());
                return true;
            }
        }
        return false;
    }

    @Override
    public Void visitPropertyDescriptor(@NotNull PropertyDescriptor descriptor, @NotNull DeclarationContext context) {
        String propertyName = descriptor.getName();
        String backingFieldName = Namer.getKotlinBackingFieldName(propertyName);
        declareName(descriptor, context, backingFieldName);
        extractAccessor(descriptor.getGetter(), true, propertyName, context);
        if (descriptor.isVar()) {
            extractAccessor(descriptor.getSetter(), false, propertyName, context);
        }
        return null;
    }

    public void extractAccessor(@Nullable PropertyAccessorDescriptor descriptor, boolean isGetter,
                                @NotNull String propertyName, @NotNull DeclarationContext context) {
        if (descriptor == null) return;

        String accessorName = Namer.getNameForAccessor(propertyName, isGetter);
        declareName(descriptor, context, accessorName);
        declareScope(descriptor, context, (isGetter ? "getter " : "setter ") + propertyName);
    }

    @Override
    public Void visitNamespaceDescriptor(@NotNull NamespaceDescriptor descriptor, @NotNull DeclarationContext context) {
        //TODO: traverse
        /* do not traverse inner namespaces */
        return null;
    }

    public void traverseNamespace(@NotNull NamespaceDescriptor descriptor, @NotNull DeclarationContext context) {
        if (!accept(descriptor)) {
            return;
        }
        DeclarationContext namespaceContext = extractNamespaceDeclaration(descriptor, context);
        declareMembers(descriptor, namespaceContext);
    }

    @NotNull
    private DeclarationContext extractNamespaceDeclaration(@NotNull NamespaceDescriptor descriptor,
                                                           @NotNull DeclarationContext context) {
        String nameForNamespace = getNameForNamespace(descriptor);
        JsName namespaceName = doDeclareName(descriptor, context, nameForNamespace);
        NamingScope namespaceScope = doDeclareScope(descriptor, context, "namespace " + namespaceName.getIdent());
        return context.innerDeclaration(namespaceScope, namespaceName);
    }

    protected void declareMembers(@NotNull NamespaceDescriptor descriptor, @NotNull DeclarationContext context) {
        for (DeclarationDescriptor memberDescriptor :
                descriptor.getMemberScope().getAllDescriptors()) {
            memberDescriptor.accept(this, context);
        }
    }

}
