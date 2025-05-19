/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.js

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Timeout
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.assertEquals

/**
 * Test changes to files in continuous build mode will trigger recompilation.
 * Since Kotlin/JS uses external processes (Yarn, Webpack) we want to check they are managed correctly.
 *
 * Use [KGPDaemonsBaseTest] because:
 * - A fresh Gradle daemon prevents VFS issues
 *  (e.g. https://github.com/gradle/gradle/issues/26946).
 *- A bug (in KGP, or configuration of the external processes) could cause the Gradle build, and thus daemon, to hang.
 *  Using independent daemons per-test means one hanging test won't affect others.
 */
class JsContinuousBuildIT : KGPDaemonsBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            // Continuous build requires file watching is enabled.
            fileSystemWatchEnabled = true,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
        )

    @GradleTest
    @TestMetadata("js-run-continuous")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    fun testJsRunContinuousBuild(
        gradleVersion: GradleVersion,
    ) {
        project("js-run-continuous", gradleVersion) {

            val compiledJs = projectPath.resolve("build/compileSync/js/main/developmentExecutable/kotlin/js-run-continuous.js")

            val daemonRelease = PipedOutputStream()
            val daemonStdin = PipedInputStream(daemonRelease)

            val checker = thread(name = "testJsRunContinuousBuild checker", isDaemon = true) {
                // wait for the first compilation to succeed
                while (!compiledJs.exists()) {
                    Thread.sleep(1000)
                }

                // wait before file modification, to give Gradle a chance to catch up with file events
                Thread.sleep(5000)

                // modify a file to trigger a re-build
                projectPath.resolve("src/jsMain/kotlin/main.kt")
                    .replaceText("//println", "println")

                // wait for the second compilation to succeed
                while ("Hello again!!!" !in compiledJs.readText()) {
                    Thread.sleep(1000)
                }

                // close the stream, which will allow Gradle to finish the build
                daemonRelease.close()
            }

            build(
                "jsBrowserDevelopmentRun",
                buildOptions = defaultBuildOptions.copy(
                    verboseVfsLogging = true,
                    continuousBuild = true,
                ),
                inputStream = daemonStdin,
                forwardBuildOutput = true,
            ) {
                checker.join()

                assertFileContains(
                    compiledJs,
                    /* language=text */ """
                    |  function main() {
                    |    println('Hello, world!');
                    |    println('Hello again!!!');
                    |  }
                    """.trimMargin()
                )

                // verify yarn dependency resolution can run
                assertTasksExecuted(":kotlinStoreYarnLock")

                // verify there's no error in the ExecAsyncHandle thread management
                assertOutputDoesNotContain("Exception in thread")

                // Verify webpack starts and is aborted.
                // (Webpack is launched using DeploymentHandle and runs continuously until Gradle stops the handle.)
                val expectedMessage = if (gradleVersion != GradleVersion.version(TestVersions.Gradle.G_8_12)) {
                    // language=text
                    """
                    |[ExecAsyncHandle webpack webpack/bin/webpack.js jsmain] started
                    |[ExecAsyncHandle webpack webpack/bin/webpack.js jsmain] finished {exitValue=?, failure=null}
                    |[ExecAsyncHandle webpack webpack/bin/webpack.js jsmain] aborted
                    """.trimMargin()
                } else {
                    // language=text
                    """
                    |[ExecAsyncHandle webpack webpack/bin/webpack.js jsmain] started
                    |[ExecAsyncHandle webpack webpack/bin/webpack.js jsmain] aborted
                    |[ExecAsyncHandle webpack webpack/bin/webpack.js jsmain] failed org.gradle.internal.UncheckedException: java.lang.InterruptedException
                    """.trimMargin()
                }
                assertEquals(
                    expectedMessage,
                    output
                        .filterLinesStartingWith("[ExecAsyncHandle webpack webpack/bin/webpack.js jsmain]")
                        // For some reason webpack doesn't close with a consistent exit code.
                        // We don't really care about the exit code, only that it _does_ exit.
                        // So, replace the exit code with a '?' to make the assertion stable.
                        .replace(Regex("\\{exitValue=-?\\d+"), "\\{exitValue=?"),
                )

                // verify the DeploymentHandle that manages webpack starts and stops successfully
                assertEquals(
                    // language=text
                    """
                    |[:jsBrowserDevelopmentRun] webpack-dev-server started webpack webpack/bin/webpack.js jsmain
                    |[:jsBrowserDevelopmentRun] webpack-dev-server stopped webpack webpack/bin/webpack.js jsmain
                    """.trimMargin(),
                    output.filterLinesStartingWith("[:jsBrowserDevelopmentRun] webpack")
                )
            }
        }

        // @GradleTest automatically deletes the project directory.
        // However, sometimes it does this too quickly.
        // So, give some time to allow the Gradle daemon to close successfully, avoiding the error:
        // org.gradle.internal.build.BuildLayoutValidator$BuildLayoutException: Directory '...' does not contain a Gradle build.
        Thread.sleep(5000)
    }

    companion object {
        /**
         * Fetch all lines starting with [prefix].
         */
        private fun String.filterLinesStartingWith(prefix: String): String =
            lines()
                .filter { line -> line.startsWith(prefix) }
                .joinToString("\n")
    }
}
