/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.resolve.diagnostics;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.diagnostics.*;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;

public interface ErrorsWasm {
    DiagnosticFactory1<KtElement, KotlinType> NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE =
            DiagnosticFactory1.create(ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory2<PsiElement, String, KotlinType>
            WRONG_JS_INTEROP_TYPE = DiagnosticFactory2.create(ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory0<PsiElement> NESTED_WASM_IMPORT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> WASM_IMPORT_PARAMETER_DEFAULT_VALUE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> WASM_IMPORT_VARARG_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> WASM_IMPORT_UNSUPPORTED_PARAMETER_TYPE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> WASM_IMPORT_UNSUPPORTED_RETURN_TYPE = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<PsiElement> WRONG_JS_FUN_TARGET = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> JSCODE_WRONG_CONTEXT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> JSCODE_UNSUPPORTED_FUNCTION_KIND = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> JSCODE_INVALID_PARAMETER_NAME = DiagnosticFactory0.create(ERROR);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(ErrorsWasm.class);
        }
    };
}
