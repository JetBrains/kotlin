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
        ConsoleOutput.Auto -> isAttachedToTerminal.get()
        // Plain mode explicitly disables colors
        ConsoleOutput.Plain -> false
        // Rich and Verbose modes force colors on regardless of terminal
        ConsoleOutput.Rich, ConsoleOutput.Verbose -> true
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
        val colorTerm = System.getenv("COLORTERM")    // Explicit color support flag
        val termProgram = System.getenv("TERM_PROGRAM") // Terminal emulator program

        // Common terminal types:
        // - "dumb": Basic terminal with minimal features (often in CI environments or redirected output)
        // - "xterm", "xterm-256color": Standard terminal types with good feature support
        // - Terminal emulators like "iTerm.app", "Apple_Terminal" will set TERM_PROGRAM

        // Windows-specific terminal detection
        val ansicon = System.getenv("ANSICON")        // Set by ANSICON and similar Windows terminal enhancers
        val conEmuANSI = System.getenv("ConEmuANSI")  // Set by ConEmu terminal
        val wtSession = System.getenv("WT_SESSION")    // Set by Windows Terminal

        // Check for PowerShell
        val psVersion = System.getenv("PSModulePath") // Typically set in PowerShell environment

        return (term != null && term != "dumb") ||    // Unix terminal check
                colorTerm != null ||                  // Color support check
                termProgram != null ||                // Modern terminal emulator check
                ansicon != null ||                    // Windows ANSI support
                "ON" == conEmuANSI ||                 // ConEmu with ANSI
                wtSession != null ||                  // Windows Terminal
                (psVersion != null && System.console() != null) // Interactive PowerShell session
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
