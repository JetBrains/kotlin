/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal

internal interface UsesKotlinToolingDiagnosticsParameters {
    @get:Internal
    val toolingDiagnosticsCollector: Property<KotlinToolingDiagnosticsCollector>

    @get:Internal
    val diagnosticRenderingOptions: Property<ToolingDiagnosticRenderingOptions>
}

internal interface UsesKotlinToolingDiagnostics : UsesKotlinToolingDiagnosticsParameters, Task {
    fun reportDiagnostic(diagnostic: ToolingDiagnostic) {
        toolingDiagnosticsCollector.get().report(this, diagnostic)
    }
}

/**
 * Use this interface to make your [TransformAction] able to report [ToolingDiagnostic].
 * You must use [setupKotlinToolingDiagnosticsParameters] to set up the parameters.
 */
internal interface TransformActionUsingKotlinToolingDiagnostics<P : TransformActionUsingKotlinToolingDiagnostics.Parameters> :
    TransformAction<P> {
    interface Parameters : TransformParameters, UsesKotlinToolingDiagnosticsParameters

    fun reportDiagnostic(diagnostic: ToolingDiagnostic) {
        parameters.toolingDiagnosticsCollector.get().report(parameters, logger, diagnostic)
    }

    companion object {
        internal val logger: Logger = Logging.getLogger(Project::class.java)
    }
}

/**
 * Use this interface to make your [BuildService] able to report [ToolingDiagnostic].
 * You must use [setupKotlinToolingDiagnosticsParameters] to set up the parameters.
 */
internal interface BuildServiceUsingKotlinToolingDiagnostics<P : BuildServiceUsingKotlinToolingDiagnostics.Parameters> :
    BuildService<P> {
    interface Parameters : BuildServiceParameters, UsesKotlinToolingDiagnosticsParameters

    fun reportDiagnostic(diagnostic: ToolingDiagnostic) {
        parameters.toolingDiagnosticsCollector.get().report(parameters, logger, diagnostic)
    }

    companion object {
        internal val logger: Logger = Logging.getLogger(Project::class.java)
    }
}

internal fun UsesKotlinToolingDiagnosticsParameters.setupKotlinToolingDiagnosticsParameters(project: Project) {
    toolingDiagnosticsCollector.set(project.kotlinToolingDiagnosticsCollectorProvider)
    diagnosticRenderingOptions.set(ToolingDiagnosticRenderingOptions.forProject(project))
}