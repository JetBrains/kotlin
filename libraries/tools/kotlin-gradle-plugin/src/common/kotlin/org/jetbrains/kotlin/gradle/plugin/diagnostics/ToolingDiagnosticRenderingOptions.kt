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
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.ERROR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.STRONG_WARNING
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.WARNING
import org.jetbrains.kotlin.gradle.utils.ConfigurationCacheOpaqueValueSource
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
    val warningMode: WarningMode,
    val displayDiagnosticsInIdeBuildLog: Boolean,
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
                    ignoreWarningMode = internalDiagnosticsIgnoreWarningMode == true,
                    warningMode = project.gradle.startParameter.warningMode,
                    displayDiagnosticsInIdeBuildLog = project.kotlinPropertiesProvider.displayDiagnosticsInIdeBuildLog && project.isInIdeaEnvironment.get(),
                )
            }
        }
    }

    fun effectiveSeverity(severity: ToolingDiagnostic.Severity): ToolingDiagnostic.Severity? {
        return if (ignoreWarningMode) {
            severity
        } else {
            // Early return if warnings are disabled and it's not an error and not fatal
            if (warningMode == WarningMode.None && severity == WARNING) {
                return null
            }

            return if (severity == WARNING && warningMode == WarningMode.Fail)
                ERROR
            else
                severity

            //TODO: KT-74986 Support WarningMode.Summary mode for gradle diagnostics
        }
    }
}

private fun Project.showColoredDiagnostics(): Boolean {
    // Based on Gradle's console output mode, determine if we should use colors
    return when (gradle.startParameter.consoleOutput) {
        // In Auto mode, check if we're in a terminal that supports colors
        ConsoleOutput.Auto -> isAttachedToTerminal.get()
        // Plain mode explicitly disables colors
        ConsoleOutput.Plain -> false
        // Colored, Rich and Verbose modes force colors on regardless of terminal
        ConsoleOutput.Colored, ConsoleOutput.Rich, ConsoleOutput.Verbose -> true
        // Enum argument can be null in Java
        else -> false
    }
}

private val Project.isAttachedToTerminal
    get() = providers.of(IsAttachedToTerminalValueSource::class.java) {}.map { it.value }

/**
 * Configuration cache value source that determines if the application is running in an
 * interactive terminal with advanced capabilities across different platforms.
 */
private abstract class IsAttachedToTerminalValueSource : ConfigurationCacheOpaqueValueSource<Boolean>("isAttachedToTerminal") {
    override fun obtainValue(): Boolean {
        // Unix/Linux/macOS terminal detection
        val term = System.getenv("TERM")              // Standard UNIX environment variable
        val colorTerm = System.getenv("COLORTERM")    // Explicit color support flag, e.g., "truecolor"
        val termProgram = System.getenv("TERM_PROGRAM") // Terminal emulator program, e.g., "vscode", "iTerm.app"

        // Common terminal types:
        // - "dumb": Basic terminal with minimal features (often in CI environments or redirected output)
        // - "xterm", "xterm-256color": Standard terminal types with good feature support
        // - Terminal emulators like "iTerm.app", "Apple_Terminal" will set TERM_PROGRAM

        // Windows-specific terminal detection
        val ansicon = System.getenv("ANSICON")        // Set by ANSICON and similar Windows terminal enhancers
        val conEmuANSI = System.getenv("ConEmuANSI")  // Set by ConEmu terminal
        val wtSession = System.getenv("WT_SESSION")   // Set by Windows Terminal

        // Check for modern terminals that explicitly support color
        if (colorTerm != null || termProgram != null || wtSession != null || conEmuANSI == "ON" || ansicon != null) {
            return true
        }

        // Fallback for standard UNIX terminals
        // This should be last, as TERM on Windows is not reliable.
        if (term != null && term != "dumb") {
            return true
        }

        // If none of the above, we are likely in an unsupported terminal
        // (like plain conhost.exe), so we must return false.
        return false
    }
}

internal fun ToolingDiagnostic.isSuppressed(options: ToolingDiagnosticRenderingOptions): Boolean {
    return when {
        // Non-suppressible
        id == KotlinToolingDiagnostics.InternalKotlinGradlePluginPropertiesUsed.id -> false

        severity == WARNING -> id in options.suppressedWarningIds

        severity == STRONG_WARNING -> id in options.suppressedErrorIds
        severity == ERROR -> id in options.suppressedErrorIds

        // NB: FATALs can not be suppressed
        else -> false
    }
}
