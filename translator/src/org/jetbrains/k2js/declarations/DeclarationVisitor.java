package org.jetbrains.k2js.declarations;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.k2js.translate.context.Namer;

/**
 * @author Talanov Pavel
 */
public final class DeclarationVisitor extends DeclarationDescriptorVisitor<Void, DeclarationContext> {

    @NotNull
    private final Declarations declarations;

    /*package*/ DeclarationVisitor(@NotNull Declarations declarations) {
        this.declarations = declarations;
    }

    @NotNull
    private DeclarationContext declareClass(@NotNull ClassDescriptor descriptor,
                                            @NotNull DeclarationContext context) {
        JsScope classScope = declareScope(descriptor, context, "class " + descriptor.getName());
        JsName className = declareName(descriptor, context);
        return context.innerDeclaration(classScope, className);
    }

    private JsScope declareScope(@NotNull DeclarationDescriptor descriptor, @NotNull DeclarationContext context,
                                 @NotNull String scopeName) {
        JsScope classScope = new JsScope(context.getScope(), scopeName);
        declarations.putScope(descriptor, classScope);
        return classScope;
    }

    @NotNull
    private JsName declareName(@NotNull DeclarationDescriptor descriptor, @NotNull DeclarationContext context,
                               @NotNull String name) {
        JsName jsName = context.getScope().declareName(name);
        declarations.putName(descriptor, jsName);
        declarations.putQualifier(descriptor, context.getQualifier());
        return jsName;
    }

    @NotNull
    private JsName declareName(@NotNull DeclarationDescriptor descriptor,
                               @NotNull DeclarationContext context) {
        return declareName(descriptor, context, descriptor.getName());
    }


    @Override
    public Void visitClassDescriptor(@NotNull ClassDescriptor descriptor, @NotNull DeclarationContext context) {
        DeclarationContext classContext = declareClass(descriptor, context);
        declareClassConstructor(descriptor, context);
        declareClassMembers(descriptor, classContext);
        return null;
    }

    private void declareClassMembers(@NotNull ClassDescriptor descriptor, @NotNull DeclarationContext context) {
        for (DeclarationDescriptor memberDescriptor :
                descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
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
        JsName alreadyDeclaredClassName = declarations.getName(descriptor.getContainingDeclaration());
        //already defined in
        declarations.putName(descriptor, alreadyDeclaredClassName);
        declarations.putQualifier(descriptor, context.getQualifier());
        return null;
    }

    @Override
    public Void visitFunctionDescriptor(@NotNull FunctionDescriptor descriptor, @NotNull DeclarationContext context) {
        declareName(descriptor, context);
        declareScope(descriptor, context, "function " + descriptor.getName());
        return null;
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
        assert descriptor != null : "Accessor descriptor should not be null";
        String accessorName = Namer.getNameForAccessor(propertyName, isGetter);
        declareName(descriptor, context, accessorName);
        declareScope(descriptor, context, (isGetter ? "getter " : "setter ") + propertyName);
    }

    @Override
    public Void visitNamespaceDescriptor(@NotNull NamespaceDescriptor descriptor, @NotNull DeclarationContext context) {
        DeclarationContext namespaceContext = extractNamespaceDeclaration(descriptor, context);
        declareMembers(descriptor, namespaceContext);
        return null;
    }

    @NotNull
    private DeclarationContext extractNamespaceDeclaration(@NotNull NamespaceDescriptor descriptor,
                                                           @NotNull DeclarationContext context) {
        JsName namespaceName = declareName(descriptor, context, nameForNamespace(descriptor));
        JsScope namespaceScope = declareScope(descriptor, context, "namespace " + namespaceName.getIdent());
        return context.innerDeclaration(namespaceScope, namespaceName);
    }

    @NotNull
    private String nameForNamespace(@NotNull NamespaceDescriptor descriptor) {
        String name = descriptor.getName();
        if (name.equals("")) {
            return "Anonymous";
        }
        return name;
    }

    private void declareMembers(@NotNull NamespaceDescriptor descriptor, @NotNull DeclarationContext context) {
        for (DeclarationDescriptor memberDescriptor :
                descriptor.getMemberScope().getAllDescriptors()) {
            memberDescriptor.accept(this, context);
        }
    }

}
