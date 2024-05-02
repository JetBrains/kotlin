/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.jetbrains.kotlin.incremental.createDirectory
import java.io.File
import kotlin.test.Test

class OverriddenKotlinHomeCheckerTest {

    @Test
    fun `overridden kotlin home with wrong path failes build`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                project.extraProperties.set(PropertiesProvider.KOTLIN_NATIVE_HOME, "non_existed_path")
            }) {
            project.multiplatformExtension.applyDefaultHierarchyTemplate()
            project.multiplatformExtension.linuxX64()
        }
        project.checkDiagnostics("BrokenKotlinNativeBundleError")
    }

    @Test
    fun `without overridden kotlin home build is successfull`() {
        val project = buildProjectWithMPP {
            project.multiplatformExtension.applyDefaultHierarchyTemplate()
            project.multiplatformExtension.linuxX64()
        }
        project.assertNoDiagnostics()
    }


    @Test
    fun `with relative path overridden kotlin native home successfull`() {
        fun createKotlinNativeDistStub(project: Project) {
            val customKonanPath = File(project.rootDir.canonicalPath).resolve("dist")
            customKonanPath.createDirectory()
            customKonanPath.resolve("konan").createDirectory()
            customKonanPath.resolve("bin").createDirectory()
        }

        val project = buildProjectWithMPP(
            preApplyCode = {
                createKotlinNativeDistStub(project)
                project.extraProperties.set(PropertiesProvider.KOTLIN_NATIVE_HOME, "./dist")
            }) {

            project.multiplatformExtension.applyDefaultHierarchyTemplate()
            project.multiplatformExtension.linuxX64()
        }

        project.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(project)
            .assertNoDiagnostics(KotlinToolingDiagnostics.BrokenKotlinNativeBundleError)
    }


}