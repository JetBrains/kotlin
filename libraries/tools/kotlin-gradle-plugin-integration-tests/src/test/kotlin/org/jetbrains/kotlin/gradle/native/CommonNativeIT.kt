/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.Test

class CommonNativeIT : BaseGradleIT() {

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    private fun doCommonNativeTest(
        projectName: String,
        libTargets: List<String>,
        appTargets: List<String>
    ) = with(transformProjectWithPluginsDsl(projectName, directoryPrefix = "new-mpp-common-native")) {
        val libCompileTasks = libTargets.map { ":lib:compileKotlin${it.capitalize()}" }
        val appCompileTasks = appTargets.map { ":app:compileKotlin${it.capitalize()}" }
        val appLinkFrameworkTasks = appTargets.map { ":app:linkDebugFramework${it.capitalize()}" }
        val appLinkTestTasks = appTargets.map { ":app:linkDebugTest${it.capitalize()}" }

        build(":lib:publish") {
            assertSuccessful()
            assertTasksExecuted(libCompileTasks)
            libTargets.forEach {
                assertContains("Configuring $it")
                assertFileExists("lib/build/classes/kotlin/$it/main/lib.klib")
            }
        }

        build(":app:build", *appLinkTestTasks.toTypedArray()) {
            assertSuccessful()
            assertTasksExecuted(appCompileTasks)
            assertTasksExecuted(appLinkFrameworkTasks)
            assertTasksExecuted(appLinkTestTasks)

            appTargets.forEach {
                assertFileExists("app/build/classes/kotlin/$it/main/app.klib")
                assertFileExists("app/build/bin/$it/debugFramework")
                assertFileExists("app/build/bin/$it/debugTest")
            }
        }
    }

    @Test
    fun testCommonIos() {
        Assume.assumeTrue(HostManager.hostIsMac)
        doCommonNativeTest(
            "common-ios",
            libTargets = listOf("iosLibArm64", "iosLibX64"),
            appTargets = listOf("iosArm64", "iosX64")
        )
    }

    @Test
    fun testCommonWatchos() {
        Assume.assumeTrue(HostManager.hostIsMac)
        doCommonNativeTest(
            "common-watchos",
            libTargets = listOf("watchosLibArm32", "watchosLibArm64", "watchosLibX86"),
            appTargets = listOf("watchosArm32", "watchosArm64", "watchosX86")
        )
    }

    @Test
    fun testCommonTvos() {
        Assume.assumeTrue(HostManager.hostIsMac)
        doCommonNativeTest(
            "common-tvos",
            libTargets = listOf("tvosLibArm64", "tvosLibX64"),
            appTargets = listOf("tvosArm64", "tvosX64")
        )
    }
}

