package org.jetbrains.k2js.translate.context.declaration;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;

import java.util.Set;

/**
 * @author Pavel Talanov
 */
public final class AnnotationsUtils {

    @NotNull
    public static final String NATIVE_FUNCTION_ANNOTATION_FQNAME = "js.annotations.NativeFun";
    @NotNull
    public static final String NATIVE_CLASS_ANNOTATION_FQNAME = "js.annotations.NativeClass";
    @NotNull
    public static final String LIBRARY_FUNCTION_ANNOTATION_FQNAME = "js.annotations.LibraryFun";
    @NotNull
    public static final String LIBRARY_CLASS_ANNOTATION_FQNAME = "js.annotations.LibraryClass";
    @NotNull
    public static Set<String> INTERNAL_ANNOTATIONS_FQNAMES = Sets.newHashSet(
            NATIVE_CLASS_ANNOTATION_FQNAME, NATIVE_FUNCTION_ANNOTATION_FQNAME,
            LIBRARY_CLASS_ANNOTATION_FQNAME, LIBRARY_FUNCTION_ANNOTATION_FQNAME);

    private AnnotationsUtils() {
    }

    public static boolean doesNotHaveInternalAnnotations(@NotNull DeclarationDescriptor descriptor) {
        for (String annotationFQNAme : INTERNAL_ANNOTATIONS_FQNAMES) {
            if (hasAnnotation(descriptor, annotationFQNAme)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasAnnotation(@NotNull DeclarationDescriptor descriptor,
                                         @NotNull String annotationFQNAme) {
        return getAnnotationByName(descriptor, annotationFQNAme) != null;
    }

    @NotNull
    public static String annotationStringParameter(@NotNull FunctionDescriptor declarationDescriptor,
                                                   @NotNull String annotationFQName) {
        AnnotationDescriptor annotationDescriptor =
                getAnnotationByName(declarationDescriptor, annotationFQName);
        assert annotationDescriptor != null;
        Object value = annotationDescriptor.getValueArguments().iterator().next().getValue();
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
}
