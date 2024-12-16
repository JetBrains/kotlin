/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.testbase.EnvironmentalVariablesOverride
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.assertTasksFailed
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@DisplayName("Tests for the workaround for KT-73511 in 2.1.10, KT-73951")
@NativeGradlePluginTests
@Suppress("ClassName")
@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC]) // Native static caches are not available for Linux & Windows.
class WorkaroundForKt73511In2_1_10 : KGPBaseTest() {
    @GradleTest
    @TestMetadata(TEST_DATA_DIR)
    @OptIn(EnvironmentalVariablesOverride::class)
    fun testFailureIn2_1_0(gradleVersion: GradleVersion) = with(gradleVersion) {
        inProject("lib1", kotlinVersion = "2.0.0") { buildAndSuccess(":publish") }
        inProject("lib2", kotlinVersion = "2.0.0") { buildAndSuccess(":publish") }

        inProject("app", kotlinVersion = "2.1.0") {
            buildAndFail(":assemble") {
                assertTasksFailed(":linkDebugExecutableMacosArm64")
                assertOutputContains("Parent of this declaration is not a class: CONSTRUCTOR MISSING_DECLARATION")
            }
        }

        inProject("app", kotlinVersion = null) {
            buildAndSuccess(":assemble")
        }
    }

    private fun GradleVersion.inProject(
        projectName: String,
        kotlinVersion: String?,
        test: TestProject.() -> Unit = {},
    ) {
        project(
            "$TEST_DATA_DIR/$projectName",
            gradleVersion = this,
            buildOptions = defaultBuildOptions.copy(
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    version = kotlinVersion ?: defaultBuildOptions.nativeOptions.version,
                    cacheKind = NativeCacheKind.STATIC,
                )
            ),
        ) { test() }
    }

    private fun TestProject.buildAndSuccess(taskPath: String) {
        build(taskPath) {
            assertTasksExecuted(taskPath)
        }
    }

    companion object {
        const val TEST_DATA_DIR = "kt-73511-workaround"
    }
}
