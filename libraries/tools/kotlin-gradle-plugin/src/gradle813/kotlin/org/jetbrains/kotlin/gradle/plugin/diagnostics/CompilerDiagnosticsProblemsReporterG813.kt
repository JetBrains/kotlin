/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.AdditionalData
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.Problems
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

/**
 * Makes a diagnostic unique per task.
 *
 * Gradle 8.13 deduplicates problems by content hash and attaches the task location only after that check, so the same
 * compiler message from the metadata, JVM and JS compilations of `commonMain` collapses into one entry (KT-88430).
 *
 * Keep this a JavaBean over `String`: 8.13 rejects `Property<T>` in additional data, which is why
 * [KotlinCompilerDiagnosticAdditionalData] can't be reused here.
 */
internal interface CompilerDiagnosticTaskData : AdditionalData {
    var taskPath: String?
}

internal abstract class CompilerDiagnosticsProblemsReporterG813 @Inject constructor(
    private val problems: Problems,
    private val taskPath: String,
) : CompilerDiagnosticsProblemsReporter {
    private val logger: Logger = Logging.getLogger(this.javaClass)

    override fun reportCompilerMessage(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
        diagnosticId: String?,
    ) {
        val gradleSeverity = severity.toGradleSeverity() ?: return
        val diagnosticGroup = severity.toDiagnosticGroup()
        val problemId = ProblemId.create(
            severity.resolvedProblemId(diagnosticId),
            severity.resolvedDisplayName(diagnosticId),
            diagnosticGroup.toProblemGroup(),
        )

        try {
            problems.reporter.report(problemId) {
                it
                    .contextualLabel(severity.toDisplayName())
                    .details(message)
                    .severity(gradleSeverity)
                    .applySourceLocation(location)
                    .additionalData(CompilerDiagnosticTaskData::class.java) { data ->
                        data.taskPath = taskPath
                    }
            }
        } catch (e: NoSuchMethodError) {
            logger.error("Can't invoke reporter method:", e)
        }
    }

    class Factory : CompilerDiagnosticsProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory, taskPath: String): CompilerDiagnosticsProblemsReporter {
            return objects.newInstance<CompilerDiagnosticsProblemsReporterG813>(taskPath)
        }
    }
}
