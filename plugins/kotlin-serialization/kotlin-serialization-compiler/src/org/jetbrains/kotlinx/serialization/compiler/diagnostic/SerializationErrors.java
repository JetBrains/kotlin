/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;
import static org.jetbrains.kotlin.diagnostics.Severity.WARNING;

public interface SerializationErrors {
    DiagnosticFactory0<PsiElement> INLINE_CLASSES_NOT_SUPPORTED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> PLUGIN_IS_NOT_ENABLED = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<PsiElement> EXPLICIT_SERIALIZABLE_IS_REQUIRED = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtAnnotationEntry> SERIALIZABLE_ANNOTATION_IGNORED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, String> DUPLICATE_SERIAL_NAME = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> SERIALIZER_NOT_FOUND = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, KotlinType, KotlinType> SERIALIZER_NULLABILITY_INCOMPATIBLE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<PsiElement> TRANSIENT_MISSING_INITIALIZER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> TRANSIENT_IS_REDUNDANT = DiagnosticFactory0.create(WARNING);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(SerializationErrors.class);
        }
    };

}