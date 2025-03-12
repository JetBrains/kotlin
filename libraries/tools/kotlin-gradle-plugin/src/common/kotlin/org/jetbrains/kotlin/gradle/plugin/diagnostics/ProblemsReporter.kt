/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.*
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.utils.newInstance
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
    fillSpec: (ProblemSpec, KotlinDiagnosticsException?) -> Unit
) {
    when (renderedDiagnostic) {
        is ReportedDiagnostic.Message -> reporting { fillSpec(it, null) }
        is ReportedDiagnostic.Throwable -> throwing { fillSpec(it, renderedDiagnostic.throwable) }
    }
}

internal abstract class DefaultProblemsReporter @Inject constructor(
    private val problems: Problems
) : ProblemsReporter {
    private val logger: Logger by lazy { Logging.getLogger(this.javaClass) }

    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic, options: ToolingDiagnosticRenderingOptions) {
        val renderedDiagnostic = diagnostic.renderReportedDiagnostic(logger, options) ?: return
        problems.reporter.report(renderedDiagnostic) { spec, throwable ->
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
            .id(diagnostic.id, diagnostic.identifier.displayName, problemGroup(diagnostic.group))
            .contextualLabel(diagnostic.identifier.displayName)
            .defaultSpecConfiguration(diagnostic, severity)
            .apply {
                throwable?.let { spec.withException(it) }
            }
    }

    private fun problemGroup(group: DiagnosticGroup): ProblemGroup = KgpProblemGroup(group)

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
internal class KgpProblemGroup(val group: DiagnosticGroup) : ProblemGroup {
    override fun getName() = group.groupId
    override fun getDisplayName() = group.displayName
    override fun getParent() = group.parent?.let { KgpProblemGroup(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProblemGroup) return false

        if (getName() != other.name) return false
        if (getParent() != other.parent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = getName().hashCode()
        result = 31 * result + (getParent()?.hashCode() ?: 0)
        return result
    }
}

internal val ToolingDiagnostic.Severity.problemSeverity: Severity
    get() = when (this) {
        ToolingDiagnostic.Severity.WARNING -> Severity.WARNING
        else -> Severity.ERROR
    }
