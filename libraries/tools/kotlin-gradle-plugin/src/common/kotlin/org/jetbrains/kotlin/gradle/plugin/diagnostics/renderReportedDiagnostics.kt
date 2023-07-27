/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.*

internal fun renderReportedDiagnostics(diagnostics: Collection<ToolingDiagnostic>, logger: Logger, useParsableFormat: Boolean) {
    for (diagnostic in diagnostics) {
        renderReportedDiagnostic(diagnostic, logger, useParsableFormat)
    }
}

internal fun renderReportedDiagnostic(
    diagnostic: ToolingDiagnostic,
    logger: Logger,
    useParsableFormat: Boolean,
) {
    when (diagnostic.severity) {
        WARNING -> logger.warn("w: ${diagnostic.render(useParsableFormat)}\n")

        ERROR -> logger.error("e: ${diagnostic.render(useParsableFormat)}\n")

        FATAL -> throw diagnostic.createAnExceptionForFatalDiagnostic(useParsableFormat)
    }
}

internal fun ToolingDiagnostic.createAnExceptionForFatalDiagnostic(useParsableFormat: Boolean): InvalidUserCodeException =
    if (throwable != null)
        InvalidUserCodeException(render(useParsableFormat), throwable)
    else
        InvalidUserCodeException(render(useParsableFormat))

private fun ToolingDiagnostic.render(useParsableFormat: Boolean): String = buildString {
    if (useParsableFormat) {
        appendLine(this@render)
        append(DIAGNOSTIC_SEPARATOR)
    } else {
        append(message)
        if (throwable != null) {
            appendLine()
            appendLine("Stacktrace:")
            append(throwable.stackTraceToString().prependIndent("    "))
        }
    }
}

internal const val DIAGNOSTIC_SEPARATOR = "#diagnostic-end"
