package org.jetbrains.k2js.translate.context.declaration;

import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.k2js.translate.context.NamingScope;

/**
 * @author Pavel Talanov
 */
public final class NativeDeclarationVisitor extends AbstractDeclarationVisitor {

    /*package*/ NativeDeclarationVisitor(@NotNull Declarations nativeDeclarations) {
        super(nativeDeclarations);
    }

    @NotNull
    @Override
    protected NamingScope doDeclareScope(@NotNull DeclarationDescriptor descriptor, @NotNull DeclarationContext context,
                                         @NotNull String recommendedName) {
        //TODO: probably we do
        /*we dont need to declare any scopes for native declarations*/
        return context.getScope();
    }

    @NotNull
    @Override
    protected JsName doDeclareName(@NotNull DeclarationDescriptor descriptor, @NotNull DeclarationContext context,
                                   @NotNull String recommendedName) {
        if (!(descriptor instanceof FunctionDescriptor)) {
            throw new IllegalStateException();
        }
        String nativeName = getNativeName((FunctionDescriptor) descriptor);
        assert nativeName != null;
        JsName jsName = context.getScope().
                declareVariable(descriptor, nativeName, false);
        jsName.setObfuscatable(false);
        declarations().putName(descriptor, jsName);
        declarations().putQualifier(descriptor, context.getQualifier());
        return jsName;
    }

    @Override
    protected boolean accept(@NotNull DeclarationDescriptor descriptor) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nullable
    private String getNativeName(@NotNull FunctionDescriptor descriptor) {
        if (isJavascriptNativeFunction(descriptor)) {
            return getNativeFunctionName(descriptor);
        }
        if (isJavascriptNativeClass(descriptor.getContainingDeclaration())) {
            return descriptor.getName();
        }
        return null;
    }

    private boolean isJavascriptNativeFunction(@NotNull DeclarationDescriptor annotationDescriptor) {
        return (getAnnotationByName(annotationDescriptor, "js.annotations.JavascriptNativeFunction") != null);
    }

    private boolean isJavascriptNativeClass(@NotNull DeclarationDescriptor descriptor) {
        assert descriptor instanceof ClassDescriptor;
        return (getAnnotationByName(descriptor, "JavascriptNativeClass") != null);
    }

    @NotNull
    private String getNativeFunctionName(@NotNull DeclarationDescriptor declarationDescriptor) {
        AnnotationDescriptor annotationDescriptor =
                getAnnotationByName(declarationDescriptor, "js.annotations.JavascriptNativeFunction");
        assert annotationDescriptor != null;
        Object value = annotationDescriptor.getValueArguments().iterator().next().getValue();
        assert value instanceof String : "Native function annotation should have one String parameter";
        return (String) value;
    }

    @Nullable
    private AnnotationDescriptor getAnnotationByName(@NotNull DeclarationDescriptor descriptor,
                                                     @NotNull String FQName) {
        for (AnnotationDescriptor annotationDescriptor : descriptor.getAnnotations()) {
            String annotationClassFQName = getAnnotationClassFQName(annotationDescriptor);
            if (annotationClassFQName.equals(FQName)) {
                return annotationDescriptor;
            }
        }
        return null;
    }

    @NotNull
    private String getAnnotationClassFQName(@NotNull AnnotationDescriptor annotationDescriptor) {
        DeclarationDescriptor annotationDeclaration =
                annotationDescriptor.getType().getConstructor().getDeclarationDescriptor();
        assert annotationDeclaration != null : "Annotation supposed to have a declaration";
        return DescriptorUtils.getFQName(annotationDeclaration);
    }
}
