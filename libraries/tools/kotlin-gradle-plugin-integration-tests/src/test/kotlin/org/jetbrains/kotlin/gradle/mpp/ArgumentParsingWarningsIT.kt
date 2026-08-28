/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.testFederation.AffectedByBuildToolsApi
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals

@MppGradlePluginTests
@AffectedByBuildToolsApi
@DisplayName("Argument-parsing warnings are reported by the Kotlin Gradle plugin")
class ArgumentParsingWarningsIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions
            .disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    @DisplayName("An argument passed twice through freeCompilerArgs is reported")
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED)
    fun testArgumentPassedMultipleTimesInFreeCompilerArgsReportsWarning(gradleVersion: GradleVersion) {
        multiplatformProject(
            gradleVersion,
            freeCompilerArgs = listOf("-language-version=2.4", "-language-version=2.5"),
        ) {
            buildAllCompileTasks {
                forEachCompileTask { taskOutput ->
                    assertWarningReportedOnce(taskOutput, passedMultipleTimesWarning("-language-version", "2.4", "2.5"))
                }
            }
        }
    }

    @GradleTest
    @DisplayName("An argument set both through compilerOptions and through freeCompilerArgs is reported")
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED)
    fun testArgumentSetViaTypedApiAndFreeCompilerArgsReportsWarning(gradleVersion: GradleVersion) {
        multiplatformProject(
            gradleVersion,
            languageVersion = KotlinVersion.KOTLIN_2_5,
            freeCompilerArgs = listOf("-language-version=2.4"),
        ) {
            buildAllCompileTasks {
                forEachCompileTask { taskOutput ->
                    assertWarningReportedOnce(taskOutput, passedMultipleTimesWarning("-language-version", "2.5", "2.4"))
                }
            }
        }
    }

    @GradleTest
    @DisplayName("An argument set only through compilerOptions is not reported")
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED)
    fun testArgumentSetOnlyViaTypedApiReportsNoWarning(gradleVersion: GradleVersion) {
        multiplatformProject(gradleVersion, languageVersion = KotlinVersion.KOTLIN_2_5) {
            buildAllCompileTasks {
                forEachCompileTask { taskOutput ->
                    assertNoWarningsMatching(taskOutput, LANGUAGE_VERSION_PASSED_MULTIPLE_TIMES)
                }
            }
        }
    }

    @GradleTest
    @DisplayName("An argument set only through freeCompilerArgs is not reported")
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED)
    fun testArgumentSetOnlyViaFreeCompilerArgsReportsNoWarning(gradleVersion: GradleVersion) {
        multiplatformProject(gradleVersion, freeCompilerArgs = listOf("-language-version=2.4")) {
            buildAllCompileTasks {
                forEachCompileTask { taskOutput ->
                    assertNoWarningsMatching(taskOutput, LANGUAGE_VERSION_PASSED_MULTIPLE_TIMES)
                }
            }
        }
    }

    @GradleTest
    @DisplayName("A module name inferred automatically and set through freeCompilerArgs is reported")
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED)
    fun testModuleNameSetViaTypedApiAndFreeCompilerArgsReportsWarning(gradleVersion: GradleVersion) {
        multiplatformProject(
            gradleVersion,
            freeCompilerArgs = listOf("-module-name=freeArgsModuleName"),
        ) {
            // the argument name is different per platform, let's check only JVM
            build(JVM_TASK) {
                assertOutputContainsExactlyTimes(passedMultipleTimesWarning("-module-name", "empty", "freeArgsModuleName"), 1)
            }
        }
    }

    @GradleTest
    @DisplayName("An unknown advanced flag is reported")
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED)
    fun testUnknownAdvancedFlagReportsWarning(gradleVersion: GradleVersion) {
        multiplatformProject(gradleVersion, freeCompilerArgs = listOf("-Xnot-a-real-flag")) {
            buildAllCompileTasks {
                forEachCompileTask { taskOutput ->
                    assertWarningReportedOnce(
                        taskOutput,
                        "Flag is not supported by this version of the compiler: -Xnot-a-real-flag".toRegex(RegexOption.LITERAL),
                    )
                }
            }
        }
    }

    @GradleTest
    @DisplayName("A removed argument is reported")
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED)
    fun testRemovedArgumentReportsWarning(gradleVersion: GradleVersion) {
        multiplatformProject(gradleVersion, freeCompilerArgs = listOf("-Xcontext-receivers")) {
            buildAllCompileTasks {
                forEachCompileTask { taskOutput ->
                    assertWarningReportedOnce(
                        taskOutput,
                        "The argument '-Xcontext-receivers' was removed in Kotlin .*\\. It has no effect\\.".toRegex(),
                    )
                }
            }
        }
    }

    @GradleTest
    @DisplayName("An unknown 'stable' argument fails the build")
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED)
    fun testUnknownStableArgumentFailsBuild(gradleVersion: GradleVersion) {
        multiplatformProject(gradleVersion, freeCompilerArgs = listOf("-not-a-real-flag")) {
            // `--continue` so that every platform is attempted, not just the first compilation to fail
            buildAndFail(*ALL_COMPILE_TASKS.toTypedArray(), "--continue") {
                assertTasksFailed(ALL_COMPILE_TASKS)
                assertOutputContains("Invalid argument: -not-a-real-flag")
            }
        }
    }

    @GradleTest
    @DisplayName("A deprecated argument name is reported even though the compiler receives the new one")
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED)
    fun testDeprecatedArgumentNameReportsWarning(gradleVersion: GradleVersion) {
        // `-Xjsr305-annotations` is a JVM-only argument
        multiplatformProject(gradleVersion, jvmOnlyFreeCompilerArgs = listOf("-Xjsr305-annotations=strict")) {
            build(JVM_TASK) {
                assertWarningReportedOnce(
                    getOutputForTask(JVM_TASK, LogLevel.INFO),
                    "-Xjsr305-annotations is deprecated\\. Please use -Xjsr305 instead".toRegex(),
                )
            }
        }
    }

    @GradleTest
    @DisplayName("A deprecated argument that the compiler reports itself is not duplicated")
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED)
    fun testDeprecatedLifecycleArgumentIsNotDuplicated(gradleVersion: GradleVersion) {
        multiplatformProject(gradleVersion, freeCompilerArgs = listOf("-Xsuppress-warning=NOTHING_TO_INLINE")) {
            buildAllCompileTasks {
                forEachCompileTask { taskOutput ->
                    assertWarningReportedOnce(
                        taskOutput,
                        "The argument '-Xsuppress-warning' is deprecated since Kotlin".toRegex(RegexOption.LITERAL),
                    )
                }
            }
        }
    }

    private fun multiplatformProject(
        gradleVersion: GradleVersion,
        freeCompilerArgs: List<String> = emptyList(),
        languageVersion: KotlinVersion? = null,
        jvmOnlyFreeCompilerArgs: List<String> = emptyList(),
        test: TestProject.() -> Unit,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    js {
                        nodejs()
                    }
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmJs {
                        nodejs()
                    }

                    compilerOptions {
                        this.freeCompilerArgs.addAll(freeCompilerArgs)
                        languageVersion?.let { this.languageVersion.set(it) }
                    }

                    if (jvmOnlyFreeCompilerArgs.isNotEmpty()) {
                        jvm {
                            compilerOptions { this.freeCompilerArgs.addAll(jvmOnlyFreeCompilerArgs) }
                        }
                    }

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }
            test()
        }
    }

    private fun TestProject.buildAllCompileTasks(assertions: BuildResult.() -> Unit) {
        build(*ALL_COMPILE_TASKS.toTypedArray()) {
            assertTasksExecuted(ALL_COMPILE_TASKS)
            assertions()
        }
    }

    private fun BuildResult.forEachCompileTask(
        taskPaths: List<String> = ALL_COMPILE_TASKS,
        assertion: (taskOutput: String) -> Unit,
    ) = taskPaths.forEach { taskPath ->
        assertion(getOutputForTask(taskPath, LogLevel.INFO))
    }

    private fun assertWarningReportedOnce(taskOutput: String, warning: Regex) {
        val matchedLines = taskOutput.warningLinesMatching(warning)
        assertEquals(1, matchedLines.size, "Expected exactly one line matching '$warning', but got $matchedLines")
    }

    private fun assertNoWarningsMatching(taskOutput: String, warning: Regex) {
        assertEquals(
            emptyList(),
            taskOutput.warningLinesMatching(warning),
            "Expected no lines matching '$warning'",
        )
    }

    private fun String.warningLinesMatching(warning: Regex) =
        lineSequence().filter { warning.containsMatchIn(it) }.toList()

    private fun passedMultipleTimesWarning(argument: String, vararg values: String): Regex {
        val renderedValues = values.joinToString(", ") { "'${Regex.escape(it)}'" }
        return ("Argument '${Regex.escape(argument)}' is passed multiple times: (?:'[^']*', )*" +
                "$renderedValues\\. The last value will be used\\.").toRegex()
    }

    private companion object {
        const val METADATA_TASK = ":compileCommonMainKotlinMetadata"
        const val JVM_TASK = ":compileKotlinJvm"
        const val JS_TASK = ":compileKotlinJs"
        const val WASM_JS_TASK = ":compileKotlinWasmJs"

        val LANGUAGE_VERSION_PASSED_MULTIPLE_TIMES = "Argument '-language-version' is passed multiple times:".toRegex()

        val ALL_COMPILE_TASKS = listOf(METADATA_TASK, JVM_TASK, JS_TASK, WASM_JS_TASK)
    }
}
