/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import oshi.SystemInfo
import java.io.IOException
import java.lang.ProcessHandle
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Waits for the termination of the Kotlin daemon processes by monitoring a specified directory for run files.
 *
 * This function checks periodically at intervals specified by [periodicCheckTime] to see if the directory
 * [runFilesDirectory] is empty, indicating that the Kotlin daemon has terminated.
 * If the directory remains non-empty and the [maxWaitTime] is exceeded, it sends a SIGTERM to the alive processes.
 * If that didn't help, it sends SIGKILL to the still alive processes and cleans [runFilesDirectory] itself.
 *
 * @param runFilesDirectory The path to the directory that contains the run files for the Kotlin daemon. Must be a valid directory.
 * @param maxWaitTime The maximum duration to wait for the graceful daemon termination.
 * @param periodicCheckTime The interval at which the directory will be checked for run files. This is also converted to milliseconds during execution.
 * @param forceKillWaitTime The duration to wait after attempting to kill a process gracefully before forcibly terminating it.
 */
fun awaitKotlinDaemonTermination(
    runFilesDirectory: Path,
    maxWaitTime: Duration = 10.seconds,
    periodicCheckTime: Duration = 100.milliseconds,
    forceKillWaitTime: Duration = 1.seconds,
) {
    require(Files.isDirectory(runFilesDirectory)) { "${runFilesDirectory.toAbsolutePath().normalize()} is not a directory." }
    require(maxWaitTime >= periodicCheckTime) { "$periodicCheckTime must be >= $maxWaitTime" }

    val endOfWaitTime = TimeSource.Monotonic.markNow() + maxWaitTime
    var lastRunFiles: List<Path> = emptyList()

    while (endOfWaitTime.hasNotPassedNow()) {
        lastRunFiles = runFilesDirectory.listDirectoryEntries()
        if (lastRunFiles.isEmpty()) {
            return
        }
        Thread.sleep(periodicCheckTime.inWholeMilliseconds)
    }

    val daemonProcesses = SystemInfo().operatingSystem.processes.filter { runFilesDirectory.toString() in it.commandLine }

    for (daemonProcess in daemonProcesses) {
        val processHandle = ProcessHandle.of(daemonProcess.processID.toLong()).getOrNull()
        // `null` indicates the process has already terminated
        if (processHandle != null) {
            System.err.println("The Kotlin daemon process ${daemonProcess.processID} was not shut down gracefully. Trying to kill it.")
            processHandle.destroy()
        }
    }

    while ((endOfWaitTime + forceKillWaitTime).hasNotPassedNow()) {
        lastRunFiles = runFilesDirectory.listDirectoryEntries()
        if (lastRunFiles.isEmpty()) {
            return
        }
        Thread.sleep(periodicCheckTime.inWholeMilliseconds)
    }

    // we do not expect new daemon processes to be started, so no reason to query running processes again
    for (daemonProcess in daemonProcesses) {
        val processHandle = ProcessHandle.of(daemonProcess.processID.toLong()).getOrNull()
        // `null` indicates the process has already terminated
        if (processHandle != null) {
            System.err.println("The Kotlin daemon process ${daemonProcess.processID} was not shut down gracefully. Killing it forcibly.")
            processHandle.destroyForcibly()
        }
    }

    // forcibly killing may prevent run files from being deleted in the shutdown hook
    val failedDeletionExceptions = lastRunFiles
        .map { path -> path to runCatching { path.deleteIfExists() } }
        .filter { (_, result) -> result.isFailure }
        .map { (path, result) -> IOException("Failed to delete $path", result.exceptionOrNull()) }

    if (failedDeletionExceptions.isNotEmpty()) {
        throw Exception("Some run files failed to be deleted.").apply {
            failedDeletionExceptions.forEach { addSuppressed(it) }
        }
    }
}