package org.jetbrains.k2js.translate.context.declaration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;

/**
 * @author Pavel Talanov
 */
public final class JsAnnotationsUtils {

    private JsAnnotationsUtils() {
    }

    @NotNull
    public static String getNativeName(@NotNull FunctionDescriptor descriptor) {
        if (hasNativeFunctionAnnotation(descriptor)) {
            return getNativeFunctionName(descriptor);
        }
        if (declaredInNativeClass(descriptor)) {
            return descriptor.getName();
        }
        throw new AssertionError("Use isNativeFunction to check");
    }

    public static boolean isNativeFunction(@NotNull FunctionDescriptor functionDescriptor) {
        return hasNativeFunctionAnnotation(functionDescriptor) || declaredInNativeClass(functionDescriptor);
    }

    public static boolean isNativeClass(@NotNull ClassDescriptor classDescriptor) {
        return hasNativeClassAnnotation(classDescriptor);
    }

    private static boolean declaredInNativeClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof ClassDescriptor)) return false;
        return hasNativeClassAnnotation((ClassDescriptor) containingDeclaration);
    }

    private static boolean hasNativeFunctionAnnotation(@NotNull FunctionDescriptor annotationDescriptor) {
        return (getAnnotationByName(annotationDescriptor, "js.annotations.JavascriptNativeFunction") != null);
    }

    private static boolean hasNativeClassAnnotation(@NotNull ClassDescriptor descriptor) {
        return (getAnnotationByName(descriptor, "JavascriptNativeClass") != null);
    }

    @NotNull
    private static String getNativeFunctionName(@NotNull DeclarationDescriptor declarationDescriptor) {
        AnnotationDescriptor annotationDescriptor =
                getAnnotationByName(declarationDescriptor, "js.annotations.JavascriptNativeFunction");
        assert annotationDescriptor != null;
        Object value = annotationDescriptor.getValueArguments().iterator().next().getValue();
        assert value instanceof String : "Native function annotation should have one String parameter";
        return (String) value;
    }

    @Nullable
    private static AnnotationDescriptor getAnnotationByName(@NotNull DeclarationDescriptor descriptor,
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
    private static String getAnnotationClassFQName(@NotNull AnnotationDescriptor annotationDescriptor) {
        DeclarationDescriptor annotationDeclaration =
                annotationDescriptor.getType().getConstructor().getDeclarationDescriptor();
        assert annotationDeclaration != null : "Annotation supposed to have a declaration";
        return DescriptorUtils.getFQName(annotationDeclaration);
    }
}
