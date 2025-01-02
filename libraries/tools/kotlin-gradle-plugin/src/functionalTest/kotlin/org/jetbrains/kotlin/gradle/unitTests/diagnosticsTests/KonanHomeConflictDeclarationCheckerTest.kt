/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.properties.NativeProperties
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.Rule
import kotlin.test.Test

class KonanHomeConflictDeclarationCheckerTest {
    @get:Rule val muteableTestRule = MuteableTestRule()

    @Test
    fun `kotlin native home and konan data dir property conflict`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                project.extraProperties.set(NativeProperties.NATIVE_HOME.name, "kotlin_naitve_home_non_existed_path")
                project.extraProperties.set(NativeProperties.KONAN_DATA_DIR.name, "konan_data_dir_non_existed_path")
            }) {
            project.extraProperties.set("kotlin.native.distribution.downloadFromMaven", "true")
            project.multiplatformExtension.applyDefaultHierarchyTemplate()
            project.multiplatformExtension.linuxX64()
        }
        project.checkDiagnostics("ConflictedKotlinNativeHomeWarning")
    }

    @Test
    fun `check that there is no diagnostics message with only konan data dir property declared`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                project.extraProperties.set(NativeProperties.KONAN_DATA_DIR.name, "build/konan")
            }) {
            project.extraProperties.set("kotlin.native.distribution.downloadFromMaven", "true")
            project.multiplatformExtension.applyDefaultHierarchyTemplate()
            project.multiplatformExtension.linuxX64()
        }
        project.assertNoDiagnostics()
    }

    @Test
    fun `check that there is no diagnostics message with only kotlin native home property declared`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                project.extraProperties.set(NativeProperties.NATIVE_HOME.name, "non_existed_path")
            }) {
            project.multiplatformExtension.applyDefaultHierarchyTemplate()
            project.multiplatformExtension.linuxX64()
        }
        project.checkDiagnostics("BrokenKotlinNativeBundleError")
    }


}