package org.jetbrains.kotlin.gradle.internal.testing

import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.process.ExecResult
import org.gradle.process.ProcessForkOptions
import org.gradle.process.internal.ExecHandle
import org.gradle.process.internal.ExecHandleFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class TCServiceMessagesTestExecutionSpec(
    val forkOptions: ProcessForkOptions,
    val args: List<String>,
    val checkExitCode: Boolean,
    val clientSettings: TCServiceMessagesClientSettings
) : TestExecutionSpec {
    internal open fun createClient(testResultProcessor: TestResultProcessor, log: Logger): TCServiceMessagesClient =
        TCServiceMessagesClient(testResultProcessor, clientSettings, log)

    internal open fun wrapExecute(body: () -> Unit) = body()
    internal open fun showSuppressedOutput() = Unit
}

private val log = LoggerFactory.getLogger("org.jetbrains.kotlin.gradle.tasks.testing")

class TCServiceMessagesTestExecutor(
    val execHandleFactory: ExecHandleFactory,
    val buildOperationExecutor: BuildOperationExecutor,
    val runListeners: MutableList<KotlinTestRunnerListener>,
    val ignoreRunFailures: Boolean
) : TestExecuter<TCServiceMessagesTestExecutionSpec> {
    var execHandle: ExecHandle? = null
    var outputReaderThread: Thread? = null
    var shouldStop = false

    override fun execute(spec: TCServiceMessagesTestExecutionSpec, testResultProcessor: TestResultProcessor) {
        spec.wrapExecute {
            val rootOperation = buildOperationExecutor.currentOperation.parentId

            val client = spec.createClient(testResultProcessor, log)

            try {
                val exec = execHandleFactory.newExec()
                spec.forkOptions.copyTo(exec)
                exec.args = spec.args
                exec.standardOutput = TCServiceMessageOutputStreamHandler(client, { spec.showSuppressedOutput() }, log)
                execHandle = exec.build()

                lateinit var result: ExecResult
                client.root(rootOperation) {
                    execHandle!!.start()
                    result = execHandle!!.waitForFinish()
                }

                if (spec.checkExitCode && result.exitValue != 0) {
                    error("$execHandle exited with errors (exit code: ${result.exitValue})")
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
        execHandle?.abort()
        outputReaderThread?.join()
    }

    companion object {
        const val TC_PROJECT_PROPERTY = "teamcity"
    }
}