/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.*
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal interface ProblemsReporter {
    fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic, parameters: Parameters)

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(objects: ObjectFactory): ProblemsReporter
    }

    interface Parameters {
        val showStacktrace: Boolean
        val ignoreWarningMode: Boolean
        val warningMode: WarningMode

        fun effectiveSeverity(severity: ToolingDiagnostic.Severity): ToolingDiagnostic.Severity?
    }
}

internal fun ProblemReporter.report(
    diagnostic: ToolingDiagnostic,
    parameters: ProblemsReporter.Parameters,
    fillSpec: (ProblemSpec, ToolingDiagnostic.Severity) -> Unit
) {
    val effectiveSeverity = parameters.effectiveSeverity(diagnostic.severity) ?: return
    if (effectiveSeverity == ToolingDiagnostic.Severity.FATAL) {
        throwing { fillSpec(it, effectiveSeverity) }
    } else {
        reporting { fillSpec(it, effectiveSeverity) }
    }
}

internal abstract class DefaultProblemsReporter @Inject constructor(
    private val problems: Problems
) : ProblemsReporter {
    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic, parameters: ProblemsReporter.Parameters) {
        problems.reporter.report(diagnostic, parameters) { spec, severity -> fillSpec(spec, diagnostic, severity) }
    }

    private fun fillSpec(
        spec: ProblemSpec,
        diagnostic: ToolingDiagnostic,
        severity: ToolingDiagnostic.Severity,
    ) {
        spec
            .id(diagnostic.id, diagnostic.identifier.displayName, problemGroup(diagnostic.group))
            .contextualLabel(diagnostic.identifier.displayName)
            .defaultSpecConfiguration(diagnostic, severity)
            .apply {
                if (severity == ToolingDiagnostic.Severity.FATAL) {
                    fillException(spec, diagnostic)
                }
            }
    }

    private fun problemGroup(group: DiagnosticGroup): ProblemGroup = KGPProblemGroup(group)

    private fun fillException(spec: ProblemSpec, diagnostic: ToolingDiagnostic) {
        spec.withException(
            KotlinDiagnosticsException(
                diagnostic.identifier.displayName,
                diagnostic.throwable ?: Throwable(diagnostic.message)
            )
        )
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

// Create own implementation of ProblemGroup. In gradle 18.3 there will be a static factory method for creating ProblemGroup
internal class KGPProblemGroup(val group: DiagnosticGroup) : ProblemGroup {
    override fun getName() = group.groupId
    override fun getDisplayName() = group.displayName
    override fun getParent() = group.parent?.let { KGPProblemGroup(it) }

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
