package org.jetbrains.k2js.translate.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getContainingClass;

/**
 * @author Pavel Talanov
 */
public final class AnnotationsUtils {

    @NotNull
    public static final String NATIVE_ANNOTATION_FQNAME = "js.annotations.native";
    @NotNull
    public static final String LIBRARY_ANNOTATION_FQNAME = "js.annotations.library";

    private AnnotationsUtils() {
    }

    //TODO: make public, use when necessary
    private static boolean hasAnnotation(@NotNull DeclarationDescriptor descriptor,
                                         @NotNull String annotationFQNAme) {
        return getAnnotationByName(descriptor, annotationFQNAme) != null;
    }

    @NotNull
    public static String getAnnotationStringParameter(@NotNull DeclarationDescriptor declarationDescriptor,
                                                      @NotNull String annotationFQName) {
        AnnotationDescriptor annotationDescriptor =
                getAnnotationByName(declarationDescriptor, annotationFQName);
        assert annotationDescriptor != null;
        //TODO: this is a quick fix for unsupported default args problem
        if (annotationDescriptor.getValueArguments().isEmpty()) {
            return "";
        }
        CompileTimeConstant<?> constant = annotationDescriptor.getValueArguments().iterator().next();
        //TODO: this is a quick fix for unsupported default args problem
        if (constant == null) {
            return "";
        }
        Object value = constant.getValue();
        assert value instanceof String : "Native function annotation should have one String parameter";
        return (String) value;
    }

    @Nullable
    public static AnnotationDescriptor getAnnotationByName(@NotNull DeclarationDescriptor descriptor,
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

    public static boolean isNativeObject(@NotNull DeclarationDescriptor descriptor) {
        if (getAnnotationByName(descriptor, NATIVE_ANNOTATION_FQNAME) != null) {
            return true;
        }
        ClassDescriptor containingClass = getContainingClass(descriptor);
        if (containingClass == null) {
            return false;
        }
        return (getAnnotationByName(containingClass, NATIVE_ANNOTATION_FQNAME) != null);
    }
}
