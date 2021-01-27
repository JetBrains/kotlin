/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.generator

import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.getGenerationPath
import org.jetbrains.kotlin.idea.frontend.api.fir.generator.rendererrs.FirDiagnosticToKtDiagnosticConverterRenderer
import org.jetbrains.kotlin.idea.frontend.api.fir.generator.rendererrs.KtDiagnosticClassImplementationRenderer
import org.jetbrains.kotlin.idea.frontend.api.fir.generator.rendererrs.KtDiagnosticClassRenderer
import java.nio.file.Path

object DiagnosticClassGenerator {
    fun generate(rootPath: Path, diagnosticList: DiagnosticList, packageName: String) {
        val path = getGenerationPath(rootPath.toFile(), packageName)
        KtDiagnosticClassRenderer.render(path.resolve("KtFirDiagnostics.kt"), diagnosticList, packageName)
        KtDiagnosticClassImplementationRenderer.render(path.resolve("KtFirDiagnosticsImpl.kt"), diagnosticList, packageName)
        FirDiagnosticToKtDiagnosticConverterRenderer.render(path.resolve("KtFirDataClassConverters.kt"), diagnosticList, packageName)
    }
}