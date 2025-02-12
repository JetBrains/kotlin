/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.logging.configuration.ShowStacktrace
import org.gradle.api.logging.configuration.WarningMode
import org.jetbrains.kotlin.gradle.internal.isInIdeaEnvironment
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.Serializable

internal class ToolingDiagnosticRenderingOptions(
    val useParsableFormat: Boolean,
    val suppressedWarningIds: List<String>,
    val suppressedErrorIds: List<String>,
    val showStacktrace: Boolean,
    val showSeverityEmoji: Boolean,
    val coloredOutput: Boolean,
    val ignoreWarningMode: Boolean,
    val warningMode: WarningMode
) : Serializable {
    companion object {
        fun forProject(project: Project): ToolingDiagnosticRenderingOptions {
            return with(project.kotlinPropertiesProvider) {
                val showStacktrace = when {
                    // if the internal property is specified, it takes the highest priority
                    internalDiagnosticsShowStacktrace != null -> internalDiagnosticsShowStacktrace!!

                    // IDEA launches sync with `--stacktrace` option, but we don't want to
                    // spam stacktraces in build toolwindow
                    project.isInIdeaSync.get() -> false

                    else -> project.gradle.startParameter.showStacktrace > ShowStacktrace.INTERNAL_EXCEPTIONS
                }

                ToolingDiagnosticRenderingOptions(
                    useParsableFormat = internalDiagnosticsUseParsableFormat,
                    suppressedWarningIds = suppressedGradlePluginWarnings,
                    suppressedErrorIds = suppressedGradlePluginErrors,
                    showStacktrace = showStacktrace,
                    showSeverityEmoji = !project.isInIdeaEnvironment.get() && !HostManager.hostIsMingw,
                    coloredOutput = project.showColoredDiagnostics(),
                    ignoreWarningMode = internalDiagnosticsIgnoreWarningMode ?: false,
                    warningMode = project.gradle.startParameter.warningMode
                )
            }
        }
    }
}

private fun Project.showColoredDiagnostics(): Boolean {
    // Based on Gradle's console output mode, determine if we should use colors
    return when (gradle.startParameter.consoleOutput) {
        // In Auto mode, check if we're in a terminal that supports colors
        ConsoleOutput.Auto -> isAttachedToTerminal()
        // Plain mode explicitly disables colors
        ConsoleOutput.Plain -> false
        // Rich and Verbose modes force colors on regardless of terminal
        ConsoleOutput.Rich, ConsoleOutput.Verbose -> true
        // Enum argument can be null in Java
        else -> false
    }
}

private fun isAttachedToTerminal(): Boolean {
    // Check various environment variables that indicate terminal capabilities
    val term = System.getenv("TERM")              // Basic terminal type
    val colorTerm = System.getenv("COLORTERM")    // Explicit color support flag
    val termProgram = System.getenv("TERM_PROGRAM") // Terminal emulator program

    // Check multiple indicators of a terminal that supports colors:
    // - TERM exists and isn't "dumb" (basic terminal)
    // - COLORTERM exists (explicit color support)
    // - TERM_PROGRAM exists (modern terminal emulator)
    return (term != null && term != "dumb") ||
            colorTerm != null ||
            termProgram != null
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
