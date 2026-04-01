/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.Problems
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal abstract class CompilerDiagnosticsProblemsReporterG811 @Inject constructor(
    private val problems: Problems,
) : CompilerDiagnosticsProblemsReporter {
    private val logger: Logger by lazy { Logging.getLogger(this.javaClass) }

    override fun reportCompilerMessage(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
    ) {
        val gradleSeverity = severity.toGradleSeverity() ?: return
        val diagnosticGroup = severity.toDiagnosticGroup()

        try {
            problems.reporter.reporting {
                it
                    .id(severity.problemId, severity.toDisplayName(), KgpProblemGroup(diagnosticGroup))
                    .contextualLabel(severity.toDisplayName())
                    .details(message)
                    .severity(gradleSeverity)
                    .applySourceLocation(location)
            }
        } catch (e: NoSuchMethodError) {
            logger.error("Can't invoke reporter method:", e)
        }
    }

    class Factory : CompilerDiagnosticsProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): CompilerDiagnosticsProblemsReporter {
            return objects.newInstance<CompilerDiagnosticsProblemsReporterG811>()
        }
    }
}
