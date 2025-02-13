/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.js

import com.intellij.util.io.readCharSequence
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Timeout
import java.io.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.absolute
import kotlin.io.path.copyTo
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource


/**
 * Use [KGPDaemonsBaseTest], because maybe a fresh daemon will avoid fs watch overflow issues?
 */
class JsContinuousBuildIT : KGPDaemonsBaseTest() {

    @GradleTest
    @TestMetadata("js-run-continuous")
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
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
                    if (timeoutMark.hasPassedNow()) {
                        System.err.writer().write("Daemon timeout!")
                        exitProcess(123123)
                    }
                }
            }

            val daemonRelease = PipedOutputStream()
            val daemonStdin = PipedInputStream(daemonRelease)
            val pipedWriter = PipedWriter()
            val pipedReader = PipedReader(pipedWriter)

            val logsBuffer = workingDir.resolve("logsBugger")
            val compiledFileBeforeChange = workingDir.resolve("compiledFileBeforeChange")
            val compiledFileAfterChange = workingDir.resolve("compiledFileAfterChange")

            val checker = thread(name = "testJsRunContinuousBuild checker", isDaemon = true) {
                // wait for the first compilation to succeed
                FileWriter(logsBuffer.toFile(), true).use { logs ->
                    pipedReader.buffered().lineSequence().takeWhile {
                        !it.contains("Waiting for changes to input files...")
                    }.forEach { logs.write(it + "\n") }

                    compiledJs.copyTo(compiledFileBeforeChange)

                    // modify a file to trigger a re-build
                    projectPath.resolve("src/jsMain/kotlin/main.kt")
                        .replaceText("//println", "println")

                    // wait for the second compilation to succeed
                    pipedReader.buffered().lineSequence().takeWhile {
                        !it.contains("Waiting for changes to input files...")
                    }.forEach { logs.write(it + "\n") }

                    compiledJs.copyTo(compiledFileAfterChange)

                    // close the stream, which will allow Gradle to close the stream
                    daemonRelease.close()
                    kotlin.runCatching {
                        // Continue reading, so that Gradle doesn't explode with broken pipe. Eventually Gradle will close the write end and read end will throw broken pipe.
                        pipedReader.readCharSequence().last()
                    }
                }
            }

            build(
                "jsBrowserDevelopmentRun",
                "--continuous",
                buildOptions = defaultBuildOptions.copy(
                    verboseVfsLogging = true,
                ),
                inputStream = daemonStdin,
                stdoutRedirect = pipedWriter,
                forceOutput = EnableGradleDebug.ENABLED,
            ) {
                // trigger the Gradle build kill-switch
                endTestKillSwitchFile.createNewFile()
                checker.join()

                assertFileContains(
                    compiledFileBeforeChange,
                    /* language=text */ """
                    |  function main() {
                    |    println('Hello, world!');
                    |  }
                    """.trimMargin()
                )

                assertFileContains(
                    compiledFileAfterChange,
                    /* language=text */ """
                    |  function main() {
                    |    println('Hello, world!');
                    |    println('Hello again!!!');
                    |  }
                    """.trimMargin()
                )

                // FIXME: Assert against logsBuffer
            }
        }
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
