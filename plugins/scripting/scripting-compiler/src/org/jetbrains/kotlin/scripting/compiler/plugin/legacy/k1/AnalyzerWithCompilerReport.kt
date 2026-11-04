/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.legacy.k1

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.SyntaxErrorReporter
import org.jetbrains.kotlin.cli.common.messages.SyntaxErrorReporter.SyntaxErrorReport
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.SimpleDiagnostic
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages

@K1Deprecation
object AnalyzerWithCompilerReport {
    private class MyDiagnostic<E : PsiElement>(
        psiElement: E, factory: DiagnosticFactory0<E>, val message: String,
    ) : SimpleDiagnostic<E>(psiElement, factory, ERROR) {

        override val isValid: Boolean = true
    }

    private val SYNTAX_ERROR_FACTORY = DiagnosticFactory0.create<PsiErrorElement>(ERROR)

    private fun reportDiagnostic(diagnostic: Diagnostic, reporter: DiagnosticMessageReporter, renderDiagnosticName: Boolean): Boolean {
        if (!diagnostic.isValid) return false

        val message = (diagnostic as? MyDiagnostic<*>)?.message ?: DefaultErrorMessages.render(diagnostic)
        val diagnosticFactoryName = diagnostic.factoryNameOrNull()
        val textToRender = when (renderDiagnosticName) {
            true -> diagnosticFactoryName?.let { "[$it] $message" } ?: message
            false -> message
        }

        reporter.report(
            diagnostic,
            diagnostic.psiFile,
            textToRender
        )

        return diagnostic.severity == ERROR
    }

    // Reports K1 diagnostics ([org.jetbrains.kotlin.diagnostics.SimpleDiagnostic])
    fun reportSyntaxErrors(file: PsiElement, reporter: DiagnosticMessageReporter): SyntaxErrorReport {
        return SyntaxErrorReporter.reportSyntaxErrors(file) { element, message ->
            val diagnostic = MyDiagnostic(element, SYNTAX_ERROR_FACTORY, message)
            reportDiagnostic(diagnostic, reporter, renderDiagnosticName = false)
        }
    }

    fun reportSyntaxErrors(file: PsiElement, messageCollector: MessageCollector): SyntaxErrorReport {
        return reportSyntaxErrors(file, DefaultDiagnosticReporter(messageCollector))
    }
}
