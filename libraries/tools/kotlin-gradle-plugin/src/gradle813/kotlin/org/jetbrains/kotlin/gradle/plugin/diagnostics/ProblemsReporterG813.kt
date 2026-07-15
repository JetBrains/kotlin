/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal abstract class ProblemsReporterG813 @Inject constructor(
    private val problems: Problems
) : ProblemsReporter {
    private val logger: Logger by lazy { Logging.getLogger(this.javaClass) }

    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic, options: ToolingDiagnosticRenderingOptions) {
        val renderedDiagnostic = diagnostic.renderReportedDiagnostic(logger, options) ?: return
        problems.reporter.report(renderedDiagnostic, diagnostic) { spec, throwable ->
            fillSpec(spec, diagnostic, renderedDiagnostic.severity, throwable)
        }
    }

    private fun fillSpec(
        spec: ProblemSpec,
        diagnostic: ToolingDiagnostic,
        severity: KotlinToolingDiagnosticsSeverity,
        throwable: KotlinDiagnosticsException?
    ) {
        spec
            .contextualLabel(diagnostic.identifier.displayName)
            .defaultSpecConfiguration(diagnostic, severity)
            .apply {
                throwable?.let { spec.withException(it) }
            }
    }

    private fun ProblemSpec.defaultSpecConfiguration(
        diagnostic: ToolingDiagnostic,
        severity: KotlinToolingDiagnosticsSeverity,
    ): ProblemSpec {
        return details(diagnostic.message)
            .severity(severity.problemSeverity)
            .apply {
                diagnostic.solutions.forEach {
                    solution(it)
                }

                diagnostic.documentation?.let {
                    documentedAt(it.url)
                }
            }
    }

    class Factory : ProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): ProblemsReporter = objects.newInstance<ProblemsReporterG813>()
    }
}
