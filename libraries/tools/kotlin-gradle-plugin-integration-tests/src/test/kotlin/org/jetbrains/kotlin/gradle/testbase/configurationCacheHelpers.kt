/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.BaseGradleIT

/**
 * Tests whether configuration cache for the tasks specified by [buildArguments] works on simple scenario when project is built twice non-incrementally.
 */
fun TestProject.assertSimpleConfigurationCacheScenarioWorks(
    vararg buildArguments: String,
    buildOptions: BuildOptions,
    executedTaskNames: List<String>? = null,
    checkUpToDateOnRebuild: Boolean = true,
) {
    // First, run a build that serializes the tasks state for configuration cache in further builds

    val executedTask: List<String> = executedTaskNames ?: buildArguments.toList()

    build(*buildArguments, buildOptions = buildOptions) {
        assertTasksExecuted(*executedTask.toTypedArray())
        if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_5)) {
            assertOutputContains(
                "Calculating task graph as no configuration cache is available for tasks: ${buildArguments.joinToString(separator = " ")}"
            )
        } else {
            assertOutputContains(
                "Calculating task graph as no cached configuration is available for tasks: ${buildArguments.joinToString(separator = " ")}"
            )
        }

        assertConfigurationCacheStored()
    }

    build("clean", buildOptions = buildOptions)

    // Then run a build where tasks states are deserialized to check that they work correctly in this mode
    build(*buildArguments, buildOptions = buildOptions) {
        assertTasksExecuted(*executedTask.toTypedArray())
        assertConfigurationCacheReused()
    }

    if (checkUpToDateOnRebuild) {
        build(*buildArguments, buildOptions = buildOptions) {
            assertTasksUpToDate(*executedTask.toTypedArray())
        }
    }
}

fun BuildResult.assertConfigurationCacheStored() {
    assertOutputContains("Configuration cache entry stored.")
}

fun BuildResult.assertConfigurationCacheReused() {
    assertOutputContains("Reusing configuration cache.")
}

val BuildOptions.withConfigurationCache: BuildOptions
    get() = copy(configurationCache = true, configurationCacheProblems = BaseGradleIT.ConfigurationCacheProblems.FAIL)