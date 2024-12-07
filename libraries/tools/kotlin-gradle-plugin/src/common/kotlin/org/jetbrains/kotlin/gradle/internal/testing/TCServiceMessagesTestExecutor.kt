/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing

import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.process.ExecResult
import org.gradle.process.ProcessForkOptions
import org.gradle.process.internal.ExecHandle
import org.gradle.process.internal.ExecHandleFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.OutputStream

open class TCServiceMessagesTestExecutionSpec(
    val forkOptions: ProcessForkOptions,
    val args: List<String>,
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
    val execHandleFactory: ExecHandleFactory,
    val runListeners: MutableList<KotlinTestRunnerListener>,
    val ignoreTcsmOverflow: Boolean,
    val ignoreRunFailures: Boolean,
) : TestExecuter<TCServiceMessagesTestExecutionSpec> {
    private lateinit var execHandle: ExecHandle
    var outputReaderThread: Thread? = null
    var shouldStop = false

    override fun execute(spec: TCServiceMessagesTestExecutionSpec, testResultProcessor: TestResultProcessor) {
        spec.wrapExecute {
            val client = spec.createClient(testResultProcessor, log)

            if (spec.dryRunArgs != null) {
                val exec = execHandleFactory.newExec()
                spec.forkOptions.copyTo(exec)
                // get rid of redundant output during dry-run
                exec.standardOutput = object : OutputStream() {
                    override fun write(b: Int) {
                        // do nothing
                    }
                }
                exec.args = spec.dryRunArgs
                execHandle = exec.build()

                execHandle.start()
                val result: ExecResult = execHandle.waitForFinish()
                if (result.exitValue != 0) {
                    error(client.testFailedMessage(execHandle, result.exitValue))
                }
            }

            try {
                val exec = execHandleFactory.newExec()
                spec.forkOptions.copyTo(exec)
                exec.args = spec.args
                exec.standardOutput = TCServiceMessageOutputStreamHandler(
                    client,
                    { spec.showSuppressedOutput() },
                    log,
                    ignoreTcsmOverflow
                )
                exec.errorOutput = TCServiceMessageOutputStreamHandler(
                    client,
                    { spec.showSuppressedOutput() },
                    log,
                    ignoreTcsmOverflow
                )
                execHandle = exec.build()

                lateinit var result: ExecResult
                client.root {
                    execHandle.start()
                    result = execHandle.waitForFinish()
                }

                if (spec.checkExitCode && result.exitValue != 0) {
                    error(client.testFailedMessage(execHandle, result.exitValue))
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

    override fun stopNow() {
        shouldStop = true
        if (::execHandle.isInitialized) {
            execHandle.abort()
        }
        outputReaderThread?.join()
    }
}