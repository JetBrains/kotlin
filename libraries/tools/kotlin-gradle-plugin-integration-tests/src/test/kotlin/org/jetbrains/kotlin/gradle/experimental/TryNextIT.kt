/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.experimental

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.gradle.BrokenOnMacosTest
import org.jetbrains.kotlin.gradle.BrokenOnMacosTestFailureExpectation
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.parseCompilerArgumentsFromBuildOutput
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.appendText

@DisplayName("'kotlin.experimental.tryNext' option")
class TryNextIT : KGPBaseTest() {

    @DisplayName("Produces single warning message in multi-project when enabled")
    @JvmGradlePluginTests
    @GradleTest
    fun singleWarningMultiproject(gradleVersion: GradleVersion) {
        project(
            "multiprojectWithDependency",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.WARN)
        ) {
            enableTryNext()

            build("--dry-run") {
                output.assertHasDiagnostic(KotlinToolingDiagnostics.ExperimentalTryNextWarning)
                assertOutputContains("No Kotlin compilation tasks have been run")
            }
        }
    }

    @DisplayName("JVM: language version default is changed to next")
    @JvmGradlePluginTests
    @GradleTest
    fun languageVersionChanged(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            enableTryNext()

            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")

                assertCompilerArgument(":compileKotlin", "-language-version $nextKotlinLanguageVersion")
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
                    "##### 'kotlin.experimental.tryNext' results #####"
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
            enableTryNext()

            build("build") {
                assertOutputContains(
                    """
                    |##### 'kotlin.experimental.tryNext' results #####
                    |:app:compileKotlin: $nextKotlinLanguageVersion language version
                    |:lib:compileKotlin: $nextKotlinLanguageVersion language version
                    |##### 100% (2/2) tasks have been compiled with Kotlin $nextKotlinLanguageVersion #####
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
            enableTryNext()

            build("build") {
                assertOutputContains(
                    """
                    |##### 'kotlin.experimental.tryNext' results #####
                    |:compileCommonMainKotlinMetadata: $nextKotlinLanguageVersion language version
                    |:compileKotlinLinuxX64: $nextKotlinLanguageVersion language version
                    |:compileKotlinMacosArm64: $nextKotlinLanguageVersion language version
                    |:compileKotlinMacosX64: $nextKotlinLanguageVersion language version
                    |:compileKotlinMingwX64: $nextKotlinLanguageVersion language version
                    |:compileNativeMainKotlinMetadata: $nextKotlinLanguageVersion language version
                    |##### 100% (6/6) tasks have been compiled with Kotlin $nextKotlinLanguageVersion #####
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
            enableTryNext()

            subProject("app").kotlinSourcesDir().resolve("foo/AA.kt").appendText(
                """
                |
                |ZZZZ
                """.trimMargin()
            )

            buildAndFail("build") {
                assertOutputContains(
                    """
                    |##### 'kotlin.experimental.tryNext' results #####
                    |:app:compileKotlin: $nextKotlinLanguageVersion language version
                    |:lib:compileKotlin: $nextKotlinLanguageVersion language version
                    |##### 100% (2/2) tasks have been compiled with Kotlin $nextKotlinLanguageVersion #####
                    """.trimMargin().normalizeLineEndings()
                )
            }
        }
    }

    @DisplayName("MPP: language version default is changed to next")
    @MppGradlePluginTests
    @GradleTest
    fun languageVersionChangedMpp(
        gradleVersion: GradleVersion,
        @TempDir localRepository: Path,
    ) {
        project(
            "new-mpp-published",
            gradleVersion,
            localRepoDir = localRepository,
            buildOptions = defaultBuildOptions.copy(
                logLevel = LogLevel.DEBUG,
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
            ),
        ) {
            enableTryNext()

            build(":compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")

                assertCompilerArgument(":compileCommonMainKotlinMetadata", "-language-version $nextKotlinLanguageVersion")
            }

            build(":compileKotlinJvm") {
                assertTasksExecuted(":compileKotlinJvm")

                assertCompilerArgument(":compileKotlinJvm", "-language-version $nextKotlinLanguageVersion")
            }

            build(":compileKotlinJs") {
                assertTasksExecuted(":compileKotlinJs")

                assertCompilerArgument(":compileKotlinJs", "-language-version $nextKotlinLanguageVersion")
            }

            build(":compileKotlinWasmJs") {
                assertTasksExecuted(":compileKotlinWasmJs")

                assertCompilerArgument(":compileKotlinWasmJs", "-language-version $nextKotlinLanguageVersion")
            }

            build(":compileKotlinLinuxX64") {
                assertTasksExecuted(":compileKotlinLinuxX64")

                val compileTaskOutput = getOutputForTask(":compileKotlinLinuxX64")
                val compilerArgs = parseCompilerArgumentsFromBuildOutput(K2NativeCompilerArguments::class, compileTaskOutput)
                assert(compilerArgs.languageVersion == nextKotlinLanguageVersion) {
                    ":compileKotlinLinuxX64 'languageVersion' is not '$nextKotlinLanguageVersion': ${compilerArgs.languageVersion}"
                }
            }
        }
    }

    @DisplayName("MPP: language version default is changed to next for metadata compilations")
    @MppGradlePluginTests
    @GradleTest
    fun languageVersionChangedMppMetadata(gradleVersion: GradleVersion) {
        project(
            "commonizeHierarchically",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            enableTryNext()

            build(":p1:compileAppleAndLinuxMainKotlinMetadata") {
                assertTasksExecuted(
                    ":p1:compileCommonMainKotlinMetadata",
                    ":p1:compileConcurrentMainKotlinMetadata",
                    ":p1:compileAppleAndLinuxMainKotlinMetadata",
                )

                assertCompilerArgument(":p1:compileCommonMainKotlinMetadata", "-language-version $nextKotlinLanguageVersion")
                assertCompilerArgument(":p1:compileConcurrentMainKotlinMetadata", "-language-version $nextKotlinLanguageVersion")
                val taskOutput = getOutputForTask(":p1:compileAppleAndLinuxMainKotlinMetadata")
                val appleAndLinuxMetadataArgs = parseCompilerArgumentsFromBuildOutput(K2NativeCompilerArguments::class, taskOutput)
                assert(appleAndLinuxMetadataArgs.languageVersion == nextKotlinLanguageVersion) {
                    ":compileAppleAndLinuxMainKotlinMetadata 'languageVersion' is not '$nextKotlinLanguageVersion': ${appleAndLinuxMetadataArgs.languageVersion}"
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
            enableTryNext()

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

    @DisplayName("JS: language version default is changed to next")
    @JsGradlePluginTests
    @GradleTest
    fun jsLanguageVersionK2(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-nodejs-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                logLevel = LogLevel.DEBUG,
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
            ),
        ) {
            enableTryNext()

            build(":compileKotlinJs") {
                assertTasksExecuted(":compileKotlinJs")

                assertCompilerArgument(":compileKotlinJs", "-language-version $nextKotlinLanguageVersion")
            }
        }
    }

    @DisplayName("JS: tryNext report is produced")
    @JsGradlePluginTests
    @GradleTest
    fun jsTryReport(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-nodejs-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
            ),
        ) {
            enableTryNext()

            build("build") {
                assertOutputContains(
                    """
                    |##### 'kotlin.experimental.tryNext' results #####
                    |:compileKotlinJs: $nextKotlinLanguageVersion language version
                    |:compileProductionExecutableKotlinJs: $nextKotlinLanguageVersion language version
                    |:compileTestDevelopmentExecutableKotlinJs: $nextKotlinLanguageVersion language version
                    |:compileTestKotlinJs: $nextKotlinLanguageVersion language version
                    |##### 100% (4/4) tasks have been compiled with Kotlin $nextKotlinLanguageVersion #####
                    """.trimMargin().normalizeLineEndings()
                )
            }
        }
    }

    @DisplayName("Native: check that only expected tasks use languageVersion")
    @NativeGradlePluginTests
    @GradleTest
    @TestMetadata("native-configuration-cache")
    @BrokenOnMacosTest(failureExpectation = BrokenOnMacosTestFailureExpectation.ALWAYS)
    fun smokeTestForNativeTasks(gradleVersion: GradleVersion) {
        project("native-configuration-cache", gradleVersion) {
            enableTryNext()
            build("build") {
                assertOutputContains(
                    """
                            |##### 'kotlin.experimental.tryNext' results #####
                            |:lib:compileCommonMainKotlinMetadata: $nextKotlinLanguageVersion language version
                            |:lib:compileKotlinIosArm64: $nextKotlinLanguageVersion language version
                            |:lib:compileKotlinIosSimulatorArm64: $nextKotlinLanguageVersion language version
                            |:lib:compileKotlinIosX64: $nextKotlinLanguageVersion language version
                            |:lib:compileKotlinLinuxX64: $nextKotlinLanguageVersion language version
                            |:lib:compileTestKotlinIosArm64: $nextKotlinLanguageVersion language version
                            |:lib:compileTestKotlinIosSimulatorArm64: $nextKotlinLanguageVersion language version
                            |:lib:compileTestKotlinIosX64: $nextKotlinLanguageVersion language version
                            |:lib:compileTestKotlinLinuxX64: $nextKotlinLanguageVersion language version
                            |##### 100% (9/9) tasks have been compiled with Kotlin $nextKotlinLanguageVersion #####
                        """.trimMargin().normalizeLineEndings()
                )
            }
        }
    }

    private fun TestProject.enableTryNext() = gradleProperties.appendText(
        """
        |
        |kotlin.experimental.tryNext=true
        """.trimMargin()
    )

    private val nextKotlinLanguageVersion = KotlinVersion.entries.first { it > KotlinVersion.DEFAULT }.version
}
