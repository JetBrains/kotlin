/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.file.File
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.writeText

@DisplayName("Tests for the K/N CInterop tool during IDEA project import")
@NativeGradlePluginTests
class CInteropIdeaSyncIT : KGPBaseTest() {

    private val ideaSyncBuildOptions = defaultBuildOptions.copy(freeArgs = listOf("-Didea.sync.active=true"))

    @DisplayName("Idea sync warning failing cinterop warning")
    @GradleTest
    fun shouldPrintWarningForCInteroperability(gradleVersion: GradleVersion) {
        val ideaSyncWarningMessage = "Warning: Failed to generate cinterop for :cinteropSampleInteropNative"
        val interopTaskName = ":cinteropSampleInteropNative"

        nativeProject("cinterop-failing", gradleVersion) {
            /* Task still fails on 'normal' builds */
            buildAndFail("commonize") {
                assertTasksFailed(interopTaskName)
            }

            /* Task failure is just a warning during import */
            build("commonize", buildOptions = ideaSyncBuildOptions) {
                assertTasksExecuted(interopTaskName)
                assertOutputContains(ideaSyncWarningMessage)
            }

            /*
            The implementation before fixing KT-52243 considered the cinterop task as *not* up-to-date
            when it was previously running in the IDE and therefore failing leniently. It would have always tried to re-run
            this task to anticipate untracked environmental changes.

            This cannot be easily implemented whilst also fixing KT-52243, which is more desirable.
            A new mechanism for 'run tasks at import' leniency is proposed (using --continue), which is supposed to replace
            the special cinterop mechanism.

            https://youtrack.jetbrains.com/issue/KT-52243/
            https://github.com/JetBrains/kotlin/pull/4812#issuecomment-1117287222
             */
            runCatching {
                /* Task is not considered up-to-date after lenient failure */
                build("commonize", buildOptions = ideaSyncBuildOptions) {
                    assertTasksExecuted(interopTaskName)
                    assertOutputContains(ideaSyncWarningMessage)
                }
            }

            /* Remove noise that causes failure */
            projectPath.resolve("src/nativeInterop/cinterop/sampleInteropNoise.h").writeText("")

            build("commonize", buildOptions = ideaSyncBuildOptions) {
                assertTasksExecuted(interopTaskName)
                assertOutputDoesNotContain(ideaSyncWarningMessage)
            }

            /* Task is considered up-to-date after first successful run */
            build("commonize", buildOptions = ideaSyncBuildOptions) {
                assertTasksUpToDate(interopTaskName)
                assertOutputDoesNotContain(ideaSyncWarningMessage)
            }

            /* Task is still considered up-to-date after successful run in idea sync */
            build("commonize") {
                assertTasksUpToDate(interopTaskName)
                assertOutputDoesNotContain(ideaSyncWarningMessage)
            }
        }
    }
}