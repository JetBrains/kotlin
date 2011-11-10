package org.jetbrains.k2js.declarations;

import com.google.dart.compiler.backend.js.ast.JsScope;
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
        PropertySetterDescriptor setter = descriptor.getSetter();
        if (setter != null) {
            setter.accept(this, enclosingScope);
        }
        PropertyGetterDescriptor getter = descriptor.getGetter();
        if (getter != null) {
            getter.accept(this, enclosingScope);
        }
        // We do not put this name into extractor because it can't be referenced from outside
        // thus it doesn't have a descriptor.
        enclosingScope.declareName(Namer.getBackingFieldNameForProperty(descriptor.getName()));
        return null;
    }

    @Override
    public Void visitPropertyGetterDescriptor(PropertyGetterDescriptor descriptor, JsScope enclosingScope) {
        String getterName = Namer.getNameForGetter(descriptor.getCorrespondingProperty().getName());
        extractor.putName(descriptor, enclosingScope.declareName(getterName));
        return null;
    }

    @Override
    public Void visitPropertySetterDescriptor(PropertySetterDescriptor descriptor, JsScope enclosingScope) {
        String setterName = Namer.getNameForSetter(descriptor.getCorrespondingProperty().getName());
        extractor.putName(descriptor, enclosingScope.declareName(setterName));
        return null;
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
