/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.utils.newInstance

internal class ProblemsReporterG76 : ProblemsReporter {
    override fun reportProblemDiagnostic(diagnostic: ToolingDiagnostic) {
        // no-op
    }

    class Factory : ProblemsReporter.Factory {
        override fun getInstance(project: Project) = project.objects.newInstance<ProblemsReporterG76>()
    }
}