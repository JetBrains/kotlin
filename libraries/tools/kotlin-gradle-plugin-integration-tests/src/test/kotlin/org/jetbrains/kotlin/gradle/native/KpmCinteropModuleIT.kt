/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

@MppGradlePluginTests
@DisplayName("KPM Cinterop integration")
class KpmCinteropModuleIT : KGPBaseTest() {

    @GradleTest
    fun `check cinterop and commonization for KPM module`(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        nativeProject(
            projectName = "kpm-cinterop-module",
            gradleVersion = gradleVersion,
            localRepoDir = tempDir
        ) {
            build("compileLinuxMainKotlinNativeMetadata") {
                assertTasksExecuted(
                    ":cinteropSampleInteropLinuxArm64",
                    ":cinteropSampleInteropLinuxX64",
                    ":commonizeSampleInterop",
                    ":commonizeSampleInteropForLinux"
                )
            }

            projectPath.resolve("src/nativeInterop/cinterop/sampleInterop.h").writeText("")
            buildAndFail("compileLinuxMainKotlinNativeMetadata") {
                assertTasksFailed(":compileLinuxMainKotlinNativeMetadata")
                assertTasksExecuted(
                    ":cinteropSampleInteropLinuxArm64",
                    ":cinteropSampleInteropLinuxX64",
                    ":commonizeSampleInterop",
                    ":commonizeSampleInteropForLinux"
                )
                assertOutputContains("src/linuxMain/kotlin/Main.kt: (3, 22): Unresolved reference: sampleInterop")
                assertOutputContains("src/linuxMain/kotlin/Main.kt: (6, 15): Unresolved reference: sampleInterop")
            }
        }
    }
}