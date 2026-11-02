/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.KtIoFileSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.report
import java.io.File

internal class TypeScriptExportContext(
    val config: TypeScriptExportConfig,
    val diagnosticReporter: DiagnosticReporter,
) : DiagnosticContext {
    override var containingFile: KtSourceFile? = null
    override val languageVersionSettings: LanguageVersionSettings get() = LanguageVersionSettingsImpl.DEFAULT

    override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean = false

    /**
     * KLIBs don't contain the exact source positions of the declarations, so the best location we can provide is the source file
     * of the top-level declaration that is currently being exported.
     */
    fun <T> withSourceFile(sourceFileName: String?, block: () -> T): T {
        val previousFile = containingFile
        containingFile = sourceFileName?.let { KtIoFileSourceFile(File(it)) }
        try {
            return block()
        } finally {
            containingFile = previousFile
        }
    }

    fun report(factory: KtSourcelessDiagnosticFactory, message: String = "") {
        diagnosticReporter.report(factory, message)
    }
}
