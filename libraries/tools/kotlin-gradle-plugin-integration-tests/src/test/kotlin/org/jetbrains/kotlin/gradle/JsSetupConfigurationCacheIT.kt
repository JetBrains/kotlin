/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.ConfigurationCacheProblems
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Files
import kotlin.io.path.isDirectory

// This test is a JS test
// but, as it is the only one to required to run on MacOS
// we are running this test as a part of Native tests
// This allows us to keep CI configuration simpler.
@OsCondition(supportedOn = [OS.LINUX, OS.MAC, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.MAC, OS.WINDOWS])
@NativeGradlePluginTests
class JsSetupConfigurationCacheIT : KGPBaseTest() {

    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(
            configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
            configurationCacheProblems = ConfigurationCacheProblems.FAIL
        )

    /**
     * Cleans up symbolic links or shortcuts within the working directory after each test execution.
     *
     * This method is specifically designed for Windows operating systems. It traverses the
     * working directory, collects any symbolic links or shortcuts that are directories but not
     * actual directories, and deletes them. The operation ensures that resources are cleaned up
     * properly and avoids any potential issues with stale shortcuts or symbolic links.
     *
     * Utilizes the Files.walk method to stream through the directory structure and ensure efficient
     * resource usage with automatic stream closure upon completion of operations.
     *
     * FIXME: Find a real cause and remove this workaround
     * KT-73795 Fix failing checkNodeJsSetup test on Windows
     *
     */
    @AfterEach
    fun cleanup() {
        if (OS.WINDOWS.isCurrentOs) {
            Files.walk(workingDir).use { stream -> // Automatically closes the stream
                val links = stream
                    .filter { it != it.toRealPath() && it.isDirectory() } // Check for symbolic links or shortcuts
                    .toList() // Collect into a list to prevent modification of the stream during iteration

                links.forEach { Files.deleteIfExists(it) } // Delete the collected shortcuts or links
            }
        }
    }

    // hack to be run on Mac m*
    @DisplayName("Check Node.JS setup on different platforms")
    @GradleTest
    fun checkNodeJsSetup(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-browser-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                jsOptions = defaultBuildOptions.jsOptions?.copy(
                    yarn = false
                )
            )
        ) {
            checkNodeJsSetup("kotlinUpgradePackageLock")
        }
    }

    // hack to be run on Mac m*
    @DisplayName("Check Node.JS setup on different platforms with Yarn")
    @GradleTest
    fun checkNodeJsSetupYarn(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-browser-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                jsOptions = defaultBuildOptions.jsOptions?.copy(
                    yarn = true
                )
            )
        ) {
            checkNodeJsSetup("kotlinUpgradeYarnLock")
        }
    }

    private fun TestProject.checkNodeJsSetup(
        upgradeTask: String
    ) {
        build(upgradeTask) {
            assertTasksExecuted(":$upgradeTask")
            assertConfigurationCacheStored()
        }

        build(upgradeTask) {
            assertTasksUpToDate(":$upgradeTask")
            assertConfigurationCacheReused()
        }
    }
}