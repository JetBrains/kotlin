/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Severity
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer

internal fun CompilerMessageRenderer.Severity.toGradleSeverity(): Severity? = when (this) {
    CompilerMessageRenderer.Severity.ERROR -> Severity.ERROR
    CompilerMessageRenderer.Severity.WARNING -> Severity.WARNING
    CompilerMessageRenderer.Severity.INFO, CompilerMessageRenderer.Severity.DEBUG -> null
}

// Default setup for all gradle variants since 8.6 till Gradle 9.6
internal fun ProblemSpec.defaultSpecConfiguration(diagnostic: ToolingDiagnostic, severity: KotlinToolingDiagnosticsSeverity): ProblemSpec {
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

internal val KotlinToolingDiagnosticsSeverity.problemSeverity: Severity
    get() = when (this) {
        KotlinToolingDiagnosticsSeverity.WARNING -> Severity.WARNING
        else -> Severity.ERROR
    }
