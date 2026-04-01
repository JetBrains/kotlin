/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemReporter
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.jetbrains.kotlin.gradle.utils.decamelize
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import javax.inject.Inject

internal abstract class ProblemsReporterG811 @Inject constructor(
    private val problems: Problems,
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
            .id(diagnostic.id.decamelize(), diagnostic.identifier.displayName, problemGroup(diagnostic.group))
            .contextualLabel(diagnostic.identifier.displayName)
            .defaultSpecConfiguration(diagnostic, severity)
            .apply {
                throwable?.let { spec.withException(it) }
            }
    }

    private fun problemGroup(group: DiagnosticGroup): ProblemGroup = KgpProblemGroup(group)

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
        override fun getInstance(objects: ObjectFactory): ProblemsReporter = objects.newInstance<ProblemsReporterG811>()
    }
}

// Create own implementation of ProblemGroup as there is no factory method to create it
internal class KgpProblemGroup(val group: DiagnosticGroup) : ProblemGroup {
    override fun getName() = group.groupId.toLowerCaseAsciiOnly()
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
