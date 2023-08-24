/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.transform.TransformSpec
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

internal interface UsesKotlinToolingDiagnostics : Task {
    @get:Internal
    val toolingDiagnosticsCollector: Property<KotlinToolingDiagnosticsCollector>

    @get:Internal
    val diagnosticRenderingOptions: Property<ToolingDiagnosticRenderingOptions>

    fun reportDiagnostic(diagnostic: ToolingDiagnostic) {
        toolingDiagnosticsCollector.get().report(this, diagnostic)
    }
}

/**
 * Use this interface to make your [TransformAction] able to report [ToolingDiagnostic].
 * You must use [setupTransformActionToolingDiagnostics] to set up [TransformActionUsingKotlinToolingDiagnostics.Parameters].
 */
internal interface TransformActionUsingKotlinToolingDiagnostics<P : TransformActionUsingKotlinToolingDiagnostics.Parameters> :
    TransformAction<P> {
    interface Parameters : TransformParameters {
        @get:Internal
        val toolingDiagnosticsCollector: Property<KotlinToolingDiagnosticsCollector>

        @get:Internal
        val diagnosticRenderingOptions: Property<ToolingDiagnosticRenderingOptions>
    }

    fun reportDiagnostic(diagnostic: ToolingDiagnostic) {
        parameters.toolingDiagnosticsCollector.get().report(this, logger, diagnostic)
    }

    companion object {
        internal val logger: Logger = Logging.getLogger(Project::class.java)
    }
}

internal fun TransformSpec<out TransformActionUsingKotlinToolingDiagnostics.Parameters>.setupTransformActionToolingDiagnostics(project: Project) {
    parameters.toolingDiagnosticsCollector.set(project.kotlinToolingDiagnosticsCollectorProvider)
    parameters.diagnosticRenderingOptions.set(ToolingDiagnosticRenderingOptions.forProject(project))
}