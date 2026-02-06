/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project

/**
 * Facade for [KotlinToolingDiagnosticsCollector] that simplifies diagnostic reporting to just one method [report]
 *
 * Useful when some code can be called during configuration and execution phases.
 */
internal abstract class PreparedKotlinToolingDiagnosticsCollector(
    protected val collector: KotlinToolingDiagnosticsCollector
) {
    abstract fun report(diagnostic: ToolingDiagnostic, reportOnce: Boolean = true, key: String = diagnostic.id)

    companion object {
        fun create(parameters: UsesKotlinToolingDiagnosticsParameters): PreparedKotlinToolingDiagnosticsCollector =
            PreparedForExecutionPhaseDiagnosticsCollector(parameters)

        fun create(project: Project): PreparedKotlinToolingDiagnosticsCollector =
            PreparedForConfigurationPhaseDiagnosticCollector(
                project.path,
                ToolingDiagnosticRenderingOptions.forProject(project),
                project.kotlinToolingDiagnosticsCollector,
            )
    }
}

private class PreparedForExecutionPhaseDiagnosticsCollector(
    private val kotlinToolingDiagnosticsParameters: UsesKotlinToolingDiagnosticsParameters
) : PreparedKotlinToolingDiagnosticsCollector(kotlinToolingDiagnosticsParameters.toolingDiagnosticsCollector.get()) {
    override fun report(diagnostic: ToolingDiagnostic, reportOnce: Boolean, key: String) {
        collector.report(kotlinToolingDiagnosticsParameters, diagnostic, reportOnce, key)
    }
}

private class PreparedForConfigurationPhaseDiagnosticCollector(
    private val projectPath: String,
    private val renderingOptions: ToolingDiagnosticRenderingOptions,
    collector: KotlinToolingDiagnosticsCollector,
) : PreparedKotlinToolingDiagnosticsCollector(collector) {
    override fun report(diagnostic: ToolingDiagnostic, reportOnce: Boolean, key: String) {
        collector.report(projectPath, renderingOptions, diagnostic, reportOnce, key)
    }
}
