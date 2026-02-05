/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.runProcess
import java.io.Closeable
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.logging.Level
import java.util.logging.Logger

/** Maximum number of attempts to boot a simulator. */
private const val BOOT_RETRIES = 3

/** Time to wait for a simulator to boot before considering it a failure. */
private const val BOOT_TIMEOUT_MINUTES = 3L

/** Time to wait for a simulator to shut down before considering it a failure. */
private const val SHUTDOWN_TIMEOUT_SECONDS = 30L

/**
 * Manages the lifecycle of a dedicated iOS simulator for tests.
 *
 * This class creates a unique simulator, provides a robust boot mechanism,
 * and ensures it is deleted (cleaned up) when done.
 *
 */
internal class XCTestHelpers : Closeable {

    companion object {
        internal val logger = Logger.getLogger(this::class.java.name)
    }

    @Serializable
    data class Device(val name: String, val udid: String)

    @Serializable
    data class Simulators(val devices: Map<String, List<Device>>)

    private val deviceIdentifier = "com.apple.CoreSimulator.SimDeviceType.iPhone-12-Pro-Max"
    private val uuid = UUID.randomUUID()
    private val testSimulatorName = "NativeXcodeSimulatorTestsIT_${uuid}_simulator"

    /**
     * Creates a new simulator instance with a unique name.
     *
     * @return The [Device] object representing the newly created simulator.
     */
    fun createSimulator(): Device {
        return Device(
            testSimulatorName,
            runProcessAndReturnStdoutWithoutTimeout(
                listOf("/usr/bin/xcrun", "simctl", "create", testSimulatorName, deviceIdentifier)
            ).lines().single { it.isNotEmpty() }
        )
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Lists all available simulators on the system.
     *
     * @return A [Simulators] data object parsed from the JSON output.
     */
    private fun simulators(): Simulators {
        return json.decodeFromString<Simulators>(
            runProcessAndReturnStdoutWithoutTimeout(
                listOf("/usr/bin/xcrun", "simctl", "list", "devices", "-j")
            )
        )
    }

    /**
     * Cleans up (deletes) any simulators created by this helper instance.
     *
     * This is typically called via the [Closeable.close] method.
     */
    override fun close() {
        simulators().devices.values.toList().flatten().filter {
            it.name == testSimulatorName
        }.forEach {
            logger.info("Cleaning up simulator ${it.name} (${it.udid})")
            processOutputWithTimeout(
                listOf("/usr/bin/xcrun", "simctl", "delete", it.udid),
                timeout = SHUTDOWN_TIMEOUT_SECONDS,
                unit = TimeUnit.SECONDS,
                logger = logger
            )
        }
    }
}

/**
 * Boots the simulator device with a timeout and retry mechanism.
 *
 * This will attempt to boot the simulator [BOOT_RETRIES] times,
 * waiting [BOOT_TIMEOUT_MINUTES] for each attempt.
 *
 * If an attempt times out, it will try to shut down the simulator
 * before retrying.
 *
 * @receiver The [XTestHelpers.Device] to boot.
 * @throws IllegalStateException if the simulator fails to boot after all retries.
 */
internal fun XCTestHelpers.Device.boot(logger: Logger = XCTestHelpers.logger) {
    // The 'bootstatus' command with '-b' will boot the device if not already booted,
    // and then wait for the boot to complete. This is the command that can hang.
    val bootCommand = listOf("/usr/bin/xcrun", "simctl", "bootstatus", udid, "-b")

    retry(logger) { attempt ->
        logger.info("Attempt $attempt/$BOOT_RETRIES to boot simulator $name ($udid)...")
        try {
            processOutputWithTimeout(
                arguments = bootCommand,
                timeout = BOOT_TIMEOUT_MINUTES,
                unit = TimeUnit.MINUTES,
                logger = logger
            )
            logger.info("Simulator $name ($udid) booted successfully.")
            // If successful, the retry block returns and exits the loop
        } catch (e: Throwable) {
            // Check if this was the first failed attempt
            if (attempt == 1) {
                logger.warning("First boot attempt failed. Running Apple's workaround (dyld_shared_cache update)...")
                try {
                    updateCache(logger)
                } catch (updateEx: Throwable) {
                    logger.log(Level.SEVERE, "Workaround 'dyld_shared_cache update' failed.", updateEx)
                    // Don't swallow the original exception, but log this new one.
                    // The original exception 'e' will be re-thrown by the retry block.
                }
            }

            // Log the original failure (TimeoutException or other)
            if (e is TimeoutException) {
                logger.warning("Boot attempt $attempt timed out after $BOOT_TIMEOUT_MINUTES minutes.")
            } else {
                logger.warning("Boot attempt $attempt failed with an error: ${e.message}")
            }

            // Always try to shut down the simulator
            try {
                processOutputWithTimeout(
                    listOf("/usr/bin/xcrun", "simctl", "shutdown", udid),
                    timeout = SHUTDOWN_TIMEOUT_SECONDS,
                    unit = TimeUnit.SECONDS,
                    logger = logger
                )
            } catch (shutdownError: Throwable) {
                logger.warning("Failed to shut down simulator $udid after failed boot: ${shutdownError.message}")
            }
            throw e // Re-throw the original exception to trigger the retry or fail
        }
    }
}

// https://developer.apple.com/documentation/xcode-release-notes/xcode-26_1-release-notes#Known-Issues
// KTI-2756 Timeout in 🍏ᵐ Gradle Integration Tests Native Mac arm64 (Macos)
private fun updateCache(logger: Logger = XCTestHelpers.logger) {
    logger.info("Attempting to update dyld_shared_cache for all runtimes. This may take a few minutes...")
    processOutputWithTimeout(
        arguments = listOf("/usr/bin/xcrun", "simctl", "runtime", "dyld_shared_cache", "update", "--all"),
        timeout = BOOT_TIMEOUT_MINUTES, // Reuse the same timeout, as this can be slow
        unit = TimeUnit.MINUTES,
        redirectErrorStream = true,
        logger = logger
    )
    logger.info("dyld_shared_cache update complete.")
}

/**
 * Wrapper for [runProcess] that asserts success and returns the output.
 * Use this for quick, non-hanging commands.
 *
 * @param arguments The command and its arguments.
 * @return The standard output (stdout) of the process.
 */
private fun runProcessAndReturnStdoutWithoutTimeout(arguments: List<String>): String {
    val result = runProcess(
        arguments, File("."),
        redirectErrorStream = false, // Keep false to see stderr on failure
    )
    result.assertProcessRunResult {
        assert(isSuccessful)
    }

    return result.output
}

/**
 * A robust utility to run an external process with a specified timeout.
 *
 * This function is necessary because [runProcess] blocks indefinitely.
 * This function uses `process.waitFor(timeout)` and stream "gobblers"
 * to read output on separate threads, preventing deadlocks.
 *
 * @param arguments The command and its arguments.
 * @param workDir The working directory for the process.
 * @param timeout The maximum time to wait.
 * @param unit The time unit for the timeout.
 * @param redirectErrorStream Whether to merge stderr into stdout.
 * @return The standard output of the process as a String.
 * @throws TimeoutException if the process does not complete within the timeout.
 * @throws IllegalStateException if the process exits with a non-zero code.
 */
private fun processOutputWithTimeout(
    arguments: List<String>,
    workDir: File = File("."),
    timeout: Long,
    unit: TimeUnit,
    redirectErrorStream: Boolean = false,
    logger: Logger
): String {
    val process = ProcessBuilder(arguments)
        .directory(workDir)
        .redirectErrorStream(redirectErrorStream)
        .start()

    val executor = Executors.newFixedThreadPool(2)

    // Consume stdout in a separate thread, logging each line
    val outputFuture = executor.submit<String> {
        val sb = StringBuilder()
        process.inputStream.bufferedReader().use { reader ->
            reader.forEachLine { line ->
                logger.info(line) // Real-time logging
                sb.append(line).append(System.lineSeparator())
            }
        }
        sb.toString()
    }

    // Consume stderr in a separate thread (if not redirected)
    val errorFuture = executor.submit<String> {
        val sb = StringBuilder()
        if (!redirectErrorStream) {
            process.errorStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    logger.warning(line) // Real-time logging
                    sb.append(line).append(System.lineSeparator())
                }
            }
        }
        sb.toString()
    }

    try {
        if (!process.waitFor(timeout, unit)) {
            // Process timed out
            process.destroyForcibly()
            throw TimeoutException(
                "Process timed out after $timeout $unit: ${arguments.joinToString(" ")}"
            )
        }

        // Process finished within time
        val exitCode = process.exitValue()
        val output = outputFuture.get()
        val error = errorFuture.get()

        if (exitCode != 0) {
            throw IllegalStateException(
                """
                Process finished with non-zero exit code: $exitCode
                Command: ${arguments.joinToString(" ")}
                Output:
                $output
                Error (if not redirected):
                $error
                """.trimIndent()
            )
        }
        return output
    } finally {
        // Clean up: ensure futures are cancelled and the thread pool is shut down
        outputFuture.cancel(true)
        errorFuture.cancel(true)
        executor.shutdownNow()
    }
}

/**
 * A utility function to execute a block of code with a retry mechanism.
 *
 * @param T The return type of the block.
 * @param block The code block to execute, receiving the current attempt number.
 * @throws IllegalStateException if all retry attempts fail.
 * @return The result of the block if successful.
 */
private fun <T> retry(
    logger: Logger,
    block: (attempt: Int) -> T
): T {
    var lastException: Throwable? = null
    for (attempt in 1..BOOT_RETRIES) {
        try {
            return block(attempt) // Eagerly return on success
        } catch (e: Throwable) {
            lastException = e
            logger.warning("Attempt $attempt/$BOOT_RETRIES failed: ${e.message}")
            if (attempt == BOOT_RETRIES) {
                // All retries failed
                throw IllegalStateException(
                    "Operation failed after $BOOT_RETRIES attempts. Last error: ${lastException.message}",
                    lastException
                )
            }
        }
    }
    // This line is theoretically unreachable but required by the compiler
    throw IllegalStateException("Retry logic fell through", lastException)
}