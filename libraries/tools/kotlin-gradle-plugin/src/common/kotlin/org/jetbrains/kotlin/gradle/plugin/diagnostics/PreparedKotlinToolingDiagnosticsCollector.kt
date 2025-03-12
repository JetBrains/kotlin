/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.logging.Logger

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
            PreparedForConfigurationPhaseDiagnosticCollector(project)
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
    private val project: Project
) : PreparedKotlinToolingDiagnosticsCollector(project.kotlinToolingDiagnosticsCollector) {
    override fun report(diagnostic: ToolingDiagnostic, reportOnce: Boolean, key: String) {
        collector.report(project, diagnostic, reportOnce, key)
    }
}
