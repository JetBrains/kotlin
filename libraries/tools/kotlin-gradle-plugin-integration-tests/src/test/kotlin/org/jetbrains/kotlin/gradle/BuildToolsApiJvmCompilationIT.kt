/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internals.asFinishLogMessage
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.tasks.USING_JVM_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInBuildToolsApi
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.writeText

@DisplayName("JVM compilation via the Build Tools API")
@JvmGradlePluginTests
@MustRunOnChangesInGradle
@MustRunOnChangesInBuildToolsApi
class BuildToolsApiJvmCompilationIT : KGPBaseTest() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(runViaBuildToolsApi = true)

    @TestMetadata("incrementalMultiproject")
    @GradleTest
    @DisplayName("Classpath snapshotting works with a compiler older than the snapshotting options known to KGP")
    fun classpathSnapshottingWithOlderCompilerVersion(gradleVersion: GradleVersion) {
        project(
            "incrementalMultiproject", gradleVersion,
            buildOptions = defaultBuildOptions.copy(expandTypeAliasesInClasspathSnapshots = true),
        ) {
            listOf("lib", "app").forEach { subProjectName ->
                subProject(subProjectName).buildGradle.append(
                    // language=Gradle
                    """
                    kotlin {
                        compilerVersion.set("${"2.4.20-RC"}")
                        coreKotlinLibrariesVersion.set("${"2.4.20-RC"}")
                    }
                    """.trimIndent()
                )
            }

            build("assemble") {
                assertTasksExecuted(":lib:compileKotlin", ":app:compileKotlin")
            }
        }
    }

    @TestMetadata("simpleProject")
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

    @TestMetadata("simpleProject")
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

    @TestMetadata("simpleProject")
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

    @TestMetadata("simpleProject")
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

    @TestMetadata("simpleProject")
    @GradleTest
    @DisplayName("Test logging produces errors with e: prefix and no [KOTLIN] prefix")
    fun compileErrorCorrectPrefix(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Fail),
        ) {
            kotlinSourcesDir().resolve("helloWorld.kt").writeText("fun f() = fffffffffffffffff")
            buildAndFail("assemble", forwardBuildOutput = true) {
                assertOutputContainsExactlyTimes(
                    Regex("\ne: file://.*/src/main/kotlin/helloWorld.kt:1:11 Unresolved reference 'fffffffffffffffff'."),
                    1
                )
            }
        }
    }

    @TestMetadata("simpleProject")
    @GradleWithCompilerVersionTest
    @CompilerVersions(versions = ["2.2.0", "2.1.0"])
    @DisplayName("KT-80932: Compiler warnings have a single w: prefix")
    fun compileWarningHasSinglePrefix(gradleVersion: GradleVersion, compilerVersion: String) {
        project("simpleProject", gradleVersion, compilerVersion = compilerVersion) {
            kotlinSourcesDir().resolve("src.kt").writeText(
                """
                @Deprecated("Use bar() instead", ReplaceWith("bar()"))
                fun foo() {}

                fun bar() {}

                fun baz() {
                    foo()
                }
                """.trimIndent()
            )
            build("compileKotlin") {
                val srcKtUri = kotlinSourcesDir().resolve("src.kt").toRealPath().toUri()
                assertOutputContainsExactlyTimes(
                    "\nw: $srcKtUri:7:5 'fun foo(): Unit' is deprecated. Use bar() instead."
                )
            }
        }
    }

    @GradleTest
    @DisplayName("Unsupported compiler flag with an older compiler does not trigger the daemon fallback")
    fun compileWithUnsupportedFlagAndOlderCompilerVersionWithFallback(gradleVersion: GradleVersion) =
        testUnsupportedFlagWithPinnedCompiler(gradleVersion, THROWING_COMPILER, KotlinCompilerExecutionStrategy.DAEMON, useFallback = true)

    @GradleTest
    @DisplayName("Unsupported compiler flag with an older compiler fails with a version hint in-process")
    fun compileWithUnsupportedFlagAndOlderCompilerVersionInProcess(gradleVersion: GradleVersion) =
        testUnsupportedFlagWithPinnedCompiler(gradleVersion, THROWING_COMPILER, KotlinCompilerExecutionStrategy.IN_PROCESS)

    @GradleTest
    @DisplayName("Unsupported compiler flag with a compiler that logs argument errors fails with a version hint")
    fun compileWithUnsupportedFlagAndCompilerLoggingArgumentErrors(gradleVersion: GradleVersion) =
        testUnsupportedFlagWithPinnedCompiler(gradleVersion, TestVersions.Kotlin.STABLE_RELEASE, KotlinCompilerExecutionStrategy.DAEMON)

    @GradleTest
    @DisplayName("Source error with a compiler that logs argument errors is not reported as rejected compiler arguments")
    fun compileErrorWithCompilerLoggingArgumentErrors(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion, compilerVersion = TestVersions.Kotlin.STABLE_RELEASE) {
            kotlinSourcesDir().resolve("helloWorld.kt").writeText("fun f() = fffffffffffffffff")
            buildAndFail("compileKotlin") {
                assertOutputContains("Unresolved reference 'fffffffffffffffff'")
                assertNoDiagnostic(KotlinToolingDiagnostics.KotlinCompilerArgumentsRejected)
            }
        }
    }

    @GradleTest
    @DisplayName("Unsupported compiler flag with the compiler matching KGP has no version hint")
    fun compileWithUnsupportedFlagAndDefaultCompilerVersion(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.append("kotlin { compilerOptions.freeCompilerArgs.add(\"-no-such-flag\") }")
            buildAndFail("compileKotlin") {
                assertOutputContains("Invalid argument: -no-such-flag")
                assertNoDiagnostic(KotlinToolingDiagnostics.KotlinCompilerArgumentsRejected)
                assertNoDiagnostic(KotlinToolingDiagnostics.KotlinCompilationInDaemonHasFailed)
            }
        }
    }

    private fun testUnsupportedFlagWithPinnedCompiler(
        gradleVersion: GradleVersion,
        compilerVersion: String,
        strategy: KotlinCompilerExecutionStrategy,
        useFallback: Boolean = false,
    ) {
        val buildOptions = defaultBuildOptions.copy(
            compilerExecutionStrategy = strategy,
            useDaemonFallbackStrategy = useFallback,
            stacktraceMode = null,
        )
        project("simpleProject", gradleVersion, buildOptions = buildOptions, compilerVersion = compilerVersion) {
            buildGradle.append("kotlin { compilerOptions.freeCompilerArgs.add(\"-no-such-flag\") }")
            buildAndFail("compileKotlin") {
                assertHasDiagnostic(KotlinToolingDiagnostics.KotlinCompilerArgumentsRejected)
                assertNoDiagnostic(KotlinToolingDiagnostics.KotlinCompilationInDaemonHasFailed)
                assertOutputDoesNotContain("Using fallback strategy")
                assertOutputDoesNotContain("CompilerArgumentsParseException")
                assertOutputContains("Invalid argument: -no-such-flag")
                assertOutputContains("This build uses Kotlin compiler $compilerVersion with Kotlin Gradle Plugin ${buildOptions.kotlinVersion}.")
            }
        }
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

private const val THROWING_COMPILER = "2.2.21"
