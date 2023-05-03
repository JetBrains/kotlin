/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.*

internal fun renderReportedDiagnostics(diagnostics: Collection<ToolingDiagnostic>, logger: Logger, isVerbose: Boolean) {
    for (diagnostic in diagnostics) {
        renderReportedDiagnostic(diagnostic, logger, isVerbose)
    }
}

internal fun renderReportedDiagnostic(
    diagnostic: ToolingDiagnostic,
    logger: Logger,
    isVerbose: Boolean
) {
    when (diagnostic.severity) {
        WARNING -> logger.warn("w: ${diagnostic.render(isVerbose)}\n")

        ERROR -> logger.error("e: ${diagnostic.render(isVerbose)}\n")

        FATAL -> throw InvalidUserCodeException(diagnostic.render(isVerbose))
    }
}

private fun ToolingDiagnostic.render(isVerbose: Boolean): String =
    if (isVerbose) toString() + "\n$DIAGNOSTIC_SEPARATOR" else message

internal const val DIAGNOSTIC_SEPARATOR = "#diagnostic-end"
