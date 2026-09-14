/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.Problems
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal abstract class CompilerDiagnosticsProblemsReporterG811 @Inject constructor(
    private val problems: Problems,
) : CompilerDiagnosticsProblemsReporter {
    private val logger: Logger by lazy { Logging.getLogger(this.javaClass) }

    override fun reportCompilerMessage(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
        diagnosticId: String?,
    ) {
        val gradleSeverity = severity.toGradleSeverity() ?: return
        val diagnosticGroup = severity.toDiagnosticGroup()
        try {
            problems.reporter.reporting {
                it
                    .id(
                        severity.resolvedProblemId(diagnosticId),
                        severity.resolvedDisplayName(diagnosticId),
                        KgpProblemGroup(diagnosticGroup),
                    )
                    .contextualLabel(severity.toDisplayName())
                    .details(message)
                    .severity(gradleSeverity)
                    .applySourceLocation(location)
            }
        } catch (e: NoSuchMethodError) {
            logger.error("Can't invoke reporter method:", e)
        }
    }

    class Factory : CompilerDiagnosticsProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): CompilerDiagnosticsProblemsReporter {
            return objects.newInstance<CompilerDiagnosticsProblemsReporterG811>()
        }
    }
}

// Create own implementation of ProblemGroup as there is no factory method to create it
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
