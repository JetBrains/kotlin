/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.configuration.WarningMode
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.*

internal fun renderReportedDiagnostics(
    diagnostics: Collection<ToolingDiagnostic>,
    logger: Logger,
    renderingOptions: ToolingDiagnosticRenderingOptions,
) {
    for (diagnostic in diagnostics) {
        renderReportedDiagnostic(diagnostic, logger, renderingOptions)
    }
}

internal fun renderReportedDiagnostic(
    diagnostic: ToolingDiagnostic,
    logger: Logger,
    renderingOptions: ToolingDiagnosticRenderingOptions
) {
    val effectiveSeverity = if (renderingOptions.ignoreWarningMode) {
        diagnostic.severity
    } else {
        // Early return if warnings are disabled and it's not an error and not fatal
        if (renderingOptions.warningMode == WarningMode.None && diagnostic.severity == WARNING) {
            return
        }

        if (diagnostic.severity == WARNING && renderingOptions.warningMode == WarningMode.Fail)
            ERROR
        else
            diagnostic.severity

        //TODO: KT-74986 Support WarningMode.Summary mode for gradle diagnostics
    }

    when (effectiveSeverity) {
        WARNING -> logger.warn("w: ${diagnostic.render(renderingOptions)}\n")
        ERROR -> logger.error("e: ${diagnostic.render(renderingOptions)}\n")
        FATAL -> throw diagnostic.createAnExceptionForFatalDiagnostic(renderingOptions)
    }
}

internal typealias KotlinDiagnosticsException = InvalidUserCodeException

internal fun ToolingDiagnostic.createAnExceptionForFatalDiagnostic(
    renderingOptions: ToolingDiagnosticRenderingOptions
): KotlinDiagnosticsException {
    // NB: override showStacktrace to false, because it will be shown as 'cause' anyways
    val message = render(renderingOptions, showStacktrace = false)
    if (throwable != null)
        throw KotlinDiagnosticsException(message, throwable)
    else
        throw KotlinDiagnosticsException(message)
}

private fun ToolingDiagnostic.render(
    renderingOptions: ToolingDiagnosticRenderingOptions,
    showStacktrace: Boolean = renderingOptions.showStacktrace,
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
