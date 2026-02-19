/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.utils.newInstance

internal abstract class ProblemsReporterG85 : ProblemsReporter {
    private val logger: Logger by lazy { Logging.getLogger(this.javaClass) }

    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic, options: ToolingDiagnosticRenderingOptions) {
        val renderedDiagnostic = diagnostic.renderReportedDiagnostic(logger, options) ?: return
        if (renderedDiagnostic.severity == ToolingDiagnostic.Severity.FATAL) {
            throw diagnostic.createAnExceptionForFatalDiagnostic(options)
        }
    }

    class Factory : ProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): ProblemsReporter = objects.newInstance<ProblemsReporterG85>()
    }
}