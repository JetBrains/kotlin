/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing

import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.utils.processes.ExecHandle
import org.jetbrains.kotlin.gradle.utils.processes.ExecResult
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import org.jetbrains.kotlin.gradle.utils.processes.ExecHandleBuilder.Companion.execHandleBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.OutputStream

open class TCServiceMessagesTestExecutionSpec(
    internal val processLaunchOpts: ProcessLaunchOptions,
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
    val runListeners: MutableList<KotlinTestRunnerListener>,
    val ignoreTcsmOverflow: Boolean,
    val ignoreRunFailures: Boolean,
    private val objects: ObjectFactory,
) : TestExecuter<TCServiceMessagesTestExecutionSpec> {

    private lateinit var execHandle: ExecHandle

    override fun execute(
        spec: TCServiceMessagesTestExecutionSpec,
        testResultProcessor: TestResultProcessor,
    ) {
        spec.wrapExecute {
            val client = spec.createClient(testResultProcessor, log)

            if (spec.dryRunArgs != null) {
                execHandle = objects.execHandleBuilder {
                    setArguments(spec.dryRunArgs)

                    launchOpts {
                        workingDir.set(spec.processLaunchOpts.workingDir)
                        executable.set(spec.processLaunchOpts.executable)
                        environment.putAll(spec.processLaunchOpts.environment)
                    }

                    // get rid of redundant output during dry-run
                    standardOutput = nullOutputStream

                    ignoreExitValue = true
                }.build()

                val result = execHandle.execute()
                if (result.exitValue != 0) {
                    error(client.testFailedMessage(execHandle, result.exitValue))
                }
            }

            try {
                execHandle = objects.execHandleBuilder {
                    setArguments(spec.processArgs)

                    launchOpts {
                        workingDir.set(spec.processLaunchOpts.workingDir)
                        executable.set(spec.processLaunchOpts.executable)
                        environment.putAll(spec.processLaunchOpts.environment)
                    }

                    standardOutput = TCServiceMessageOutputStreamHandler(
                        client,
                        { spec.showSuppressedOutput() },
                        log,
                        ignoreTcsmOverflow
                    )
                    errorOutput = TCServiceMessageOutputStreamHandler(
                        client,
                        { spec.showSuppressedOutput() },
                        log,
                        ignoreTcsmOverflow
                    )
                    ignoreExitValue = true
                }.build()

                lateinit var result: ExecResult
                client.root {
                    result = execHandle.execute()
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
