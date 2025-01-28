/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils.processes

import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions.Companion.processLaunchOptions
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Internal builder for a [ExecHandle].
 *
 * Can be configured to control how a process is managed during its execution
 * (I/O, daemon, exit values).
 * (These options are configured separately to [ProcessLaunchOptions] because typically
 * these options are critical to launching and managing a process, and so should not
 * be modified by users.)
 */
internal class ExecHandleBuilder(
    val launchOpts: ProcessLaunchOptions,
) {
    var displayName: String = ""

    /** Sets whether a non-zero exit value is ignored, else throw an exception. */
    var ignoreExitValue: Boolean = false

    /** Merge the [errorOutput] into [standardOutput]. */
    var redirectErrorStream: Boolean = false

    /**
     * Sets the standard input stream for the process executing the command.
     * The stream is closed after the process completes.
     */
    var standardInput: InputStream? = null

    /**
     * Sets the output stream to consume standard output from the process executing the command.
     * The stream is closed after the process completes.
     */
    var standardOutput: OutputStream = System.out

    /**
     * Sets the output stream to consume standard error from the process executing the command.
     * The stream is closed after the process completes.
     */
    var errorOutput: OutputStream = System.err

    /**
     * Java [Executor] used to launch the process and handle IO streams.
     *
     * The executor must be multithreaded, otherwise simultaneously handling IO streams and the process
     * will cause a deadlock.
     */
    var executor: Executor = defaultExecHandleExecutor

    /** Arguments for the command to be executed. */
    val arguments: MutableList<String> = mutableListOf()

    fun setArguments(args: Iterable<String>) {
        arguments.clear()
        arguments.addAll(args)
    }

    /** Configure [launchOpts]. */
    fun launchOpts(block: ProcessLaunchOptions.() -> Unit) {
        launchOpts.apply(block)
    }

    fun build(): ExecHandle {
        val executable = launchOpts.executable.orNull
        check(!executable.isNullOrBlank()) { "executable must not be empty" }

        val streamsHandler = StreamsHandler.OutputStreamsForwarder(
            standardOutput = standardOutput,
            errorOutput = errorOutput,
            readErrorStream = !redirectErrorStream
        )

        val inputHandler = standardInput?.let { StreamsHandler.ForwardStdin(it) } ?: StreamsHandler.EmptyStdIn()

        val workingDir = launchOpts.workingDir.orNull?.asFile ?: error("workingDir is required")
        val environment = launchOpts.environment.orNull.orEmpty()

        return ExecHandle(
            displayName = displayName.ifBlank { "command '$executable'" },
            directory = workingDir,
            command = executable,
            arguments = arguments,
            environment = environment,
            outputHandler = streamsHandler,
            inputHandler = inputHandler,
            redirectErrorStream = redirectErrorStream,
            executor = executor,
            ignoreExitValue = ignoreExitValue
        )
    }

    companion object {
        /**
         * A default shared thread pool for all exec handles.
         *
         * Note: We re-use a pool to try to minimise resource use.
         * Trying to manually shut down an executor is technically challenging
         * (the pool is used for IO and the launched process, so there's no clear moment when everything stops.)
         * The docs say "a pool that remains idle for long enough will not consume any resources", so a shared pool
         * should have minimal impact.
         */
        private val defaultExecHandleExecutor: ExecutorService by lazy { Executors.newCachedThreadPool() }

        /**
         * Create a new instance of [ExecHandleBuilder].
         */
        internal fun ObjectFactory.execHandleBuilder(
            block: ExecHandleBuilder.() -> Unit = {},
        ): ExecHandleBuilder {
            return ExecHandleBuilder(
                processLaunchOptions(),
            )
                .apply(block)
        }
    }
}
