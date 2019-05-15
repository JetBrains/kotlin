package org.jetbrains.kotlin.gradle.internal.testing

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.process.ProcessForkOptions
import org.gradle.process.internal.ExecHandle
import org.gradle.process.internal.ExecHandleFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

open class TCServiceMessagesTestExecutionSpec(
    val forkOptions: ProcessForkOptions,
    val args: List<String>,
    val checkExitCode: Boolean,
    val clientSettings: TCServiceMessagesClientSettings
) : TestExecutionSpec {
    internal open fun createClient(testResultProcessor: TestResultProcessor, log: Logger): TCServiceMessagesClient =
        TCServiceMessagesClient(testResultProcessor, clientSettings, log)
}

private val log = LoggerFactory.getLogger("org.jetbrains.kotlin.gradle.tasks.testing")

class TCServiceMessagesTestExecutor(
    val execHandleFactory: ExecHandleFactory,
    val buildOperationExecutor: BuildOperationExecutor
) : TestExecuter<TCServiceMessagesTestExecutionSpec> {
    var execHandle: ExecHandle? = null
    var outputReaderThread: Thread? = null
    var shouldStop = false

    override fun execute(spec: TCServiceMessagesTestExecutionSpec, testResultProcessor: TestResultProcessor) {
        val stdInPipe = PipedInputStream()

        val rootOperation = buildOperationExecutor.currentOperation.parentId

        outputReaderThread = thread(name = "${spec.forkOptions} output reader") {
            try {
                val client = spec.createClient(testResultProcessor, log)

                client.root(rootOperation) {
                    stdInPipe.reader().useLines { lines ->
                        lines.forEach {
                            if (shouldStop) {
                                client.closeAll()
                                return@thread
                            }

                            try {
                                ServiceMessage.parse(it, client)
                            } catch (e: Exception) {
                                log.error(
                                    "Error while processing test process output message \"$it\"",
                                    e
                                )
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                log.error("Error creating TCServiceMessagesClient", t)
            }
        }

        val exec = execHandleFactory.newExec()
        spec.forkOptions.copyTo(exec)
        exec.args = spec.args
        exec.standardOutput = PipedOutputStream(stdInPipe)

        execHandle = exec.build()

        execHandle!!.start()
        val result = execHandle!!.waitForFinish()
        outputReaderThread!!.join()

        if (spec.checkExitCode && result.exitValue != 0) {
            error("$execHandle exited with errors (exit code: ${result.exitValue})")
        }
    }

    override fun stopNow() {
        shouldStop = true
        execHandle?.abort()
        outputReaderThread?.join()
    }
}