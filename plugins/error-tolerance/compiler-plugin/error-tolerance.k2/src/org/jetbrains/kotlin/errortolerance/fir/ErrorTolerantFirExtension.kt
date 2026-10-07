/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.errortolerance.fir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.fir.backend.FirErroneousCodePlan
import org.jetbrains.kotlin.fir.backend.FirErrorTolerantCompilationExtension
import org.jetbrains.kotlin.fir.declarations.FirFile

class ErrorTolerantFirExtension : FirErrorTolerantCompilationExtension() {
    override fun computeErroneousCodePlan(
        files: List<FirFile>,
        errorsByFile: Map<KtSourceFile?, List<KtDiagnostic>>,
    ): FirErroneousCodePlan = ErroneousCodePlanBuilder(files, errorsByFile).build()
}
