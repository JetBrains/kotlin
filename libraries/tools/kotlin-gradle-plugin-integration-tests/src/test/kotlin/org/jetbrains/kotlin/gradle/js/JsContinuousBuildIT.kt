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
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.system.exitProcess
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * Use [KGPDaemonsBaseTest], because maybe a fresh daemon will avoid fs watch overflow issues?
 */
class JsContinuousBuildIT : KGPDaemonsBaseTest() {

    @GradleTest
    @TestMetadata("js-run-continuous")
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    fun testJsRunContinuousBuild(
        gradleVersion: GradleVersion,
    ) {

        project("js-run-continuous", gradleVersion) {

            // Use this file to trigger a kill switch to force-kill the Gradle build upon test completion,
            // because `--no-daemon` is disabled by `--continuous`.
            val endTestKillSwitchFile = projectPath.resolve("end-test-kill-switch").absolute().toFile()

            val compiledJs = projectPath.resolve("build/compileSync/js/main/developmentExecutable/kotlin/js-run-continuous.js")

            buildScriptInjection {
                thread(name = "testJsRunContinuousBuild kill switch", isDaemon = true) {
                    val timeoutMark = TimeSource.Monotonic.markNow() + 5.minutes
                    while (timeoutMark.hasPassedNow() || !endTestKillSwitchFile.exists()) {
                        Thread.sleep(1000)
                    }
                    exitProcess(123123)
                }
            }

            val daemonRelease = PipedOutputStream()
            val daemonStdin = PipedInputStream(daemonRelease)

            val checker = thread(name = "testJsRunContinuousBuild checker", isDaemon = true) {
                // wait for the first compilation to succeed
                while (!compiledJs.exists()) {
                    Thread.sleep(1000)
                }

                // wait before file modification, to give Gradle a chance to catch up with file events
                // (not sure if this is necessary... this test is flaky af)
                Thread.sleep(5000)

                // modify a file to trigger a re-build
                projectPath.resolve("src/jsMain/kotlin/main.kt")
                    .replaceText("//println", "println")

                // wait for the second compilation to succeed
                while ("Hello again!!!" !in compiledJs.readText()) {
                    Thread.sleep(1000)
                }

                // close the stream, which will allow Gradle to close the stream
                daemonRelease.close()
            }

            build(
                "jsBrowserDevelopmentRun",
                "--continuous",
                buildOptions = defaultBuildOptions.copy(verboseVfsLogging = true),
                inputStream = daemonStdin,
            ) {
                // trigger the Gradle build kill-switch
                endTestKillSwitchFile.createNewFile()

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

                // verify yarn dependency resolution starts and stops successfully
                assertEquals(
                    // language=text
                    """
                    |[ExecHandle Resolving NPM dependencies using yarn] Changing state from Starting to Started
                    |[ExecHandle Resolving NPM dependencies using yarn] Started. Waiting until streams are handled...
                    |[ExecHandle Resolving NPM dependencies using yarn] Starting process 'Resolving NPM dependencies using yarn'.
                    |[ExecHandle Resolving NPM dependencies using yarn] Changing state from Initial to Starting
                    |[ExecHandle Resolving NPM dependencies using yarn] Waiting until process started
                    |[ExecHandle Resolving NPM dependencies using yarn] Successfully started process
                    |[ExecHandle Resolving NPM dependencies using yarn] Changing state from Started to Succeeded
                    |[ExecHandle Resolving NPM dependencies using yarn] finished with exit value 0 (state: Succeeded)
                    """.trimMargin(),
                    output.filterLinesStartingWith("[ExecHandle Resolving NPM dependencies using yarn]"),
                )

                // Verify webpack starts and is aborted.
                // (Webpack is launched using DeploymentHandle and runs continuously, so Gradle will always abort it.)
                assertEquals(
                    // language=text
                    """
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Changing state from Starting to Started
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Started. Waiting until streams are handled...
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Starting process 'webpack webpack/bin/webpack.js jsmain'.
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Changing state from Initial to Starting
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Waiting until process started
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Successfully started process
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Abort requested. Destroying process.
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Changing state from Started to Aborted
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] finished with exit value ? (state: Aborted)
                    """.trimMargin(),
                    output
                        .filterLinesStartingWith("[ExecHandle webpack webpack/bin/webpack.js jsmain]")
                        // For some reason webpack doesn't close with a consistent exit code.
                        // We don't really care about the exit code, only that it _does_ exit.
                        // So, replace the exit code with a '?' to make the assertion stable.
                        .replace(Regex("finished with exit value -?\\d+ "), "finished with exit value ? "),
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
