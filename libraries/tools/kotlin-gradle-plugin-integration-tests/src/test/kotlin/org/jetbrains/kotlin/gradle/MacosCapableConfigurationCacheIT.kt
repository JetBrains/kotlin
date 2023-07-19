/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@DisplayName("Configuration cache tests that can be run on MacOS")
@NativeGradlePluginTests
@OsCondition(supportedOn = [OS.LINUX, OS.MAC, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.MAC, OS.WINDOWS])
class MacosCapableConfigurationCacheIT : AbstractConfigurationCacheIT() {

    @DisplayName("works with native tasks in complex project")
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_7_4,
        additionalVersions = [TestVersions.Gradle.G_7_6],
    )
    @GradleTest
    fun testNativeTasks(gradleVersion: GradleVersion) {
        val expectedTasks = mutableListOf(
            ":lib:cinteropMyCinteropLinuxX64",
            ":lib:commonizeCInterop",
            ":lib:compileKotlinLinuxX64",
            ":lib:linkExecutableDebugExecutableLinuxX64",
            ":lib:linkSharedDebugSharedLinuxX64",
            ":lib:linkStaticDebugStaticLinuxX64",
            ":lib:linkDebugTestLinuxX64",
        )

        if (HostManager.hostIsMac) {
            expectedTasks += listOf(
                ":lib:cinteropMyCinteropIosX64",
                ":lib:compileKotlinIosX64",
                ":lib:assembleMyframeDebugFrameworkIosArm64",
                ":lib:assembleMyfatframeDebugFatFramework",
                ":lib:assembleLibDebugXCFramework",
                ":lib:compileTestKotlinIosX64",
                ":lib:linkDebugTestIosX64",
                ":lib:transformCommonMainDependenciesMetadata",
                ":lib:transformCommonMainCInteropDependenciesMetadata",
                ":lib:linkDebugFrameworkIosArm64",
                ":lib:linkDebugFrameworkIosX64",
                ":lib:linkDebugFrameworkIosFat",
                ":lib:linkReleaseFrameworkIosArm64",
                ":lib:linkReleaseFrameworkIosX64",
                ":lib:linkReleaseFrameworkIosFat",
            )
        }

        project("native-configuration-cache", gradleVersion) {
            testConfigurationCacheOf(
                "build",
                executedTaskNames = expectedTasks,
            )
        }
    }

    @OptIn(EnvironmentalVariablesOverride::class)
    @DisplayName("works with apple framework embedding and signing")
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_7_4,
        additionalVersions = [TestVersions.Gradle.G_7_6],
    )
    @GradleTest
    fun testAppleFrameworkTasks(gradleVersion: GradleVersion, @TempDir targetBuildDir: Path) {
        project(
            projectName = "sharedAppleFramework",
            gradleVersion = gradleVersion,
            environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "Debug",
                "SDK_NAME" to "iphoneos",
                "ARCHS" to "arm64",
                "EXPANDED_CODE_SIGN_IDENTITY" to "-",
                "TARGET_BUILD_DIR" to targetBuildDir.toString(),
                "FRAMEWORKS_FOLDER_PATH" to "testFrameworksDir"
            ),
        ) {
            testConfigurationCacheOf(":shared:embedAndSignAppleFrameworkForXcode")
        }
    }
}