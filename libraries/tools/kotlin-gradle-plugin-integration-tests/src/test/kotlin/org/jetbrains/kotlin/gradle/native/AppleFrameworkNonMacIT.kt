/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.CocoapodsPluginDiagnostics
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@OsCondition(supportedOn = [OS.LINUX, OS.WINDOWS])
@DisplayName("K/N tests for Apple Framework on non Mac Os")
@NativeGradlePluginTests
class AppleFrameworkNonMacIT : KGPBaseTest() {

    @DisplayName("podImport task execution without fail")
    @GradleTest
    fun checkSuccessPodImport(gradleVersion: GradleVersion) {

        nativeProject("native-cocoapods-tests", gradleVersion) {
            val buildOptions = defaultBuildOptions.copy(
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    cocoapodsGenerateWrapper = true
                )
            )

            build("podImport", buildOptions = buildOptions) {
                assertHasDiagnostic(CocoapodsPluginDiagnostics.UnsupportedOs)
            }
        }
    }

    @DisplayName("XCFramework and FatFramework tasks are SKIPPED")
    @GradleTest
    fun checkXCFrameworkAndFatFrameworkTasksAreSkipped(gradleVersion: GradleVersion) {

        project("appleXCFramework", gradleVersion) {
            build("assembleSharedDebugXCFramework") {
                assertTasksSkipped(":shared:linkDebugFrameworkIosArm64")
                assertTasksSkipped(":shared:linkDebugFrameworkIosX64")
                assertTasksSkipped(":shared:linkDebugFrameworkWatchosArm32")
                assertTasksSkipped(":shared:linkDebugFrameworkWatchosArm64")
                assertTasksSkipped(":shared:linkDebugFrameworkWatchosDeviceArm64")
                assertTasksSkipped(":shared:linkDebugFrameworkWatchosX64")
                assertTasksSkipped(":shared:assembleDebugWatchosFatFrameworkForSharedXCFramework")
                assertTasksSkipped(":shared:assembleSharedDebugXCFramework")
            }
        }
    }
}