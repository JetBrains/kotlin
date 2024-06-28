/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.capitalize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.util.*

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("K/N tests of apple native common libs")
@NativeGradlePluginTests
class CommonNativeIT : KGPBaseTest() {

    private val String.withPrefix get() = "native-apple-devices-common/$this"

    @DisplayName("Common ios")
    @GradleTest
    fun testCommonIos(gradleVersion: GradleVersion) {
        doCommonNativeTest(
            "common-ios",
            libTargets = listOf("iosLibArm64", "iosLibX64"),
            appTargets = listOf("iosArm64", "iosX64"),
            gradleVersion
        )
    }

    @DisplayName("Common watchos")
    @GradleTest
    fun testCommonWatchos(gradleVersion: GradleVersion) {
        doCommonNativeTest(
            "common-watchos",
            libTargets = listOf("watchosLibArm32", "watchosLibArm64", "watchosLibX64"),
            appTargets = listOf("watchosArm32", "watchosArm64", "watchosX64"),
            gradleVersion
        )
    }

    @DisplayName("Common tvos")
    @GradleTest
    fun testCommonTvos(gradleVersion: GradleVersion) {
        doCommonNativeTest(
            "common-tvos",
            libTargets = listOf("tvosLibArm64", "tvosLibX64"),
            appTargets = listOf("tvosArm64", "tvosX64"),
            gradleVersion
        )
    }

    private fun doCommonNativeTest(
        projectName: String,
        libTargets: List<String>,
        appTargets: List<String>,
        gradleVersion: GradleVersion,
    ) {
        nativeProject(
            projectName.withPrefix,
            gradleVersion,
            localRepoDir = defaultLocalRepo(gradleVersion)
        ) {
            val libCompileTasks = libTargets.map { ":lib:compileKotlin${it.capitalize()}" }
            val appCompileTasks = appTargets.map { ":app:compileKotlin${it.capitalize()}" }
            val appLinkFrameworkTasks = appTargets.map { ":app:linkDebugFramework${it.capitalize()}" }
            val appLinkTestTasks = appTargets.map { ":app:linkDebugTest${it.capitalize()}" }
            build(":lib:publish") {
                assertTasksExecuted(libCompileTasks)
                libTargets.forEach {
                    assertOutputContains("Configuring $it")
                    assertFileInProjectExists("lib/build/classes/kotlin/$it/main/klib/lib.klib")
                }
            }

            build(":app:build", *appLinkTestTasks.toTypedArray()) {
                assertTasksExecuted(appCompileTasks)
                assertTasksExecuted(appLinkFrameworkTasks)
                assertTasksExecuted(appLinkTestTasks)

                appTargets.forEach {
                    assertFileInProjectExists("app/build/classes/kotlin/$it/main/klib/app.klib")
                    assertDirectoryInProjectExists("app/build/bin/$it/debugFramework")
                    assertDirectoryInProjectExists("app/build/bin/$it/debugTest")
                }
            }
        }
    }
}