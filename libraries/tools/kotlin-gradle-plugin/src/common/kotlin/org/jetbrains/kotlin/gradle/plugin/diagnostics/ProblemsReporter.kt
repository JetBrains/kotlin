/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal interface ProblemsReporter {
    fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic)

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(project: Project): ProblemsReporter
    }
}

internal class DefaultProblemsReporter @Inject constructor(
    private val problems: Problems,
) : ProblemsReporter {
    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic) {
        if (diagnostic.throwable != null) {
            problems.reporter.throwing { fillSpec(it, diagnostic) }
        } else {
            problems.reporter.reporting(diagnostic::configureProblemSpec)
        }
    }

    private fun fillSpec(spec: ProblemSpec, diagnostic: ToolingDiagnostic): ProblemSpec {
        return spec
            .id(diagnostic.id, diagnostic.identifier.displayName, diagnostic.problemGroup())
            .details(diagnostic.message)
            .severity(diagnostic.severity.problemSeverity)
            .apply { diagnostic.configureProblemSpec(this) }
    }

    class Factory : ProblemsReporter.Factory {
        override fun getInstance(project: Project) = project.objects.newInstance<DefaultProblemsReporter>()
    }
}

internal val Project.problemsReporter
    get() = variantImplementationFactory<ProblemsReporter.Factory>()
        .getInstance(this)

internal fun ToolingDiagnostic.problemGroup(): ProblemGroup {
    class ProblemGroupImpl(val group: DiagnosticGroup) : ProblemGroup {
        override fun getName() = group.groupId
        override fun getDisplayName() = group.displayName
        override fun getParent() = group.parent?.let { ProblemGroupImpl(it) }

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

    return ProblemGroupImpl(identifier.group)
}

internal fun ToolingDiagnostic.configureProblemSpec(spec: ProblemSpec): ProblemSpec {
    var mSpec = spec
    solutions.forEach {
        mSpec = mSpec.solution(it)
    }

    documentation?.let {
        mSpec = mSpec.documentedAt(it.url)
    }

    throwable?.let {
        mSpec = mSpec.withException(RuntimeException(it))
    }

    return mSpec
}

internal val ToolingDiagnostic.Severity.problemSeverity: Severity
    get() = when (this) {
        ToolingDiagnostic.Severity.WARNING -> Severity.WARNING
        else -> Severity.ERROR
    }
