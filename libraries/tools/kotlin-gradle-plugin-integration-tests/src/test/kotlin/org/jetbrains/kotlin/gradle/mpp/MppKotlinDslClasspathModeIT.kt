/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata

@MppGradlePluginTests
class MppKotlinDslClasspathModeIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata("kotlin-mpp-classpathMode")
    fun testErrorInClasspathMode(
        gradleVersion: GradleVersion,
    ) {
        val configPhaseErrorMsg = "ERROR DURING CONFIGURATION PHASE"

        project(
            projectName = "kotlin-mpp-classpathMode",
            gradleVersion = gradleVersion,
        ) {
            buildAndFail("tasks") {
                assertOutputContains(configPhaseErrorMsg)
            }

            val classpathModeOptions = defaultBuildOptions.copy(
                freeArgs = listOf("-Dorg.gradle.kotlin.dsl.provider.mode=classpath"),
                logLevel = LogLevel.QUIET,
            )

            build("tasks", buildOptions = classpathModeOptions) {
                assertOutputDoesNotContain(configPhaseErrorMsg)
            }

            build("listCollectedErrors", buildOptions = classpathModeOptions) {
                assertOutputContains("Collected 1 exception(s)")
                assertOutputContains(configPhaseErrorMsg)
            }
        }
    }
}
