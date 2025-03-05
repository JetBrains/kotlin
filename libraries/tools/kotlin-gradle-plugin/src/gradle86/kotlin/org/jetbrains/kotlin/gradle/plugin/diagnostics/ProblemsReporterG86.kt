/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal abstract class ProblemsReporterG86 @Inject constructor(
    private val problems: Problems,
) : ProblemsReporter {

    private val reporter by lazy { problems.forNamespace("org.jetbrains.kotlin.gradle.plugin") }

    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic, parameters: ProblemsReporter.Parameters) {
        reporter.report(diagnostic, parameters) { spec, severity -> fillSpec(spec, diagnostic, severity) }
    }

    private fun fillSpec(
        spec: ProblemSpec,
        diagnostic: ToolingDiagnostic,
        severity: ToolingDiagnostic.Severity,
    ) {
        spec
            .category(diagnostic.group.groupId)
            .label(diagnostic.identifier.displayName)
            .defaultSpecConfiguration(diagnostic, severity)
            .apply {
                if (severity == ToolingDiagnostic.Severity.FATAL) {
                    fillException(spec, diagnostic)
                }
            }
    }

    private fun fillException(spec: ProblemSpec, diagnostic: ToolingDiagnostic) {
        spec.withException(
            RuntimeException(
                diagnostic.identifier.displayName,
                diagnostic.throwable ?: Throwable(diagnostic.message)
            )
        )
    }

    class Factory : ProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): ProblemsReporter = objects.newInstance<ProblemsReporterG86>()
    }
}