/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.utils;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource;
import org.jetbrains.kotlin.descriptors.SourceFile;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.source.PsiSourceFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnnotationsUtils {

    private AnnotationsUtils() {
    }

    @NotNull
    public static List<AnnotationDescriptor> getContainingFileAnnotations(
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor descriptor
    ) {
        KtFile kotlinFile = getFile(descriptor);
        if (kotlinFile != null) {
            List<AnnotationDescriptor> annotations = new ArrayList<>();
            for (KtAnnotationEntry psiAnnotation : kotlinFile.getAnnotationEntries()) {
                AnnotationDescriptor annotation = bindingContext.get(BindingContext.ANNOTATION, psiAnnotation);
                if (annotation != null) {
                    annotations.add(annotation);
                }
            }
            return annotations;
        }

        return Collections.emptyList();
    }

    @Nullable
    private static KtFile getFile(DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof DeclarationDescriptorWithSource)) return null;
        SourceFile file = ((DeclarationDescriptorWithSource) descriptor).getSource().getContainingFile();
        if (!(file instanceof PsiSourceFile)) return null;

        PsiFile psiFile = ((PsiSourceFile) file).getPsiFile();
        if (!(psiFile instanceof KtFile)) return null;

        return (KtFile) psiFile;
    }
}
