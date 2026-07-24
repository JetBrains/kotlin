/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRendererWithDiagnosticId
import org.jetbrains.kotlin.gradle.plugin.diagnostics.CompilerDiagnosticsProblemsReporter
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

private data class BufferedDiagnostic(
    val severity: CompilerMessageRenderer.Severity,
    val message: String,
    val location: CompilerMessageRenderer.SourceLocation?,
    val diagnosticId: String?,
)

/**
 * Collects compiler diagnostics emitted by the Build Tools API and replays them to the Gradle Problems API later.
 *
 * This indirection is required because `CompilerMessageRenderer.render` is not invoked on the Gradle worker thread.
 * Build Tools API executes in-process compilations on its own thread pool (see KT-81414), and daemon compilations
 * deliver messages on RMI dispatch threads. Gradle's `DefaultProblemReporter.report(Problem)` resolves the current
 * operation from `CurrentBuildOperationRef` (thread-local) and silently skips reporting when the thread has no
 * operation context (see https://github.com/gradle/gradle/issues/31274).
 *
 * To preserve reporting, diagnostics are buffered here and replayed on the Gradle worker thread after the
 * compilation operation completes.
 */
internal class ProblemsApiCompilerMessageRenderer(
    private val suppressLogForProblemsApi: Boolean = false,
) : CompilerMessageRendererWithDiagnosticId {
    private val bufferedDiagnostics = ConcurrentLinkedQueue<BufferedDiagnostic>()

    /**
     * Changes for all already received diagnostics error severity to warning.
     */
    @Synchronized
    fun lowerErrorSeveritiesToWarning() {
        val errors = bufferedDiagnostics.toList()

        bufferedDiagnostics.clear()
        bufferedDiagnostics.addAll(errors.map {
            if (it.severity == CompilerMessageRenderer.Severity.ERROR) {
                it.copy(severity = CompilerMessageRenderer.Severity.WARNING)
            } else {
                it
            }
        })
    }

    override fun render(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
        diagnosticId: String?,
    ): String {
        bufferedDiagnostics.add(BufferedDiagnostic(severity, message, location, diagnosticId))

        // When --warning-mode=all is active, Gradle's ConsoleProblemEmitter renders Problems API entries
        // as "Problem found:" blocks in the console. Returning an empty string here avoids duplicating
        // the warning that will already be rendered by Gradle via the Problems API replay (see replayTo).
        if (suppressLogForProblemsApi &&
            (severity == CompilerMessageRenderer.Severity.WARNING || severity == CompilerMessageRenderer.Severity.ERROR)
        ) {
            return ""
        }

        return buildString {
            location?.apply {
                val fileUri = File(path).toPath().toUri()
                append(fileUri)
                if (line > 0 && column > 0) {
                    append(":$line:$column")
                }
                append(' ')
            }
            append(message)
        }
    }

    fun replayTo(problemsReporter: CompilerDiagnosticsProblemsReporter) {
        while (true) {
            val diagnostic = bufferedDiagnostics.poll() ?: break
            problemsReporter.reportCompilerMessage(diagnostic.severity, diagnostic.message, diagnostic.location, diagnostic.diagnosticId)
        }
    }
}
