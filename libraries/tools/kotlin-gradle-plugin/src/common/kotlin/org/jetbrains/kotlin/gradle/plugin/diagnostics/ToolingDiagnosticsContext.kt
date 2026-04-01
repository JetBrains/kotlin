/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.utils.getOrPut
import java.io.Serializable

/**
 * Immutable diagnostics metadata extracted from a Gradle [Project].
 *
 * The context can be safely passed through task/build-service parameters without keeping
 * a direct reference to [Project].
 */
internal data class ToolingDiagnosticsContext(
    /** Gradle path of the project that reports diagnostics (for example, `:app`). */
    val projectPath: String,
    /** Human-readable project name used in diagnostic messages. */
    val projectName: String,
    /** Rendering settings resolved for this project. */
    val renderingOptions: ToolingDiagnosticRenderingOptions,
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 1L

        /**
         * Builds a [ToolingDiagnosticsContext] from [project].
         */
        fun fromProject(project: Project): ToolingDiagnosticsContext {
            return ToolingDiagnosticsContext(
                projectPath = project.path,
                projectName = project.name,
                renderingOptions = ToolingDiagnosticRenderingOptions.forProject(project),
            )
        }
    }
}

private const val TOOLING_DIAGNOSTICS_CONTEXT_EXTRA_KEY = "kotlin.tooling.diagnostics.context"

/**
 * Returns diagnostics context derived from this [Project].
 *
 * The value is computed once and cached in project's extra properties to avoid repeated
 * value source evaluations while still keeping the context project-free.
 */
internal val Project.toolingDiagnosticsContext: ToolingDiagnosticsContext
    get() = extensions.extraProperties.getOrPut(TOOLING_DIAGNOSTICS_CONTEXT_EXTRA_KEY) {
        ToolingDiagnosticsContext.fromProject(this)
    }
