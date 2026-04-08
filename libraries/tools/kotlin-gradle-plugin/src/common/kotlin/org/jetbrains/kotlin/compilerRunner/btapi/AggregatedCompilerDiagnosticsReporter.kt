/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.gradle.plugin.BuildFinishedListenerService
import org.jetbrains.kotlin.gradle.plugin.diagnostics.CompilerDiagnosticsProblemsReporter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class AggregatedCompilerDiagnosticsReporter(
    private val buildId: UUID,
    private val buildFinishedListenerService: BuildFinishedListenerService,
    private val delegate: CompilerDiagnosticsProblemsReporter,
) : CompilerDiagnosticsProblemsReporter {

    override fun reportCompilerMessage(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
        taskPaths: Collection<String>,
    ) {
        AggregatedCompilerDiagnosticsStore.record(buildId, severity, message, location, taskPaths)
        buildFinishedListenerService.onCloseOnceByKey("aggregated-compiler-diagnostics:$buildId") {
            AggregatedCompilerDiagnosticsStore.remove(buildId).orEmpty()
                .sortedBy { diagnostic -> diagnostic.sortKey }
                .forEach { diagnostic ->
                    delegate.reportCompilerMessage(
                        severity = diagnostic.severity,
                        message = diagnostic.message,
                        location = diagnostic.location,
                        taskPaths = diagnostic.taskPaths.sorted(),
                    )
                }
        }
    }
}

private object AggregatedCompilerDiagnosticsStore {
    private val diagnosticsByBuild = ConcurrentHashMap<UUID, ConcurrentHashMap<DiagnosticKey, AggregatedDiagnostic>>()

    fun record(
        buildId: UUID,
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
        taskPaths: Collection<String>,
    ) {
        val diagnosticsForBuild = diagnosticsByBuild.computeIfAbsent(buildId) { ConcurrentHashMap() }
        val diagnostic = diagnosticsForBuild.computeIfAbsent(DiagnosticKey(severity, message, location?.toKey())) {
            AggregatedDiagnostic(severity, message, location)
        }

        taskPaths.filter { it.isNotBlank() }.forEach(diagnostic.taskPaths::add)
    }

    fun remove(buildId: UUID): Collection<AggregatedDiagnostic>? =
        diagnosticsByBuild.remove(buildId)?.values

    private data class DiagnosticKey(
        val severity: CompilerMessageRenderer.Severity,
        val message: String,
        val location: SourceLocationKey?,
    )

    private data class SourceLocationKey(
        val path: String,
        val line: Int,
        val column: Int,
        val lineEnd: Int,
        val columnEnd: Int,
        val lineContent: String?,
    )

    class AggregatedDiagnostic(
        val severity: CompilerMessageRenderer.Severity,
        val message: String,
        val location: CompilerMessageRenderer.SourceLocation?,
    ) {
        val taskPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()

        val sortKey: String
            get() = buildString {
                append(severity.name)
                append('\u0000')
                append(message)
                append('\u0000')
                append(location?.path.orEmpty())
                append('\u0000')
                append(location?.line ?: -1)
                append('\u0000')
                append(location?.column ?: -1)
            }
    }

    private fun CompilerMessageRenderer.SourceLocation.toKey() = SourceLocationKey(
        path = path,
        line = line,
        column = column,
        lineEnd = lineEnd,
        columnEnd = columnEnd,
        lineContent = lineContent,
    )
}
