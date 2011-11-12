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
    private final DeclarationExtractor extractor;

    /*package*/ ExtractionVisitor(@NotNull DeclarationExtractor extractor) {
        this.extractor = extractor;
    }


    @Override
    public Void visitClassDescriptor(@NotNull ClassDescriptor descriptor, @NotNull JsScope enclosingScope) {
        String className = descriptor.getName();
        extractor.putName(descriptor, enclosingScope.declareName(className));
        JsScope classScope = new JsScope(enclosingScope, "class " + className);
        extractor.putScope(descriptor, classScope);
        for (DeclarationDescriptor memberDescriptor :
                descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            memberDescriptor.accept(this, classScope);
        }
        return null;
    }

    @Override
    public Void visitFunctionDescriptor(@NotNull FunctionDescriptor descriptor, @NotNull JsScope enclosingScope) {
        String functionName = descriptor.getName();
        extractor.putName(descriptor, enclosingScope.declareName(functionName));
        JsScope functionScope = new JsScope(enclosingScope, "function " + functionName);
        extractor.putScope(descriptor, functionScope);
        return null;
    }

    @Override
    public Void visitPropertyDescriptor(@NotNull PropertyDescriptor descriptor, @NotNull JsScope enclosingScope) {
        String propertyName = descriptor.getName();
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
        extractor.putScope(descriptor, accessorScope);
        extractor.putName(descriptor, jsName);
        // Note : We do not put backing field name into extractor because it can't be referenced from outside
        accessorScope.declareName(Namer.getKotlinBackingFieldName(descriptor.getCorrespondingProperty().getName()));
    }

    @Override
    public Void visitNamespaceDescriptor(NamespaceDescriptor descriptor, JsScope enclosingScope) {
        String namespaceName = descriptor.getName();
        extractor.putName(descriptor, enclosingScope.declareName(namespaceName));
        JsScope namespaceScope = new JsScope(enclosingScope, "namespace " + namespaceName);
        extractor.putScope(descriptor, namespaceScope);
        for (DeclarationDescriptor memberDescriptor :
                descriptor.getMemberScope().getAllDescriptors()) {
            memberDescriptor.accept(this, namespaceScope);
        }
        return null;
    }

}
