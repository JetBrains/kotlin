/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.gradle.utils.newInstance

internal abstract class CompilerDiagnosticsProblemsReporterG76 : CompilerDiagnosticsProblemsReporter {
    override fun reportCompilerMessage(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
    ) {
        // Gradle < 8.6 does not provide the Problems API, so compiler diagnostics are reported only via
        // regular compiler output rendered by Build Tools API and this reporter intentionally does nothing.
    }

    class Factory : CompilerDiagnosticsProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): CompilerDiagnosticsProblemsReporter {
            return objects.newInstance<CompilerDiagnosticsProblemsReporterG76>()
        }
    }
}
