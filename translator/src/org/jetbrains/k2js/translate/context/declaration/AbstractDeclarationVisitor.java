package org.jetbrains.k2js.translate.context.declaration;

import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.k2js.translate.context.NamingScope;

import java.util.Set;

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

    abstract public void traverseNamespace(@NotNull NamespaceDescriptor namespace, @NotNull DeclarationContext context);

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
        assert accept(descriptor) : "Must accept constructor fot he class we accepted";
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


}
