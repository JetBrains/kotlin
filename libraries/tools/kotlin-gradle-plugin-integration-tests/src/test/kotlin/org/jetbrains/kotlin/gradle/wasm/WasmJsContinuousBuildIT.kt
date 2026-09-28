/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.wasm

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinBrowserBundler
import org.jetbrains.kotlin.gradle.js.AbstractWebContinuousBuildIT
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Timeout
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

class WasmJsContinuousBuildIT : AbstractWebContinuousBuildIT() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            // Continuous build requires file watching is enabled.
            fileSystemWatchEnabled = true,
        ).disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    // Timeout is much longer than expected test duration because sometimes KGP needs to download JS tools.
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    fun testWasmJsRunContinuousBuildWithoutBundler(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "wasm-browser-simple-project"
            }

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmJs {
                        binaries.executable()
                        browser(bundler = KotlinBrowserBundler.NONE)
                    }
                }
            }

            projectPath.resolve("src/wasmJsMain/kotlin/A.kt")
                .also {
                    it.createParentDirectories()
                }
                .writeText(
                    """
                    fun main() {
                        println("Hello, world")
                    }
                    """.trimIndent()
                )

            val compiledWasm =
                projectPath.resolve("build/compileSync/wasmJs/main/developmentExecutable/kotlin/wasm-browser-simple-project.wasm")

            val daemonRelease = PipedOutputStream()
            val daemonStdin = PipedInputStream(daemonRelease)

            val checker = thread(name = "testWasmJsRunContinuousBuild checker", isDaemon = true) {
                try {
                    val buildStartMark = TimeSource.Monotonic.markNow()
                    fun checkBuildDuration() {
                        check(buildStartMark.elapsedNow() < 10.minutes) {
                            "build took too long - ${buildStartMark.elapsedNow()}"
                        }
                    }

                    println("Waiting for the first compilation to succeed...")
                    while (!compiledWasm.exists()) {
                        Thread.sleep(1000)
                        checkBuildDuration()
                    }
                    println("First compilation completed.")

                    println("Waiting before file modification...")
                    // wait to give Gradle a chance to catch up with file events
                    Thread.sleep(5000)

                    // modify a file to trigger a re-build
                    projectPath.resolve("src/wasmJsMain/kotlin/A.kt")
                        .replaceText("Hello, world", "Hello again!!!")
                    println("Modified main.kt")

                    println("Waiting for the second compilation to succeed...")
                    // The string should be presented in binary wasm file
                    while ("Hello again!!!" !in compiledWasm.readText()) {
                        Thread.sleep(1000)
                        checkBuildDuration()
                    }
                    println("Second compilation completed")
                } catch (t: Throwable) {
                    println("Exception in ${Thread.currentThread().name}:\n${t.stackTraceToString()}")
                    throw t
                } finally {
                    println("Releasing daemon stdin stream...")
                    // close the stream, which will allow Gradle to finish the build
                    daemonRelease.close()
                    println("Released daemon stdin stream.")
                }
            }

            build(
                "wasmJsBrowserDevelopmentRun",
                buildOptions = defaultBuildOptions.copy(
                    verboseVfsLogging = true,
                    continuousBuild = true,
                ),
                inputStream = daemonStdin,
                forwardBuildOutput = true,
            ) {
                checker.join()

                assertFileContains(
                    compiledWasm,
                    """Hello again!!!""".trimMargin()
                )

                // verify yarn dependency resolution can run
                assertTasksExecuted(":kotlinWasmStoreYarnLock")
                assertTasksExecuted(":wasmJsGenerateImportMapServer")

                // verify there's no error in the ExecAsyncHandle thread management
                assertOutputDoesNotContain("Exception in thread")

                assertOutputContains("Development server started at http://localhost:8080")
            }
        }

        // @GradleTest automatically deletes the project directory.
        // However, sometimes it does this too quickly.
        // So, give some time to allow the Gradle daemon to close successfully, avoiding the error:
        // org.gradle.internal.build.BuildLayoutValidator$BuildLayoutException: Directory '...' does not contain a Gradle build.
        Thread.sleep(5000)
    }

    @GradleTest
    @TestMetadata("wasm-run-continuous")
    // Timeout is much longer than expected test duration because sometimes KGP needs to download JS tools.
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    fun testWasmJsRunContinuousBuild(
        gradleVersion: GradleVersion,
    ) {
        doTest(
            gradleVersion,
            "wasm",
            "wasmJs"
        ) {
            assertFileContains(
                it,
                "Hello, world!"
            )

            assertFileContains(
                it,
                "Hello again!!!"
            )
        }
    }
}
