/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.tasks.CompilationErrorException
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal interface CompilerDiagnosticsProblemsReporter {
    fun reportCompilerMessage(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
        diagnosticId: String?,
    )

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(objects: ObjectFactory): CompilerDiagnosticsProblemsReporter
    }
}

internal abstract class DefaultCompilerDiagnosticsProblemsReporter @Inject constructor(
    private val problems: Problems,
) : CompilerDiagnosticsProblemsReporter {
    private val logger: Logger = Logging.getLogger(this.javaClass)

    override fun reportCompilerMessage(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
        diagnosticId: String?,
    ) {
        val diagnosticGroup = severity.toDiagnosticGroup()
        val problemId = ProblemId.create(
            severity.resolvedProblemId(diagnosticId),
            severity.resolvedDisplayName(diagnosticId),
            diagnosticGroup.toProblemGroup(),
        )

        fun ProblemSpec.populateSpec() = contextualLabel(severity.toDisplayName())
            .details(message)
            .applySourceLocation(location)
            .additionalData(KotlinCompilerDiagnosticAdditionalData::class.java) { data ->
                data.severity.value(severity).finalizeValue()
            }

        try {
            when (severity) {
                // It is expected that error messages are coming within task action, and it is ok to throw failing the build
                // Anyway compilation task will fail
                CompilerMessageRenderer.Severity.ERROR -> problems.reporter
                    .throwing(CompilationErrorException(message), problemId) {
                        it.populateSpec()
                    }
                CompilerMessageRenderer.Severity.WARNING -> problems.reporter.report(problemId) {
                    it.populateSpec()
                }
                else -> Unit
            }
        } catch (e: NoSuchMethodError) {
            logger.error("Can't invoke reporter method:", e)
        }
    }

    class Factory : CompilerDiagnosticsProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): CompilerDiagnosticsProblemsReporter {
            return objects.newInstance<DefaultCompilerDiagnosticsProblemsReporter>()
        }
    }
}

internal abstract class NoOpCompilerDiagnosticsProblemsReporter : CompilerDiagnosticsProblemsReporter {
    override fun reportCompilerMessage(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
        diagnosticId: String?,
    ) {
    }

    class Factory : CompilerDiagnosticsProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): CompilerDiagnosticsProblemsReporter {
            return objects.newInstance<NoOpCompilerDiagnosticsProblemsReporter>()
        }
    }
}

internal val CompilerMessageRenderer.Severity.problemId: String
    get() = when (this) {
        CompilerMessageRenderer.Severity.ERROR -> "compiler-error"
        CompilerMessageRenderer.Severity.WARNING -> "compiler-warning"
        CompilerMessageRenderer.Severity.INFO -> "compiler-info"
        CompilerMessageRenderer.Severity.DEBUG -> "compiler-debug"
    }

internal fun CompilerMessageRenderer.Severity.toDiagnosticGroup(): DiagnosticGroup = when (this) {
    CompilerMessageRenderer.Severity.ERROR -> DiagnosticGroup.Compiler.Error
    CompilerMessageRenderer.Severity.WARNING -> DiagnosticGroup.Compiler.Warning
    CompilerMessageRenderer.Severity.INFO, CompilerMessageRenderer.Severity.DEBUG -> DiagnosticGroup.Compiler.Default
}

internal fun CompilerMessageRenderer.Severity.toDisplayName(): String = when (this) {
    CompilerMessageRenderer.Severity.ERROR -> "Kotlin compiler error"
    CompilerMessageRenderer.Severity.WARNING -> "Kotlin compiler warning"
    CompilerMessageRenderer.Severity.INFO -> "Kotlin compiler info"
    CompilerMessageRenderer.Severity.DEBUG -> "Kotlin compiler debug"
}

internal fun CompilerMessageRenderer.Severity.resolvedProblemId(diagnosticId: String?): String =
    diagnosticId ?: problemId

internal fun CompilerMessageRenderer.Severity.resolvedDisplayName(diagnosticId: String?): String =
    diagnosticId?.let { "Kotlin compiler: $it" } ?: toDisplayName()

internal fun ProblemSpec.applySourceLocation(location: CompilerMessageRenderer.SourceLocation?): ProblemSpec {
    if (location == null) return this
    val path = location.path
    val line = location.line
    val column = location.column

    return when {
        line > 0 && column > 0 -> lineInFileLocation(path, line, column, location.locationLength)
        line > 0 -> lineInFileLocation(path, line)
        else -> fileLocation(path)
    }
}

internal val CompilerMessageRenderer.SourceLocation.locationLength: Int
    get() {
        val length = if (line == lineEnd && columnEnd > column) {
            columnEnd - column
        } else {
            1
        }
        return length.coerceAtLeast(1)
    }
