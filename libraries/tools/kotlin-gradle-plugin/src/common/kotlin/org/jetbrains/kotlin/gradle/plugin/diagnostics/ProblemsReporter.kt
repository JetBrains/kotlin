/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.*
import org.gradle.internal.cc.base.logger
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.utils.decamelize
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import javax.inject.Inject

internal interface ProblemsReporter {
    fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic, options: ToolingDiagnosticRenderingOptions)

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(objects: ObjectFactory): ProblemsReporter
    }
}

internal fun ToolingDiagnostic.reportProblem(
    reporter: ProblemsReporter,
    options: ToolingDiagnosticRenderingOptions
) {
    reporter.reportProblemDiagnostic(this, options)
}

internal fun Collection<ToolingDiagnostic>.reportProblems(
    reporter: ProblemsReporter,
    options: ToolingDiagnosticRenderingOptions
) {
    for (diagnostic in this) {
        diagnostic.reportProblem(reporter, options)
    }
}

internal fun ProblemReporter.report(
    renderedDiagnostic: ReportedDiagnostic,
    diagnostic: ToolingDiagnostic,
    fillSpec: (ProblemSpec, KotlinDiagnosticsException?) -> Unit
) {
    try {
        when (renderedDiagnostic) {
            is ReportedDiagnostic.Message -> report(
                ProblemId.create(diagnostic.id.decamelize(), diagnostic.name, diagnostic.group.toProblemGroup())
            ) { fillSpec(it, null) }
            is ReportedDiagnostic.Throwable -> throwing(
                renderedDiagnostic.throwable,
                ProblemId.create(diagnostic.id.decamelize(), diagnostic.name, diagnostic.group.toProblemGroup())
            ) { fillSpec(it, renderedDiagnostic.throwable) }
        }
    } catch (e: NoSuchMethodError) {
        logger.error("Can't invoke reporter method:", e)
    }
}

internal abstract class DefaultProblemsReporter @Inject constructor(
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
        severity: ToolingDiagnostic.Severity,
        throwable: KotlinDiagnosticsException?
    ) {
        spec
            .contextualLabel(diagnostic.identifier.displayName)
            .defaultSpecConfiguration(diagnostic, severity)
            .apply {
                throwable?.let { spec.withException(it) }
            }
    }

    class Factory : ProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): ProblemsReporter = objects.newInstance<DefaultProblemsReporter>()
    }
}

// Default setup for all gradle variants since 8.6
internal fun ProblemSpec.defaultSpecConfiguration(diagnostic: ToolingDiagnostic, severity: ToolingDiagnostic.Severity): ProblemSpec {
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

// Create own implementation of ProblemGroup. In gradle 8.13 there will be a static factory method for creating ProblemGroup
internal fun DiagnosticGroup.toProblemGroup(): ProblemGroup = ProblemGroup.create(
    groupId.toLowerCaseAsciiOnly(),
    displayName,
    parent?.toProblemGroup()
)

internal val ToolingDiagnostic.Severity.problemSeverity: Severity
    get() = when (this) {
        ToolingDiagnostic.Severity.WARNING -> Severity.WARNING
        else -> Severity.ERROR
    }
