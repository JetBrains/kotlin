/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.logging.configuration.ShowStacktrace
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import java.io.Serializable

internal class ToolingDiagnosticRenderingOptions(
    val useParsableFormat: Boolean,
    val suppressedWarningIds: List<String>,
    val suppressedErrorIds: List<String>,
    val showStacktrace: Boolean,
) : Serializable {
    companion object {
        fun forProject(project: Project): ToolingDiagnosticRenderingOptions {
            return with(project.kotlinPropertiesProvider) {
                val showStacktrace = when {
                    // if the internal property is specified, it takes the highest priority
                    internalDiagnosticsShowStacktrace != null -> internalDiagnosticsShowStacktrace!!

                    // IDEA launches sync with `--stacktrace` option, but we don't want to
                    // spam stacktraces in build toolwindow
                    project.isInIdeaSync -> false

                    else -> project.gradle.startParameter.showStacktrace > ShowStacktrace.INTERNAL_EXCEPTIONS
                }

                ToolingDiagnosticRenderingOptions(
                    internalDiagnosticsUseParsableFormat,
                    suppressedGradlePluginWarnings,
                    suppressedGradlePluginErrors,
                    showStacktrace
                )
            }
        }
    }
}

internal fun ToolingDiagnostic.isSuppressed(options: ToolingDiagnosticRenderingOptions): Boolean {
    return when {
        // Non-suppressible
        id == KotlinToolingDiagnostics.InternalKotlinGradlePluginPropertiesUsed.id -> false

        severity == ToolingDiagnostic.Severity.WARNING -> id in options.suppressedWarningIds

        severity == ToolingDiagnostic.Severity.ERROR -> id in options.suppressedErrorIds

        // NB: FATALs can not be suppressed
        else -> false
    }
}
