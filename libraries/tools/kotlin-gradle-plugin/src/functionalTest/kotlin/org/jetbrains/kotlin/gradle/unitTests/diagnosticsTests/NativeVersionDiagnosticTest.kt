/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test

class NativeVersionDiagnosticTest {
    private fun setUpProject(
        nativeVersion: String,
        skipNativeTarget: Boolean = false,
    ): ProjectInternal {
        val project = buildProjectWithMPP(
            preApplyCode = {
                project.extraProperties.set("kotlin.native.version", nativeVersion)
                project.extraProperties.set("kotlin.native.distribution.downloadFromMaven", true)
            }
        ) {
            kotlin {
                jvm()
                if (!skipNativeTarget) linuxX64()
            }
        }
        project.evaluate()
        return project
    }

    @Test
    fun newKotlinNativeVersionCheck() {
        val project = setUpProject("30.0.0")
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.NewNativeVersionDiagnostic)
    }

    @Test
    fun oldKotlinNativeVersionCheck() {
        val project = setUpProject("1.0.0")
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.OldNativeVersionDiagnostic)
    }

    @Test
    fun kotlinNativeVersionCheck() {
        val project = setUpProject(CURRENT)
        project.assertNoDiagnostics(filterDiagnosticIds = emptyList())
    }

    @Test
    fun checkIsSkippedInProjectWithoutNativeTargets() {
        val project = setUpProject("1.0.0", true)

        project.assertNoDiagnostics(filterDiagnosticIds = emptyList())
    }

    @Test
    fun checkIsSkippedInJvmOnlyProject() {
        val project = buildProjectWithJvm(
            preApplyCode = {
                project.extraProperties.set("kotlin.native.version", "1.0.0")
                project.extraProperties.set("kotlin.native.distribution.downloadFromMaven", true)
            }
        )
        project.evaluate()
        project.assertNoDiagnostics(filterDiagnosticIds = emptyList())
    }

    companion object {
        val CURRENT: String
            get() = System.getProperty("kotlinVersion")
    }
}