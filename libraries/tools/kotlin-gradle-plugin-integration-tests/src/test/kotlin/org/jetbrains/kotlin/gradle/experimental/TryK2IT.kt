/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.experimental

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.parseCompilerArgumentsFromBuildOutput
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("'kotlin.experimental.tryK2' option")
class TryK2IT : KGPBaseTest() {

    @DisplayName("Produces single warning message in multi-project when enabled")
    @JvmGradlePluginTests
    @GradleTest
    fun singleWarningMultiproject(gradleVersion: GradleVersion) {
        project(
            "multiprojectWithDependency",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.WARN)
        ) {
            enableTryK2()

            build("--dry-run") {
                output.assertHasDiagnostic(KotlinToolingDiagnostics.ExperimentalK2Warning)
                assertOutputContains("No Kotlin compilation tasks have been run")
            }
        }
    }

    @DisplayName("JVM: language version default is changed to 2.0")
    @JvmGradlePluginTests
    @GradleTest
    fun languageVersionChanged(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            enableTryK2()

            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")

                assertCompilerArgument(":compileKotlin", "-language-version 2.0")
            }
        }
    }

    @DisplayName("JVM: build report is not printed")
    @JvmGradlePluginTests
    @GradleTest
    fun buildReportNotPrinted(gradleVersion: GradleVersion) {
        project(
            "incrementalMultiproject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.WARN)
        ) {
            build("build") {
                assertOutputDoesNotContain(
                    "##### 'kotlin.experimental.tryK2' results #####"
                )
            }
        }
    }

    @DisplayName("JVM: report is printed at the end of the build")
    @JvmGradlePluginTests
    @GradleTest
    fun buildReport(gradleVersion: GradleVersion) {
        project(
            "incrementalMultiproject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.WARN)
        ) {
            enableTryK2()

            build("build") {
                assertOutputContains(
                    """
                    |##### 'kotlin.experimental.tryK2' results #####
                    |:app:compileKotlin: 2.0 language version
                    |:lib:compileKotlin: 2.0 language version
                    |##### 100% (2/2) tasks have been compiled with Kotlin 2.0 #####
                    """.trimMargin().normalizeLineEndings()
                )
            }
        }
    }

    @DisplayName("Native: report is printed at the end of the build")
    @MppGradlePluginTests
    @GradleTest
    fun buildReportForNative(gradleVersion: GradleVersion) {
        project(
            "k2-native-intermediate-metadata",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.WARN)
        ) {
            enableTryK2()

            build("build") {
                assertOutputContains(
                    """
                    |##### 'kotlin.experimental.tryK2' results #####
                    |:compileCommonMainKotlinMetadata: 2.0 language version
                    |:compileKotlinLinuxX64: 2.0 language version${
                        if (HostManager.hostIsMac)
                            "\n|:compileKotlinMacosArm64: 2.0 language version\n|:compileKotlinMacosX64: 2.0 language version"
                        else ""
                    }
                    |:compileKotlinMingwX64: 2.0 language version
                    |:compileNativeMainKotlinMetadata: 2.0 language version
                    |##### 100% ${if (HostManager.hostIsMac) "(6/6)" else "(4/4)"} tasks have been compiled with Kotlin 2.0 #####
                    """.trimMargin().normalizeLineEndings()
                )
            }
        }
    }

    @DisplayName("JVM: report is printed at the end of the build in case of compilation error")
    @JvmGradlePluginTests
    @GradleTest
    fun buildReportOnCompilationError(gradleVersion: GradleVersion) {
        project(
            "incrementalMultiproject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.WARN)
        ) {
            enableTryK2()

            subProject("app").kotlinSourcesDir().resolve("foo/AA.kt").appendText(
                """
                |
                |ZZZZ
                """.trimMargin()
            )

            buildAndFail("build") {
                assertOutputContains(
                    """
                    |##### 'kotlin.experimental.tryK2' results #####
                    |:app:compileKotlin: 2.0 language version
                    |:lib:compileKotlin: 2.0 language version
                    |##### 100% (2/2) tasks have been compiled with Kotlin 2.0 #####
                    """.trimMargin().normalizeLineEndings()
                )
            }
        }
    }

    @DisplayName("MPP: language version default is changed to 2.0")
    @MppGradlePluginTests
    @GradleTest
    fun languageVersionChangedMpp(gradleVersion: GradleVersion) {
        project(
            "new-mpp-published",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            enableTryK2()

            build(":compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")

                assertCompilerArgument(":compileCommonMainKotlinMetadata", "-language-version 2.0")
            }

            build(":compileKotlinJvm") {
                assertTasksExecuted(":compileKotlinJvm")

                assertCompilerArgument(":compileKotlinJvm", "-language-version 2.0")
            }

            build(":compileKotlinJs") {
                assertTasksExecuted(":compileKotlinJs")

                assertCompilerArgument(":compileKotlinJs", "-language-version 2.0")
            }

            build(":compileKotlinWasmJs") {
                assertTasksExecuted(":compileKotlinWasmJs")

                assertCompilerArgument(":compileKotlinWasmJs", "-language-version 2.0")
            }

            build(":compileKotlinLinuxX64") {
                assertTasksExecuted(":compileKotlinLinuxX64")

                val compileTaskOutput = getOutputForTask(":compileKotlinLinuxX64")
                val compilerArgs = parseCompilerArgumentsFromBuildOutput(K2NativeCompilerArguments::class, compileTaskOutput)
                assert(compilerArgs.languageVersion == "2.0") {
                    ":compileKotlinLinuxX64 'languageVersion' is not '2.0': ${compilerArgs.languageVersion}"
                }
            }
        }
    }

    @DisplayName("MPP: language version default is changed to 2.0 for metadata compilations")
    @MppGradlePluginTests
    @GradleTest
    fun languageVersionChangedMppMetadata(gradleVersion: GradleVersion) {
        project(
            "commonizeHierarchically",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            enableTryK2()

            build(":p1:compileAppleAndLinuxMainKotlinMetadata") {
                assertTasksExecuted(
                    ":p1:compileCommonMainKotlinMetadata",
                    ":p1:compileConcurrentMainKotlinMetadata",
                    ":p1:compileAppleAndLinuxMainKotlinMetadata",
                )

                assertCompilerArgument(":p1:compileCommonMainKotlinMetadata", "-language-version 2.0")
                assertCompilerArgument(":p1:compileConcurrentMainKotlinMetadata", "-language-version 2.0")
                val taskOutput = getOutputForTask(":p1:compileAppleAndLinuxMainKotlinMetadata")
                val appleAndLinuxMetadataArgs = parseCompilerArgumentsFromBuildOutput(K2NativeCompilerArguments::class, taskOutput)
                assert(appleAndLinuxMetadataArgs.languageVersion == "2.0") {
                    ":compileAppleAndLinuxMainKotlinMetadata 'languageVersion' is not '2.0': ${appleAndLinuxMetadataArgs.languageVersion}"
                }
            }
        }
    }

    @DisplayName("JVM: explicit user setting for languageVersion is not overridden")
    @JvmGradlePluginTests
    @GradleTest
    fun languageVersionOverride(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            enableTryK2()

            buildGradle.appendText(
                """
                |
                |kotlin.compilerOptions.languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
                """.trimMargin()
            )

            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")

                assertCompilerArgument(":compileKotlin", "-language-version 1.9")
            }
        }
    }

    @DisplayName("JS: language version default is changed to 2.0")
    @JsGradlePluginTests
    @GradleTest
    fun jsLanguageVersionK2(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-nodejs-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            enableTryK2()

            build(":compileKotlinJs") {
                assertTasksExecuted(":compileKotlinJs")

                assertCompilerArgument(":compileKotlinJs", "-language-version 2.0")
            }
        }
    }

    @DisplayName("JS: tryK2 report is produced")
    @JsGradlePluginTests
    @GradleTest
    fun jsTryReport(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-nodejs-project",
            gradleVersion,
        ) {
            enableTryK2()

            build("build") {
                assertOutputContains(
                    """
                    |##### 'kotlin.experimental.tryK2' results #####
                    |:compileKotlinJs: 2.0 language version
                    |:compileProductionExecutableKotlinJs: 2.0 language version
                    |:compileTestDevelopmentExecutableKotlinJs: 2.0 language version
                    |:compileTestKotlinJs: 2.0 language version
                    |##### 100% (4/4) tasks have been compiled with Kotlin 2.0 #####
                    """.trimMargin().normalizeLineEndings()
                )
            }
        }
    }

    @DisplayName("Native: check that only expected tasks use languageVersion")
    @NativeGradlePluginTests
    @GradleTest
    fun smokeTestForNativeTasks(gradleVersion: GradleVersion) {
        project("native-configuration-cache", gradleVersion) {
            enableTryK2()
            build("build") {
                if (HostManager.hostIsMac) {
                    assertOutputContains(
                        """
                            |##### 'kotlin.experimental.tryK2' results #####
                            |:lib:compileCommonMainKotlinMetadata: 2.0 language version
                            |:lib:compileKotlinIosArm64: 2.0 language version
                            |:lib:compileKotlinIosSimulatorArm64: 2.0 language version
                            |:lib:compileKotlinIosX64: 2.0 language version
                            |:lib:compileKotlinLinuxX64: 2.0 language version
                            |:lib:compileTestKotlinIosSimulatorArm64: 2.0 language version
                            |:lib:compileTestKotlinIosX64: 2.0 language version
                            |:lib:compileTestKotlinLinuxX64: 2.0 language version
                            |##### 100% (8/8) tasks have been compiled with Kotlin 2.0 #####
                        """.trimMargin().normalizeLineEndings()
                    )
                } else {
                    assertOutputContains(
                        """
                            |##### 'kotlin.experimental.tryK2' results #####
                            |:lib:compileKotlinLinuxX64: 2.0 language version
                            |:lib:compileCommonMainKotlinMetadata: 2.0 language version
                            |:lib:compileTestKotlinLinuxX64: 2.0 language version
                            |##### 100% (3/3) tasks have been compiled with Kotlin 2.0 #####
                        """.trimMargin().normalizeLineEndings()
                    )
                }
            }
        }
    }

    private fun TestProject.enableTryK2() = gradleProperties.appendText(
        """
        |
        |kotlin.experimental.tryK2=true
        """.trimMargin()
    )
}
