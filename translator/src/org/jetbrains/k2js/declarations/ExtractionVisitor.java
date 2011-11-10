package org.jetbrains.k2js.declarations;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import com.intellij.codeInspection.ex.Descriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.k2js.translate.Namer;

/**
 * @author Talanov Pavel
 */
public final class ExtractionVisitor extends DeclarationDescriptorVisitor<Void, JsScope> {

    @NotNull private final DeclarationExtractor extractor;

    /*package*/ ExtractionVisitor(@NotNull DeclarationExtractor extractor) {
        this.extractor = extractor;
    }


    @Override
    public Void visitClassDescriptor(ClassDescriptor descriptor, JsScope enclosingScope) {
        String className = descriptor.getName();
        enclosingScope.declareName(className);
        JsScope classScope = new JsScope(enclosingScope, "Scope for class " + className);
        extractor.put(descriptor, classScope);
        for (DeclarationDescriptor memberDescriptor :
                descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            memberDescriptor.accept(this, classScope);
        }
        return null;
    }

    @Override
    public Void visitFunctionDescriptor(FunctionDescriptor descriptor, JsScope enclosingScope) {
        String functionName = descriptor.getName();
        enclosingScope.declareName(functionName);
        JsScope functionScope = new JsScope(enclosingScope, "Scope for function " + functionName);
        extractor.put(descriptor, functionScope);
        return null;
    }

    @Override
    public Void visitPropertyDescriptor(PropertyDescriptor descriptor, JsScope enclosingScope) {
        String propertyName = descriptor.getName();
        enclosingScope.declareName(Namer.getNameForSetter(propertyName));
        enclosingScope.declareName(Namer.getNameForGetter(propertyName));
        return null;
    }

    @Override
    public Void visitNamespaceDescriptor(NamespaceDescriptor descriptor, JsScope enclosingScope) {
        String namespaceName = descriptor.getName();
        enclosingScope.declareName(namespaceName);
        JsScope namespaceScope = new JsScope(enclosingScope, "Scope for namespace " + namespaceName);
        extractor.put(descriptor, namespaceScope);
        for (DeclarationDescriptor memberDescriptor :
                descriptor.getMemberScope().getAllDescriptors()) {
            memberDescriptor.accept(this, namespaceScope);
        }
        return null;
    }

}
