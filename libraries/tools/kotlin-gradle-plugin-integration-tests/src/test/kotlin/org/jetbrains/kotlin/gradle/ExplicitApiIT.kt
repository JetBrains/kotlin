/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Explicit API DSL")
@JvmGradlePluginTests
class ExplicitApiIT : KGPBaseTest() {

    @DisplayName("Explicit api warning mode produces warnings")
    @GradleTest
    fun explicitApiWarning(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                """
                |
                |kotlin.explicitApiWarning()
                """.trimMargin()
            )

            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")

                assertCompilerArgument(":compileKotlin", "-Xexplicit-api=warning")
            }
        }
    }

    @DisplayName("Explicit api strict mode produces errors on violation")
    @GradleTest
    fun explicitApiStrict(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                """
                |
                |kotlin.explicitApi()
                """.trimMargin()
            )

            buildAndFail("compileKotlin") {
                assertTasksFailed(":compileKotlin")

                assertCompilerArgument(":compileKotlin", "-Xexplicit-api=strict")
                assertOutputContains("Visibility must be specified in explicit API mode")
            }
        }
    }

    @DisplayName("KT-57653: Explicit api warning mode is not overridden by freeCompilerArgs")
    @GradleTest
    fun explicitApiWarningFreeArgsOverride(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=groovy
                """
                |
                |kotlin.explicitApiWarning()
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile.class).configureEach {
                |    compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")
                |}
                |
                """.trimMargin()
            )

            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")

                assertCompilerArgument(":compileKotlin", "-Xcontext-receivers")
                assertCompilerArgument(":compileKotlin", "-Xexplicit-api=warning")
            }
        }
    }

    @DisplayName("MPP: explicit api warning works for MPP tasks")
    @GradleTest
    @MppGradlePluginTests
    fun explicitApiMpp(gradleVersion: GradleVersion) {
        project(
            "new-mpp-lib-and-app/sample-lib",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=groovy
                """
                |
                |kotlin.explicitApiWarning()
                """.trimMargin()
            )

            build(":compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
                assertCompilerArgument(":compileCommonMainKotlinMetadata", "-Xexplicit-api=warning")
            }

            build(":compileKotlinJvm6") {
                assertTasksExecuted(":compileKotlinJvm6")
                assertCompilerArgument(":compileKotlinJvm6", "-Xexplicit-api=warning")
            }

            build(":compileKotlinNodeJs") {
                assertTasksExecuted(":compileKotlinNodeJs")
                assertCompilerArgument(":compileKotlinNodeJs", "-Xexplicit-api=warning")
            }

            val nativeTaskName = when (HostManager.host) {
                KonanTarget.LINUX_X64 -> ":compileKotlinLinux64"
                KonanTarget.MACOS_ARM64 -> ":compileKotlinMacos64"
                KonanTarget.MINGW_X64 -> ":compileKotlinMingw64"
                else -> null
            }
            if (nativeTaskName != null) {
                build(nativeTaskName) {
                    assertTasksExecuted(nativeTaskName)
                    assertNativeTasksCommandLineArguments(nativeTaskName) {
                        assertCommandLineArgumentsContain("-Xexplicit-api=warning", commandLineArguments = it)
                    }
                }
            }
        }
    }
}
