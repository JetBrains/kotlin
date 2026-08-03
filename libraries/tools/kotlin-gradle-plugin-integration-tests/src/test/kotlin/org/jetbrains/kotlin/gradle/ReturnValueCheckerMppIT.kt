/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@MppGradlePluginTests
@DisplayName("Return value checker DSL in multiplatform projects")
class ReturnValueCheckerMppIT : KGPBaseTest() {

    @DisplayName("returnValueChecker reaches the JVM and JS compilations and reports unused return values end-to-end")
    @GradleTest
    fun returnValueCheckerReachesJvmAndJs(gradleVersion: GradleVersion) {
        project(
            "returnValueCheckerMpp",
            gradleVersion,
            // The Kotlin/JS Node.js infrastructure registers root-project tasks that are incompatible with Isolated
            // Projects (unrelated to the return value checker), so it is disabled here while keeping the configuration cache.
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG).disableIsolatedProjects()
        ) {
            buildGradle.appendText(
                """
                |
                |kotlin.returnValueChecker()
                """.trimMargin()
            )

            build("compileKotlinJvm", "compileKotlinJs") {
                assertCompilerArgument(":compileKotlinJvm", "-Xreturn-value-checker=check")
                assertCompilerArgument(":compileKotlinJs", "-Xreturn-value-checker=check")
                // common sources are compiled with each platform, so the warning is reported in both compilations
                assertTaskOutputContains(":compileKotlinJvm", "Unused return value")
                assertTaskOutputContains(":compileKotlinJs", "Unused return value")
            }
        }
    }
}
