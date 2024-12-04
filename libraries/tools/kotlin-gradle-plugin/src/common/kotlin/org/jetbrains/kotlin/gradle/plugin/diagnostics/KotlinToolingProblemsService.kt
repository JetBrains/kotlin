/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import javax.inject.Inject

internal val GRADLE_VERSION_FOR_PROBLEMS_API = GradleVersion.version("8.11")

internal interface KotlinToolingProblemsService {
    fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic)
}

private abstract class KotlinToolingProblemsDummyServiceImp : BuildService<BuildServiceParameters.None>, KotlinToolingProblemsService {
    private val logger = Logging.getLogger(javaClass)

    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic) {
        logger.info("Problems API is not available for Gradle ${GradleVersion.current().version}")
    }
}

private abstract class KotlinToolingProblemsServiceImp @Inject constructor(
    private val problems: Problems,
) : BuildService<BuildServiceParameters.None>, KotlinToolingProblemsService {

    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic) {
        if (diagnostic.throwable != null) {
            problems.reporter.throwing(diagnostic::configureProblemSpec)
        } else {
            problems.reporter.reporting(diagnostic::configureProblemSpec)
        }
    }
}

private fun ToolingDiagnostic.problemGroup(): ProblemGroup {
    class ProblemGroupImpl(val group: DiagnosticGroup) : ProblemGroup {
        override fun getName() = group.groupId
        override fun getDisplayName() = group.displayName
        override fun getParent() = group.parent?.let { ProblemGroupImpl(it) }
    }

    return ProblemGroupImpl(identifier.group)
}

private fun ToolingDiagnostic.configureProblemSpec(spec: ProblemSpec): ProblemSpec {
    var mSpec = spec
        .id(id, identifier.displayName, problemGroup())
        .details(message)
        .severity(severity.problemSeverity)

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

private val ToolingDiagnostic.Severity.problemSeverity: Severity
    get() = when (this) {
        ToolingDiagnostic.Severity.WARNING -> Severity.WARNING
        else -> Severity.ERROR
    }


internal val Project.kotlinToolingProblemsServiceProvider: Provider<out KotlinToolingProblemsService>
    get() = if (GradleVersion.current() >= GRADLE_VERSION_FOR_PROBLEMS_API) {
        gradle.registerClassLoaderScopedBuildService(KotlinToolingProblemsServiceImp::class)
    } else {
        gradle.registerClassLoaderScopedBuildService(KotlinToolingProblemsDummyServiceImp::class)
    }

//internal val Project.kotlinToolingProblemsService: KotlinToolingProblemsService
//    get() = kotlinToolingProblemsServiceProvider.get()
