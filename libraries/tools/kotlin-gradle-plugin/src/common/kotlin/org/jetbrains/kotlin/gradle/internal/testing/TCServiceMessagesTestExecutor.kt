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
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions.Companion.configure
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.OutputStream

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

    override fun execute(
        spec: TCServiceMessagesTestExecutionSpec,
        testResultProcessor: TestResultProcessor,
    ) {
        spec.wrapExecute {
            val client = spec.createClient(testResultProcessor, log)

            if (spec.dryRunArgs != null) {
                execHandle = execOps.execAsync("$description (dry run)") { exec ->
                    exec.configure(spec.processLaunchOptions)
                    // get rid of redundant output during dry-run
                    exec.standardOutput = nullOutputStream
                    exec.args = spec.dryRunArgs
                }

                val exitValue = execHandle.start().waitForResult()?.exitValue ?: -1
                if (exitValue != 0) {
                    error(client.testFailedMessage(execHandle, exitValue))
                }
            }

            try {
                execHandle = execOps.execAsync(description) { exec ->
                    exec.configure(spec.processLaunchOptions)
                    exec.args = spec.processArgs
                    exec.standardOutput = TCServiceMessageOutputStreamHandler(
                        client,
                        { spec.showSuppressedOutput() },
                        log,
                        ignoreTcsmOverflow,
                    )
                    exec.errorOutput = TCServiceMessageOutputStreamHandler(
                        client,
                        { spec.showSuppressedOutput() },
                        log,
                        ignoreTcsmOverflow,
                    )
                }

                val exitValue =
                    client.root {
                        execHandle.start().waitForResult()?.exitValue ?: -1
                    }

                if (spec.checkExitCode && exitValue != 0) {
                    error(client.testFailedMessage(execHandle, exitValue))
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
            }
        }
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
