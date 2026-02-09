/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProject
import kotlin.test.Test

class DiagnosticFilteringTest {
    @Test
    fun filtersDiagnosticsWithIdSuffix() {
        val project = buildProject()

        project.reportDiagnosticOncePerBuild(
            KotlinToolingDiagnostics.DisabledNativeTargetTaskWarning(
                taskName = "runDebugExecutableLinuxX64",
                targetName = "linuxX64",
                currentHost = "test-host",
                reason = "tests can only run on linuxX64",
            )
        )

        project.assertNoDiagnostics(filterDiagnosticIds = listOf(KotlinToolingDiagnostics.DisabledNativeTargetTaskWarning))
    }
}
