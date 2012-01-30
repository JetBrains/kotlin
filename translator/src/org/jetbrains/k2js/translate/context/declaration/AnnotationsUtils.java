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
public final class AnnotationsUtils {

    @NotNull
    private static final String NATIVE_FUNCTION_ANNOTATION_FQNAME = "js.annotations.NativeFun";
    @NotNull
    private static final String NATIVE_CLASS_ANNOTATION_FQNAME = "js.annotations.NativeClass";

    private AnnotationsUtils() {
    }

    @NotNull
    public static String getNativeName(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof FunctionDescriptor) {
            return getNativeNameForFunction((FunctionDescriptor) descriptor);
        }
        if (descriptor instanceof ClassDescriptor) {
            return getNativeNameForClass((ClassDescriptor) descriptor);
        }
        throw new AssertionError("Use isNativeDeclaration to check");
    }

    @NotNull
    private static String getNativeNameForFunction(@NotNull FunctionDescriptor descriptor) {
        if (hasNativeFunctionAnnotation(descriptor)) {
            return getNativeFunctionName(descriptor);
        }
        if (declaredInNativeClass(descriptor)) {
            return descriptor.getName();
        }
        throw new AssertionError("Use isNativeFunction to check");
    }

    @NotNull
    private static String getNativeNameForClass(@NotNull ClassDescriptor descriptor) {
        if (hasNativeClassAnnotation(descriptor)) {
            return descriptor.getName();
        }
        throw new AssertionError("Use isNativeClass to check");
    }

    public static boolean isNativeDeclaration(@NotNull DeclarationDescriptor descriptor) {
        return hasNativeClassAnnotation(descriptor) || hasNativeFunctionAnnotation(descriptor)
                || declaredInNativeClass(descriptor);
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
        return hasNativeClassAnnotation(containingDeclaration);
    }

    //TODO: constants
    private static boolean hasNativeFunctionAnnotation(@NotNull DeclarationDescriptor descriptor) {
        return (getAnnotationByName(descriptor, NATIVE_FUNCTION_ANNOTATION_FQNAME) != null);
    }

    private static boolean hasNativeClassAnnotation(@NotNull DeclarationDescriptor descriptor) {
        return (getAnnotationByName(descriptor, NATIVE_CLASS_ANNOTATION_FQNAME) != null);
    }

    @NotNull
    private static String getNativeFunctionName(@NotNull FunctionDescriptor declarationDescriptor) {
        AnnotationDescriptor annotationDescriptor =
                getAnnotationByName(declarationDescriptor, NATIVE_FUNCTION_ANNOTATION_FQNAME);
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
