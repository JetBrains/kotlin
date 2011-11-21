package org.jetbrains.k2js.declarations;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.k2js.translate.Namer;

/**
 * @author Talanov Pavel
 */
public final class ExtractionVisitor extends DeclarationDescriptorVisitor<Void, JsScope> {

    @NotNull
    private final Declarations declarations;

    /*package*/ ExtractionVisitor(@NotNull Declarations declarations) {
        this.declarations = declarations;
    }

    @Override
    public Void visitClassDescriptor(@NotNull ClassDescriptor descriptor, @NotNull JsScope enclosingScope) {
        JsScope classScope = extractClassDeclarations(descriptor, enclosingScope);
        visitClassMembers(descriptor, classScope);
        return null;
    }

    @NotNull
    private JsScope extractClassDeclarations(@NotNull ClassDescriptor descriptor, @NotNull JsScope enclosingScope) {
        String className = descriptor.getName();
        declarations.putName(descriptor, enclosingScope.declareName(className));
        JsScope classScope = new JsScope(enclosingScope, "class " + className);
        declarations.putScope(descriptor, classScope);
        return classScope;
    }

    private void visitClassMembers(@NotNull ClassDescriptor descriptor, @NotNull JsScope classScope) {
        visitClassConstructor(descriptor, classScope);
        for (DeclarationDescriptor memberDescriptor :
                descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            memberDescriptor.accept(this, classScope);
        }
    }

    private void visitClassConstructor(@NotNull ClassDescriptor descriptor, @NotNull JsScope classScope) {
        for (ConstructorDescriptor constructorDescriptor : descriptor.getConstructors()) {
            constructorDescriptor.accept(this, classScope);
        }
    }

    //TODO: think about the ways to make this less hacky
    //For now constructor just references the name of the class. Initialize method scope is defined independently.
    @Override
    public Void visitConstructorDescriptor(@NotNull ConstructorDescriptor descriptor, @NotNull JsScope enclosingScope) {
        String className = descriptor.getContainingDeclaration().getName();
        JsName alreadyDeclaredClassName = enclosingScope.findExistingName(className);
        declarations.putName(descriptor, alreadyDeclaredClassName);
        return null;
    }

    @Override
    public Void visitFunctionDescriptor(@NotNull FunctionDescriptor descriptor, @NotNull JsScope enclosingScope) {
        String functionName = descriptor.getName();
        declarations.putName(descriptor, enclosingScope.declareName(functionName));
        JsScope functionScope = new JsScope(enclosingScope, "function " + functionName);
        declarations.putScope(descriptor, functionScope);
        return null;
    }

    @Override
    public Void visitPropertyDescriptor(@NotNull PropertyDescriptor descriptor, @NotNull JsScope enclosingScope) {
        String propertyName = descriptor.getName();
        declarations.putName(descriptor, enclosingScope.declareName(Namer.getKotlinBackingFieldName(propertyName)));
        extractAccessor(descriptor.getGetter(), true, propertyName, enclosingScope);
        if (descriptor.isVar()) {
            extractAccessor(descriptor.getSetter(), false, propertyName, enclosingScope);
        }
        return null;
    }

    //Not using visitors here for convenience
    public void extractAccessor(@Nullable PropertyAccessorDescriptor descriptor, boolean isGetter, String propertyName,
                                @NotNull JsScope enclosingScope) {
        assert descriptor != null : "Accessor descriptor should not be null";
        String accessorName = Namer.getNameForAccessor(propertyName, isGetter);
        JsName jsName = enclosingScope.declareName(accessorName);
        JsScope accessorScope = new JsScope(enclosingScope, (isGetter ? "getter " : "setter ") + propertyName);
        declarations.putScope(descriptor, accessorScope);
        declarations.putName(descriptor, jsName);
    }

    @Override
    public Void visitNamespaceDescriptor(@NotNull NamespaceDescriptor descriptor, @NotNull JsScope enclosingScope) {
        JsScope namespaceScope = extractNamespaceDeclaration(descriptor, enclosingScope);
        visitMemberDeclarations(descriptor, namespaceScope);
        return null;
    }

    @NotNull
    private JsScope extractNamespaceDeclaration(@NotNull NamespaceDescriptor descriptor,
                                                @NotNull JsScope enclosingScope) {
        String namespaceName = descriptor.getName();
        declarations.putName(descriptor, enclosingScope.declareName(namespaceName));
        JsScope namespaceScope = new JsScope(enclosingScope, "namespace " + namespaceName);
        declarations.putScope(descriptor, namespaceScope);
        return namespaceScope;
    }

    private void visitMemberDeclarations(@NotNull NamespaceDescriptor descriptor, @NotNull JsScope namespaceScope) {
        for (DeclarationDescriptor memberDescriptor :
                descriptor.getMemberScope().getAllDescriptors()) {
            memberDescriptor.accept(this, namespaceScope);
        }
    }

}
