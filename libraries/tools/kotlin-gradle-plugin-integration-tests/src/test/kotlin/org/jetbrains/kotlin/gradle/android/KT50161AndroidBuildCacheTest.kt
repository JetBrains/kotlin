/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@AndroidGradlePluginTests
@DisplayName("Android Build Cache Test for KT-50161")
class KT50161AndroidBuildCacheTest : KGPBaseTest() {

    @GradleAndroidTest
    @DisplayName("test build cache w/ enabled variant filter")
    fun testBuildCacheWithVariantFilter(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
        @TempDir localBuildCacheDir: Path
    ) {
        project(
            "kt-50161-androidBuildCacheWithVariantFilter",
            gradleVersion,
            defaultBuildOptions.copy(
                androidVersion = agpVersion,
                buildCacheEnabled = true,
            ),
            buildJdk = jdkVersion.location,
        ) {
            enableLocalBuildCache(localBuildCacheDir)

            build("assembleDebug") {
                assertTasksExecuted(":app:assembleDebug")
                getCompileKotlinTasks().forEach { task ->
                    assertTasksExecuted(task.path)
                }
            }
            build("clean")

            build("assembleDebug") {
                getCompileKotlinTasks().forEach { task ->
                    assertTasksFromCache(task.path)
                }
            }
            build("clean")

            build("assembleDebug", "-PenableVariantFilter") {
                assertOutputContains("enableVariantFilter")
                getCompileKotlinTasks().forEach { task ->
                    assertTasksFromCache(task.path)
                }
            }
        }
    }

    private fun BuildResult.getCompileKotlinTasks(): List<BuildTask> {
        val compileTasks = tasks.filter { it.path.lowercase().contains("compile") && it.path.lowercase().contains("kotlin") }
        if (compileTasks.isEmpty()) fail("Expected at least one compile task to be executed. Found $tasks")
        return compileTasks
    }
}
