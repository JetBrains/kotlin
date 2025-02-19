/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal class ProblemsReporterG88 @Inject constructor(
    private val problems: Problems,
) : ProblemsReporter {

    private val reporter by lazy { problems.forNamespace("org.jetbrains.kotlin.gradle.plugin") }

    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic) {
        if (diagnostic.throwable != null) {
            reporter.throwing { fillSpec(it, diagnostic) }
        } else {
            reporter.reporting { fillSpec(it, diagnostic) }
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
        override fun getInstance(project: Project) = project.objects.newInstance<ProblemsReporterG88>()
    }
}