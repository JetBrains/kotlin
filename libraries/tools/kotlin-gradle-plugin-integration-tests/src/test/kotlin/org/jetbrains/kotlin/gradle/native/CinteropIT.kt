/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.condition.OS

@NativeGradlePluginTests
class CinteropIT : KGPBaseTest() {

    @GradleTest
    fun `rerun cinterop after a header changing`(gradleVersion: GradleVersion) {
        nativeProject("cinterop-kt-53191", gradleVersion = gradleVersion) {
            val headerFile = projectPath.resolve("native_lib/nlib.h").toFile()

            build(":compileKotlinLinux") {
                assertTasksExecuted(":cinteropNlibLinux")
            }
            build(":compileKotlinLinux") {
                assertTasksUpToDate(":cinteropNlibLinux", ":compileKotlinLinux")
            }

            headerFile.writeText("void foo();")
            buildAndFail(":compileKotlinLinux") {
                assertTasksExecuted(":cinteropNlibLinux")
                assertOutputContains("src/linuxMain/kotlin/org/sample/Platform.kt:3:10 Unresolved reference 'sample'")
            }

            headerFile.writeText("void sample(int i);")
            build(":compileKotlinLinux") {
                assertTasksExecuted(":cinteropNlibLinux")
            }
        }
    }

    @GradleTest
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    fun `cinterop gives hint about -fmodules`(gradleVersion: GradleVersion) {
        nativeProject("cinterop-fmodules", gradleVersion = gradleVersion) {
            val defFile = projectPath.resolve("native_lib/nlib.def").toFile()

            buildAndFail(":cinteropNlibIosX64") {
                assertOutputContains("Try adding `-compiler-option -fmodules` to cinterop.")
            }

            defFile.appendText("\ncompilerOpts = -fmodules")
            build(":cinteropNlibIosX64") {
                assertOutputDoesNotContain("Try adding `-compiler-option -fmodules` to cinterop.")
            }
        }
    }
}