/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.ProblemReporter
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal abstract class ProblemsReporterG86 @Inject constructor(
    private val problems: Problems,
) : ProblemsReporter {

    private val logger: Logger by lazy { Logging.getLogger(this.javaClass) }
    private val reporter by lazy { problems.forNamespace("org.jetbrains.kotlin.gradle.plugin") }

    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic, options: ToolingDiagnosticRenderingOptions) {
        val renderedDiagnostic = diagnostic.renderReportedDiagnostic(logger, options) ?: return
        reporter.report(renderedDiagnostic) { spec, throwable ->
            fillSpec(spec, diagnostic, renderedDiagnostic.severity, throwable)
        }
    }

    private fun fillSpec(
        spec: ProblemSpec,
        diagnostic: ToolingDiagnostic,
        severity: ToolingDiagnostic.Severity,
        throwable: KotlinDiagnosticsException?
    ) {
        spec
            .category(diagnostic.group.groupId)
            .label(diagnostic.identifier.displayName)
            .defaultSpecConfiguration(diagnostic, severity)
            .apply {
                throwable?.let { spec.withException(it) }
            }
    }

    internal fun ProblemReporter.report(
        renderedDiagnostic: ReportedDiagnostic,
        fillSpec: (ProblemSpec, KotlinDiagnosticsException?) -> Unit
    ) {
        try {
            when (renderedDiagnostic) {
                is ReportedDiagnostic.Message -> reporting { fillSpec(it, null) }
                is ReportedDiagnostic.Throwable -> throwing { fillSpec(it, renderedDiagnostic.throwable) }
            }
        } catch (e: NoSuchMethodError) {
            logger.error("Can't invoke reporter method:", e)
        }
    }

    class Factory : ProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): ProblemsReporter = objects.newInstance<ProblemsReporterG86>()
    }
}