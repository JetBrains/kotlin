/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal abstract class ProblemsReporterG88 @Inject constructor(
    private val problems: Problems,
) : ProblemsReporter {

    private val reporter by lazy { problems.forNamespace("org.jetbrains.kotlin.gradle.plugin") }

    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic) {
        reporter.report(diagnostic) { fillSpec(it, diagnostic) }
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
        override fun getInstance(objects: ObjectFactory): ProblemsReporter = objects.newInstance<ProblemsReporterG88>()
    }
}

