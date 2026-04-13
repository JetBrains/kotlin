/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing

import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.utils.processes.ExecAsyncHandle
import org.jetbrains.kotlin.gradle.utils.processes.ExecAsyncHandle.Companion.execAsync
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.OutputStream
import kotlin.concurrent.thread

open class TCServiceMessagesTestExecutionSpec(
    internal val processLaunchOptions: ProcessLaunchOptions,
    val processArgs: List<String>,
    val checkExitCode: Boolean,
    val clientSettings: TCServiceMessagesClientSettings,
    val dryRunArgs: List<String>? = null,
) : TestExecutionSpec {

    internal open fun createClient(
        testResultProcessor: TestResultProcessor,
        log: Logger,
    ): TCServiceMessagesClient =
        TCServiceMessagesClient(testResultProcessor, clientSettings, log)

    internal open fun wrapExecute(body: () -> Unit) = body()

    internal open fun showSuppressedOutput() = Unit
}

private val log = LoggerFactory.getLogger("org.jetbrains.kotlin.gradle.tasks.testing")

class TCServiceMessagesTestExecutor(
    val description: String,
    val runListeners: MutableList<KotlinTestRunnerListener>,
    val ignoreTcsmOverflow: Boolean,
    val ignoreRunFailures: Boolean,
    private val execOps: ExecOperations,
) : TestExecuter<TCServiceMessagesTestExecutionSpec> {

    private lateinit var execHandle: ExecAsyncHandle

    @Volatile
    private var process: Process? = null

    override fun execute(
        spec: TCServiceMessagesTestExecutionSpec,
        testResultProcessor: TestResultProcessor,
    ) {
        spec.wrapExecute {
            val client = spec.createClient(testResultProcessor, log)

            if (spec.dryRunArgs != null) {
                execHandle = execOps.execAsync("$description (dry run)") { exec ->
                    exec.workingDir = spec.processLaunchOptions.workingDir.orNull?.asFile
                    exec.environment(spec.processLaunchOptions.environment.orNull.orEmpty())
                    exec.executable = spec.processLaunchOptions.executable.get()
                    // get rid of redundant output during dry-run
                    exec.standardOutput = nullOutputStream
                    exec.args = spec.dryRunArgs
                }

                val exitValue = execHandle.start().waitForResult()?.exitValue ?: -1
                if (exitValue != 0) {
                    error(client.testFailedMessage(execHandle.displayName, exitValue))
                }
            }

            try {
                val handler = TCServiceMessageOutputStreamHandler(
                    client,
                    { spec.showSuppressedOutput() },
                    log,
                    ignoreTcsmOverflow,
                )

                // Use ProcessBuilder with redirectErrorStream(true) to merge stderr into stdout
                // at the OS pipe level. This eliminates the race condition where TC service messages
                // on stdout (e.g. testFinished) arrive before stderr output (e.g. stack traces)
                // has been fully read, which caused stderr to leak into the Gradle build log.
                // See KT-69896.
                val processBuilder = ProcessBuilder(
                    buildList {
                        add(spec.processLaunchOptions.executable.get())
                        addAll(spec.processArgs)
                    }
                ).apply {
                    redirectErrorStream(true)
                    spec.processLaunchOptions.workingDir.orNull?.asFile?.let { directory(it) }
                    spec.processLaunchOptions.environment.orNull?.let { env ->
                        environment().putAll(env)
                    }
                }

                // Log in the same format and logger as ExecAsyncHandle for compatibility
                // with tests that parse this log (e.g. JsBrowserTestsIT).
                LoggerFactory.getLogger(ExecAsyncHandle::class.java).debug(
                    "[ExecAsyncHandle {}] created ExecSpec. Command: {}, Environment: {}, WorkingDir: {}",
                    description,
                    processBuilder.command().joinToString(),
                    processBuilder.environment(),
                    processBuilder.directory()
                )

                val exitValue = client.root {
                    val proc = processBuilder.start()
                    process = proc

                    // Test processes don't read from stdin.
                    proc.outputStream.close()

                    val readerThread = thread(name = "$description-stdout-reader", isDaemon = true) {
                        try {
                            proc.inputStream.use { input ->
                                // Wrap in BufferedInputStream to reduce native read syscalls and help
                                // drain the OS pipe buffer promptly. Per Process docs, limited pipe
                                // buffer sizes on some platforms can cause the child process to block
                                // or deadlock if the stream is not read promptly.
                                // The default 8KB BufferedInputStream buffer is sufficient — copyTo
                                // also uses an 8KB internal buffer, so reads are already batched.
                                input.buffered().use { buffered ->
                                    buffered.copyTo(handler)
                                }
                            }
                        } catch (_: IOException) {
                            // Process was destroyed, stream closed — expected during stopNow()
                        } finally {
                            handler.close()
                        }
                    }

                    readerThread.join()
                    proc.waitFor()
                }

                if (spec.checkExitCode && exitValue != 0) {
                    error(client.testFailedMessage(description, exitValue))
                }
            } catch (e: Throwable) {
                spec.showSuppressedOutput()

                val wrappedError = client.ensureNodesClosed(null, e, false) ?: if (e is Error) e else Error(e)

                runListeners.forEach {
                    it.runningFailure(wrappedError)
                }

                if (ignoreRunFailures) {
                    log.error(wrappedError.message)
                } else {
                    throw e
                }
            } finally {
                // When Gradle times out a task, it interrupts the task thread rather than
                // calling stopNow(). With ProcessBuilder (unlike ExecAsyncHandle), thread
                // interruption does not automatically kill the child process.
                destroyProcessIfNeeded()
            }
        }
    }

    private fun destroyProcessIfNeeded() {
        process?.let { proc ->
            if (proc.isAlive) {
                log.info("[$description] destroying test process")
                proc.destroyForcibly()
            }
        }
        process = null
    }

    /**
     * Cancel the tests.
     *
     * *NOTE* Currently Gradle only calls this when `--fail-fast` is enabled.
     * KMP tests do not support `--fail-fast` KT-32108, so we expect this function is never called.
     * It's implemented in case the Gradle implementation changes.
     */
    override fun stopNow() {
        if (::execHandle.isInitialized) {
            execHandle.abort()
        }
        destroyProcessIfNeeded()
    }
}

/**
 * Returns a new [OutputStream] which discards all bytes.
 *
 * Replace with [OutputStream.nullOutputStream] when min JDK is 11+.
 */
private val nullOutputStream: OutputStream =
    object : OutputStream() {
        override fun write(b: Int) {
            // do nothing
        }
    }
