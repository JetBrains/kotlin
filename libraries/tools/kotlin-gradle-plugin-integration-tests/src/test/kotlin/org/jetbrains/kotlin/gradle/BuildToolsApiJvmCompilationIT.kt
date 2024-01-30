/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internals.asFinishLogMessage
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.tasks.USING_JVM_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText

@DisplayName("JVM compilation via the Build Tools API")
@JvmGradlePluginTests
class BuildToolsApiJvmCompilationIT : KGPBaseTest() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(runViaBuildToolsApi = true)

    @GradleTest
    @DisplayName("Build Tools version consistency checker works if the old way of compilation is used together with the build tools API transform")
    fun versionConsistencyDiagnosticWorks(gradleVersion: GradleVersion) {
        project(
            "simpleProject", gradleVersion, buildOptions = defaultBuildOptions.copy(
                runViaBuildToolsApi = false,
                incremental = false,
            ),
        ) {
            build("assemble") {
                assertNoDiagnostic(KotlinToolingDiagnostics.BuildToolsApiVersionInconsistency)
            }
            chooseCompilerVersion(TestVersions.Kotlin.STABLE_RELEASE)
            buildAndFail("assemble") {
                assertHasDiagnostic(KotlinToolingDiagnostics.BuildToolsApiVersionInconsistency)
                assertOutputContains("Expected version: ${defaultBuildOptions.kotlinVersion}")
                assertOutputContains("Actual resolved version: ${TestVersions.Kotlin.STABLE_RELEASE}")
            }
        }
    }

    @GradleTest
    @DisplayName("Build Tools version consistency checker disabled if compilation goes via the build tools api")
    fun versionConsistencyDiagnosticDisabled(gradleVersion: GradleVersion) {
        project(
            "simpleProject", gradleVersion, buildOptions = defaultBuildOptions.copy(
                incremental = false,
            )
        ) {
            chooseCompilerVersion(TestVersions.Kotlin.STABLE_RELEASE)
            build("assemble") {
                assertNoDiagnostic(KotlinToolingDiagnostics.BuildToolsApiVersionInconsistency)
            }
        }
    }

    @GradleTest
    @DisplayName("Gradle side outputs backup works")
    fun outputsBackupWorks(gradleVersion: GradleVersion) {
        project(
            "simpleProject", gradleVersion, buildOptions = defaultBuildOptions.copy(
                usePreciseOutputsBackup = false,
                keepIncrementalCompilationCachesInMemory = false,
            )
        ) {
            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")
            }
            val newBrokenSrc = kotlinSourcesDir().resolve("broken.kt")
            newBrokenSrc.writeText(
                //language=kt
                """
                broken code
                """.trimIndent()
            )
            buildAndFail("compileKotlin") {
                assertTasksFailed(":compileKotlin")
            }
            newBrokenSrc.deleteExisting()
            build("compileKotlin") {
                assertTasksUpToDate(":compileKotlin")
            }
        }
    }

    @GradleTest
    @DisplayName("Simple project non-incremental in-process compilation")
    fun compileJvmInProcessNonIncremental(gradleVersion: GradleVersion) = testSimpleProject(
        gradleVersion, defaultBuildOptions.copy(
            compilerExecutionStrategy = KotlinCompilerExecutionStrategy.IN_PROCESS,
            incremental = false,
        )
    ) {
        assertOutputContains(KotlinCompilerExecutionStrategy.IN_PROCESS.asFinishLogMessage)
        assertOutputDoesNotContain(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
    }

    @GradleTest
    @DisplayName("Simple project incremental in-process compilation")
    fun compileJvmInProcessIncremental(gradleVersion: GradleVersion) = testSimpleProject(
        gradleVersion, defaultBuildOptions.copy(
            compilerExecutionStrategy = KotlinCompilerExecutionStrategy.IN_PROCESS,
            incremental = true,
        )
    ) {
        assertOutputContains(KotlinCompilerExecutionStrategy.IN_PROCESS.asFinishLogMessage)
        assertOutputContains(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
    }

    @GradleTest
    @DisplayName("Simple project non-incremental compilation within the daemon")
    fun compileJvmNonIncrementalWithinDaemon(gradleVersion: GradleVersion) = testSimpleProject(
        gradleVersion, defaultBuildOptions.copy(
            compilerExecutionStrategy = KotlinCompilerExecutionStrategy.DAEMON,
            incremental = false,
        )
    ) {
        assertOutputContains(KotlinCompilerExecutionStrategy.DAEMON.asFinishLogMessage)
        assertOutputDoesNotContain(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
    }

    @GradleTest
    @DisplayName("Simple project incremental compilation within the daemon")
    fun compileJvmIncrementalWithinDaemon(gradleVersion: GradleVersion) = testSimpleProject(
        gradleVersion, defaultBuildOptions.copy(
            compilerExecutionStrategy = KotlinCompilerExecutionStrategy.DAEMON,
            incremental = true,
        )
    ) {
        assertOutputContains(KotlinCompilerExecutionStrategy.DAEMON.asFinishLogMessage)
        assertOutputContains(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
    }

    private fun testSimpleProject(gradleVersion: GradleVersion, buildOptions: BuildOptions, assertions: BuildResult.() -> Unit) {
        project("simpleProject", gradleVersion, buildOptions = buildOptions) {
            build("assemble", "compileDeployKotlin") {
                assertFileExists(kotlinClassesDir("main").resolve("demo/KotlinGreetingJoiner.class"))
                assertFileExists(javaClassesDir("main").resolve("demo/Greeter.class"))
                assertFileExists(javaClassesDir("main").resolve("demo/HelloWorld.class"))
                assertFileExists(kotlinClassesDir("deploy").resolve("demo/ExampleSource.class"))
                assertions()
            }
        }
    }
}