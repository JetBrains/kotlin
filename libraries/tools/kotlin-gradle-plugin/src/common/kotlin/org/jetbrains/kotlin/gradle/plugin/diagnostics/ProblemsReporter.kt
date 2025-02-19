/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.*
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal interface ProblemsReporter {
    fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic)

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(objects: ObjectFactory): ProblemsReporter
    }
}

internal fun ProblemReporter.report(diagnostic: ToolingDiagnostic, fillSpec: (ProblemSpec) -> Unit) {
    if (diagnostic.throwable != null) {
        throwing { fillSpec(it) }
    } else {
        reporting { fillSpec(it) }
    }
}

internal abstract class DefaultProblemsReporter @Inject constructor(
    private val problems: Problems,
) : BuildService<BuildServiceParameters.None>, ProblemsReporter {
    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic) {
        problems.reporter.report(diagnostic) { fillSpec(it, diagnostic) }
    }

    private fun fillSpec(spec: ProblemSpec, diagnostic: ToolingDiagnostic) {
        spec
            .id(diagnostic.id, diagnostic.identifier.displayName, problemGroup(diagnostic.group))
            .contextualLabel(diagnostic.identifier.displayName)
            .apply {
                diagnostic.configureProblemSpec(this)
                diagnostic.throwable?.let {
                    withException(InvalidUserCodeException(diagnostic.identifier.displayName, it))
                }
            }
    }

    private fun problemGroup(group: DiagnosticGroup): ProblemGroup = KGPProblemGroup(group)

    class Factory : ProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): ProblemsReporter = objects.newInstance<DefaultProblemsReporter>()
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

internal fun ToolingDiagnostic.configureProblemSpec(spec: ProblemSpec) {
    spec
        .details(message)
        .severity(severity.problemSeverity)

    solutions.forEach {
        spec.solution(it)
    }

    documentation?.let {
        spec.documentedAt(it.url)
    }
}

internal val ToolingDiagnostic.Severity.problemSeverity: Severity
    get() = when (this) {
        ToolingDiagnostic.Severity.WARNING -> Severity.WARNING
        else -> Severity.ERROR
    }
