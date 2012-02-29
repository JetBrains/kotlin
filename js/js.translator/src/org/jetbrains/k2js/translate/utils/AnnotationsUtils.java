/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    public static final String NATIVE_ANNOTATION_FQNAME = "js.native";
    @NotNull
    public static final String LIBRARY_ANNOTATION_FQNAME = "js.library";

    private AnnotationsUtils() {
    }

    //TODO: make public, use when necessary
    private static boolean hasAnnotation(@NotNull DeclarationDescriptor descriptor,
                                         @NotNull String annotationFQNAme) {
        return getAnnotationByName(descriptor, annotationFQNAme) != null;
    }

    @Nullable
    public static String getAnnotationStringParameter(@NotNull DeclarationDescriptor declarationDescriptor,
                                                      @NotNull String annotationFQName) {
        AnnotationDescriptor annotationDescriptor =
                getAnnotationByName(declarationDescriptor, annotationFQName);
        assert annotationDescriptor != null;
        //TODO: this is a quick fix for unsupported default args problem
        if (annotationDescriptor.getValueArguments().isEmpty()) {
            return null;
        }
        CompileTimeConstant<?> constant = annotationDescriptor.getValueArguments().iterator().next();
        //TODO: this is a quick fix for unsupported default args problem
        if (constant == null) {
            return null;
        }
        Object value = constant.getValue();
        assert value instanceof String : "Native function annotation should have one String parameter";
        return (String) value;
    }

    @Nullable
    public static String getNameForAnnotatedObject(@NotNull DeclarationDescriptor declarationDescriptor,
                                                   @NotNull String annotationFQName) {
        if (!hasAnnotation(declarationDescriptor, annotationFQName)) {
            return null;
        }
        return getAnnotationStringParameter(declarationDescriptor, annotationFQName);
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
        return hasAnnotationOrInsideAnnotatedClass(descriptor, NATIVE_ANNOTATION_FQNAME);
    }

    public static boolean isLibraryObject(@NotNull DeclarationDescriptor descriptor) {
        return hasAnnotationOrInsideAnnotatedClass(descriptor, LIBRARY_ANNOTATION_FQNAME);
    }

    public static boolean isPredefinedObject(@NotNull DeclarationDescriptor descriptor) {
        return isLibraryObject(descriptor) || isNativeObject(descriptor);
    }

    public static boolean hasAnnotationOrInsideAnnotatedClass(@NotNull DeclarationDescriptor descriptor,
                                                              @NotNull String annotationFQName) {
        if (getAnnotationByName(descriptor, annotationFQName) != null) {
            return true;
        }
        ClassDescriptor containingClass = getContainingClass(descriptor);
        if (containingClass == null) {
            return false;
        }
        return (getAnnotationByName(containingClass, annotationFQName) != null);
    }
}
