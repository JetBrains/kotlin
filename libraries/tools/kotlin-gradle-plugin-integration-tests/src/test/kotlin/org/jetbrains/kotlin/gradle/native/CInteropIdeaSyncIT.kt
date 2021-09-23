/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import kotlin.test.Test

class CInteropIdeaSyncIT : BaseGradleIT() {

    private val ideaSyncBuildOptions = defaultBuildOptions().copy(
        freeCommandLineArgs = listOf("-Didea.sync.active=true")
    )

    @Test
    fun `test failing cinterop warning`() {
        val ideaSyncWarningMessage = "Warning: Failed to generate cinterop for :cinteropSampleInteropNative"
        val interopTaskName = ":cinteropSampleInteropNative"

        with(Project("cinterop-failing")) {
            /* Task still fails on 'normal' builds */
            build("commonize") {
                assertTasksFailed(interopTaskName)
            }

            /* Task failure is just a warning during import */
            build("commonize", options = ideaSyncBuildOptions) {
                assertSuccessful()
                assertTasksExecuted(interopTaskName)
                assertContains(ideaSyncWarningMessage)
            }

            /* Task is not considered up-to-date after lenient failure */
            build("commonize", options = ideaSyncBuildOptions) {
                assertSuccessful()
                assertTasksExecuted(interopTaskName)
                assertContains(ideaSyncWarningMessage)
            }

            /* Remove noise that causes failure */
            projectDir.resolve("src/nativeInterop/cinterop/sampleInteropNoise.h").writeText("")

            build("commonize", options = ideaSyncBuildOptions) {
                assertSuccessful()
                assertTasksExecuted(interopTaskName)
                assertNotContains(ideaSyncWarningMessage)
            }

            /* Task is considered up-to-date after first successful run */
            build("commonize", options = ideaSyncBuildOptions) {
                assertSuccessful()
                assertTasksUpToDate(interopTaskName)
                assertNotContains(ideaSyncWarningMessage)
            }

            /* Task is still considered up-to-date after successful run in idea sync */
            build("commonize") {
                assertSuccessful()
                assertTasksUpToDate(interopTaskName)
                assertNotContains(ideaSyncWarningMessage)
            }
        }
    }
}
