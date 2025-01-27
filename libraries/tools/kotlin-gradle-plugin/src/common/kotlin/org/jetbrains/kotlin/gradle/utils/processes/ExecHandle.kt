/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils.processes

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Launch and manage an external process.
 *
 * Use [execute] to run the process and wait for the result, or [start] to launch asynchronously.
 *
 * The process can be killed using [abort].
 *
 * ## State flows
 *
 * - `Initial → Started → [Succeeded|Failed|Aborted]`
 * - `Initial → Failed`
 * - `Initial → Started → Aborted`
 *
 * State is controlled on all control methods:
 *
 * - [start] allowed when state is `Initial`.
 * - [abort] allowed when state is `Started`.
 */
internal class ExecHandle(
    val displayName: String,
    /** The working directory of the process. */
    val directory: File,
    /** The executable to run. */
    val command: String,
    /** Arguments to pass to the executable. */
    val arguments: List<String>,
    /** The variables to set in the environment the executable is run in. */
    val environment: Map<String, String>,
    private val outputHandler: StreamsHandler,
    private val inputHandler: StreamsHandler,
    /** Merge the process' error stream into its output stream. */
    val redirectErrorStream: Boolean,
    val ignoreExitValue: Boolean,
    private val executor: Executor,
) {
    /** Lock to guard mutable state. */
    private val lock: Lock = ReentrantLock()

    private val stateChanged: Condition = lock.newCondition()

    /** State of this [ProcessRunner]. */
    private var state: ExecHandleState = ExecHandleState.Initial
        set(value) {
            lock.withLock {
                logger.info("[ExecHandle $displayName] Changing state to: $state")
                field = value
                stateChanged.signalAll()
            }
        }

    /**
     * When not null, the runnable that is waiting.
     */
    private var execHandleRunner: ProcessRunner? = null

    private var execResult: ExecResult? = null

    /** Shut down the launched process via [shutdownHooks]. */
    private val shutdownHookAction: Runnable = Runnable {
        try {
            abort()
        } catch (t: Throwable) {
            logger.error("failed to abort '$displayName'", t)
        }
    }

    fun start(): ExecHandle {
        logger.info("Starting process '$displayName'. Working directory: $directory Command: $command ${arguments.joinToString(" ")}")
        lock.withLock {
            check(stateIn(ExecHandleState.Initial)) { "Cannot start process '$displayName' because it has already been started" }
            state = ExecHandleState.Starting

            val runner = ProcessRunner(
                execHandle = this,
                streamsHandler = CompositeStreamsHandler(),
                executor = executor,
            )
            execHandleRunner = runner
            executor.execute(runner)

            while (stateIn(ExecHandleState.Starting)) {
                logger.info("[ExecHandle $displayName] Waiting until process started")
                try {
                    stateChanged.await()
                } catch (e: InterruptedException) {
                    execHandleRunner!!.abortProcess()
                    throw e
                }
            }

            if (execResult != null) {
                execResult!!.rethrowFailure()
            }

            logger.info("[ExecHandle $displayName] Successfully started process")
        }
        return this
    }

    fun execute(): ExecResult {
        val execResult = start().waitForFinish()
        if (!ignoreExitValue) {
            execResult.assertNormalExitValue()
        }
        return execResult
    }

    fun abort() {
        lock.withLock {
            if (stateIn(ExecHandleState.Succeeded, ExecHandleState.Failed, ExecHandleState.Aborted)) {
                return
            }
            check(stateIn(ExecHandleState.Started)) {
                "Cannot abort process '$displayName' because it is not started"
            }
            execHandleRunner!!.abortProcess()
            this.waitForFinish()
        }
    }

    private fun stateIn(vararg states: ExecHandleState): Boolean =
        lock.withLock {
            state in states
        }

    private fun setEndStateInfo(
        newState: ExecHandleState,
        exitValue: Int,
        failureCause: Throwable? = null,
    ) {
        removeShutdownHook(shutdownHookAction)

        val currentState = lock.withLock { state }

        val newResult = ExecResult(exitValue, displayName, execExceptionFor(failureCause, currentState))

        lock.withLock {
            state = newState
            this.execResult = newResult
        }

        logger.info("[ExecHandle $displayName] finished with exit value $exitValue (state: $newState)")
    }

    private fun execExceptionFor(failureCause: Throwable?, currentState: ExecHandleState): ExecException? {
        if (failureCause == null) return null

        val failureMessage = when (currentState) {
            ExecHandleState.Starting -> {
                if (isFailureCausedByCommandLength(command, arguments, failureCause)) {
                    "Process '$displayName' could not be started because the command line length exceed operating system limits."
                } else {
                    "A problem occurred starting process '$displayName'."
                }
            }
            else -> "A problem occurred waiting for process '$displayName' to complete."
        }

        return ExecException(message = failureMessage, cause = failureCause)
    }

    private fun waitForFinish(): ExecResult {
        lock.withLock {
            while (!state.isTerminal) {
                try {
                    stateChanged.await()
                } catch (e: InterruptedException) {
                    execHandleRunner!!.abortProcess()
                    throw e
                }
            }
        }

        // At this point:
        // If in daemon mode, the process has started successfully and all streams to the process have been closed
        // If in fork mode, the process has completed and all cleanup has been done
        // In both cases, all asynchronous work for the process has completed and we're done
        return result()
    }

    private fun result(): ExecResult {
        lock.withLock {
            execResult!!.rethrowFailure()
            return execResult!!
        }
    }

    private fun started() {
        addShutdownHook(shutdownHookAction)
        state = ExecHandleState.Started
    }

    private fun finished(exitCode: Int) {
        if (exitCode != 0) {
            setEndStateInfo(ExecHandleState.Failed, exitCode)
        } else {
            setEndStateInfo(ExecHandleState.Succeeded, 0)
        }
    }

    private fun aborted(exitCode: Int) {
        val actualExitCode = if (exitCode == 0) {
            -1 // This can happen on Windows
        } else {
            exitCode
        }
        setEndStateInfo(ExecHandleState.Aborted, actualExitCode)
    }

    private fun failed(failureCause: Throwable?) {
        setEndStateInfo(ExecHandleState.Failed, -1, failureCause)
    }

    override fun toString(): String = displayName

    private inner class CompositeStreamsHandler : StreamsHandler {
        override fun connectStreams(process: Process, processName: String, executor: Executor) {
            inputHandler.connectStreams(process, processName, executor)
            outputHandler.connectStreams(process, processName, executor)
        }

        override fun start() {
            inputHandler.start()
            outputHandler.start()
        }

        override fun close() {
            outputHandler.close()
            inputHandler.close()
        }

        override fun disconnect() {
            outputHandler.disconnect()
            inputHandler.disconnect()
        }
    }

    private enum class ExecHandleState(val isTerminal: Boolean) {
        Initial(false),
        Starting(false),
        Started(false),

        Aborted(true),
        Failed(true),
        Succeeded(true),
        ;
    }

    /**
     * Run a process (defined by [execHandle]), using Java's [ProcessBuilder].
     */
    private class ProcessRunner(
        private val execHandle: ExecHandle,
        private val streamsHandler: StreamsHandler,
        private val executor: Executor,
    ) : Runnable {
        private val lock: Lock = ReentrantLock()

        private var process: Process? = null
        private var aborted: Boolean = false

        fun abortProcess() {
            lock.withLock {
                if (aborted) {
                    return
                }
                aborted = true
                if (process != null) {
                    streamsHandler.disconnect()
                    logger.info("[ExecHandle ${execHandle.displayName}] Abort requested. Destroying process.")
                    process!!.destroy()
                }
            }
        }

        override fun run() {
            try {
                startProcess()
                execHandle.started()

                logger.info("[ExecHandle ${execHandle.displayName}] Started. Waiting until streams are handled...")
                streamsHandler.start()

                val exitValue = process!!.waitFor()
                streamsHandler.close()
                if (aborted) {
                    execHandle.aborted(exitValue)
                } else {
                    execHandle.finished(exitValue)
                }
            } catch (t: Throwable) {
                execHandle.failed(t)
            }
        }

        private fun startProcess() {
            lock.withLock {
                check(!aborted) { "Cannot start process ${execHandle.displayName}. Process has already been aborted." }
                val processBuilder: ProcessBuilder = createProcessBuilder(execHandle)
                val process: Process = processBuilder.start()
                streamsHandler.connectStreams(process, execHandle.displayName, executor)
                this.process = process
            }
        }

        companion object {
            private val logger: Logger = Logging.getLogger(ProcessRunner::class.java)

            /**
             * Create a new [ProcessBuilder] from an [ProcessRunner].
             */
            private fun createProcessBuilder(execHandle: ExecHandle): ProcessBuilder {
                val commandWithArguments = mutableListOf<String>().apply {
                    add(execHandle.command)
                    addAll(execHandle.arguments)
                }

                val processBuilder = ProcessBuilder(commandWithArguments).apply {
                    directory(execHandle.directory)
                    redirectErrorStream(execHandle.redirectErrorStream)
                }

                val environment = processBuilder.environment()
                environment.clear()
                environment.putAll(execHandle.environment)

                return processBuilder
            }
        }
    }


    companion object {
        private val logger: Logger = Logging.getLogger(ProcessRunner::class.java)

        /** Shutdown hooks to force-stop launched processes when the JVM stops. */
        private val shutdownHooks: ConcurrentHashMap<Runnable, Thread> = ConcurrentHashMap()

        private fun addShutdownHook(hook: Runnable) {
            val thread = Thread(hook, "ExecHandle-shutdown-hook")
            shutdownHooks[hook] = thread
            Runtime.getRuntime().addShutdownHook(thread)
        }

        private fun removeShutdownHook(hook: Runnable) {
            val thread = shutdownHooks.remove(hook)
            try {
                if (thread != null) {
                    Runtime.getRuntime().removeShutdownHook(thread)
                }
            } catch (e: IllegalStateException) {
                // When shutting down is in progress, invocation of this method throws exception,
                // interrupting other shutdown hooks, so we catch it here.
                //
                // Caused by: java.lang.IllegalStateException: Shutdown in progress
                //        at java.base/java.lang.ApplicationShutdownHooks.remove(ApplicationShutdownHooks.java:82)
                //        at java.base/java.lang.Runtime.removeShutdownHook(Runtime.java:243)
                logger.error("Removing shutdown hook $thread failed", e)
            }
        }

        private fun isFailureCausedByCommandLength(
            command: String,
            arguments: List<String>,
            failureCause: Throwable,
        ): Boolean {
            // length = command length + length of all args + spaces between each arg
            val commandLineLength = command.length + arguments.sumOf { it.length } + arguments.size
            if (commandLineLength <= maxCommandLineLength) return false

            val allFailureMessages = generateSequence(failureCause) { it.cause }
                .mapNotNull { it.message }

            return allFailureMessages.any { message ->
                when {
                    // Windows
                    "The filename or extension is too long" in message -> true
                    // Nix
                    "error=7, Argument list too long" in message -> true
                    else -> false
                }
            }
        }

        /** Max OS command line length, in chars. */
        private val maxCommandLineLength: Int by lazy {
            val currentOs = System.getProperty("os.name").toLowerCase()
            when {
                // Windows: See http://msdn.microsoft.com/en-us/library/windows/desktop/ms682425(v=vs.85).aspx
                "win" in currentOs -> 32766

                // macOS: Derived from default when running getconf ARG_MAX in OSX
                "mac" in currentOs || "darwin" in currentOs -> 262144

                // Nix: Derived from MAX_ARG_STRLEN as per http://man7.org/linux/man-pages/man2/execve.2.html
                else -> 131072
            }
        }
    }
}
