/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.utils.newInstance

internal abstract class ProblemsReporterG81 : ProblemsReporter {
    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic, parameters: ProblemsReporter.Parameters) {
        // no-op
    }

    class Factory : ProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): ProblemsReporter = objects.newInstance<ProblemsReporterG81>()
    }
}