/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.logging.Logger
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
    when (diagnostic.severity) {
        WARNING -> logger.warn("w: ${diagnostic.render(renderingOptions.useParsableFormat, renderingOptions.showStacktrace)}\n")

        ERROR -> logger.error("e: ${diagnostic.render(renderingOptions.useParsableFormat, renderingOptions.showStacktrace)}\n")

        FATAL -> throw diagnostic.createAnExceptionForFatalDiagnostic(renderingOptions)
    }
}

internal fun ToolingDiagnostic.createAnExceptionForFatalDiagnostic(
    renderingOptions: ToolingDiagnosticRenderingOptions
): InvalidUserCodeException {
    // NB: override showStacktrace to false, because it will be shown as 'cause' anyways
    val message = render(renderingOptions.useParsableFormat, showStacktrace = false)
    if (throwable != null)
        throw InvalidUserCodeException(message, throwable)
    else
        throw InvalidUserCodeException(message)
}

private fun ToolingDiagnostic.render(useParsableFormatting: Boolean, showStacktrace: Boolean): String = buildString {
    // Main message
    if (useParsableFormatting) appendLine(this@render) else append(message)

    // Additional stacktrace, if requested
    if (showStacktrace) renderStacktrace(this@render.throwable, useParsableFormatting)

    // Separator, if in verbose mode
    if (useParsableFormatting) appendLine(DIAGNOSTIC_SEPARATOR)
}

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
