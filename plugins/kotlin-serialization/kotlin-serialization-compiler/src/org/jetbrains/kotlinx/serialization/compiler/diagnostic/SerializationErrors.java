/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;

import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;

public interface SerializationErrors {
    DiagnosticFactory0<KtAnnotationEntry> SERIALIZABLE_ANNOTATION_IGNORED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, String> DUPLICATE_SERIAL_NAME = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> SERIALIZER_NOT_FOUND = DiagnosticFactory0.create(ERROR);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(SerializationErrors.class);
        }
    };

}