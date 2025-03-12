/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.*

internal sealed class ReportedDiagnostic(val severity: ToolingDiagnostic.Severity) {
    class Message(
        severity: ToolingDiagnostic.Severity
    ) : ReportedDiagnostic(severity)

    class Throwable(
        severity: ToolingDiagnostic.Severity,
        val throwable: KotlinDiagnosticsException,
    ) : ReportedDiagnostic(severity)
}

internal fun ToolingDiagnostic.renderReportedDiagnostic(
    logger: Logger,
    renderingOptions: ToolingDiagnosticRenderingOptions
): ReportedDiagnostic? {
    if (isSuppressed(renderingOptions)) return null
    val effectiveSeverity = renderingOptions.effectiveSeverity(severity) ?: return null
    val message by lazy { render(renderingOptions, effectiveSeverity = effectiveSeverity) }

    when (effectiveSeverity) {
        WARNING -> logger.warn("w: ${message}\n")
        ERROR -> logger.error("e: ${message}\n")
        else -> {}
    }

    return if (effectiveSeverity == FATAL)
        ReportedDiagnostic.Throwable(effectiveSeverity, createAnExceptionForFatalDiagnostic(renderingOptions))
    else
        ReportedDiagnostic.Message(effectiveSeverity)
}

internal typealias KotlinDiagnosticsException = InvalidUserCodeException

internal fun ToolingDiagnostic.createAnExceptionForFatalDiagnostic(
    renderingOptions: ToolingDiagnosticRenderingOptions
): KotlinDiagnosticsException {
    // NB: override showStacktrace to false, because it will be shown as 'cause' anyways
    // override coloredOutput to false, because it's exception message
    val message = render(renderingOptions, showStacktrace = false, coloredOutput = false)
    return if (throwable != null)
        KotlinDiagnosticsException(message, throwable)
    else
        KotlinDiagnosticsException(message)
}

private fun ToolingDiagnostic.render(
    renderingOptions: ToolingDiagnosticRenderingOptions,
    showStacktrace: Boolean = renderingOptions.showStacktrace,
    coloredOutput: Boolean = renderingOptions.coloredOutput,
): String = buildString {
    with(renderingOptions) {
        val diagnosticOutput = if (coloredOutput) styled(showSeverityEmoji) else plain(showSeverityEmoji)

        // Main message
        if (useParsableFormat) {
            appendLine(this@render)
        } else {
            appendLine(diagnosticOutput.name)
            appendLine(diagnosticOutput.message)
            diagnosticOutput.solution?.let {
                appendLine(it)
            }
            diagnosticOutput.documentation?.let {
                appendLine(it)
            }
        }

        // Additional stacktrace, if requested
        if (showStacktrace) renderStacktrace(this@render.throwable, useParsableFormat)

        // Separator, if in verbose mode
        if (useParsableFormat) appendLine(DIAGNOSTIC_SEPARATOR)
    }
}.trimEnd()

private fun StringBuilder.renderStacktrace(throwable: Throwable?, useParsableFormatting: Boolean) {
    if (throwable == null) return
    appendLine()
    appendLine(DIAGNOSTIC_STACKTRACE_START)
    appendLine(throwable.stackTraceToString().trim().prependIndent("    "))
    if (useParsableFormatting) appendLine(DIAGNOSTIC_STACKTRACE_END_SEPARATOR)
}

internal const val DIAGNOSTIC_SEPARATOR = "#diagnostic-end"
internal const val DIAGNOSTIC_STACKTRACE_START = "Stacktrace:"
internal const val DIAGNOSTIC_STACKTRACE_END_SEPARATOR = "#stacktrace-end"
