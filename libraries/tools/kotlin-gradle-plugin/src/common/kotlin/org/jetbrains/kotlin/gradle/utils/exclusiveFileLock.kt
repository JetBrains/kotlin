/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Path
import java.time.Instant
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.isRegularFile
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Create an exclusive lock that prevents concurrent execution of [block].
 *
 * Execute [block] under two locks to prevent concurrent execution.
 *
 * - Within the same JVM process: use [kotlin.synchronized].
 * - Across JVM processes: use [java.nio.channels.FileLock], using [file].
 *
 * Note that [file] is used for inter-process locking, but [block] does not necessarily have to read or write the file.
 * The file could just exist and does not have to be read or written to.
 *
 * @param[file] Used to create a [java.nio.channels.FileLock]. The file must exist.
 * @param[lockTimeout] How long to try to get the file lock before throwing an error.
 */
@OptIn(ExperimentalContracts::class)
internal fun <T> exclusiveFileLock(
    file: Path,
    lockTimeout: Duration = 5.seconds,
    block: () -> T,
): T {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

    require(file.isRegularFile()) {
        "file must be a regular file, got $file"
    }
    require(lockTimeout.isPositive()) {
        "maxWait must be positive, got $lockTimeout"
    }

    synchronized(ExclusiveFileLockSync) {
        var attempts = 0
        val startMark = Instant.now()

        do {
            try {
                RandomAccessFile(file.toFile(), "rw").use { lockFileAccess ->
                    val fileLock = lockFileAccess.channel.tryLock(
                        0,
                        1,
                        // set shared=false to trigger OverlappingFileLockException
                        // if the lockfile is used by the same thread
                        false,
                    )

                    if (fileLock != null) {
                        fileLock.use {
                            // Successfully acquired exclusive lock
                            return block()
                        }
                    } else {
                        // Failed to acquire exclusive lock, already locked by another process
                    }
                }
            } catch (e: OverlappingFileLockException) {
                // Locked by this process, treat as not acquired
            }

            attempts++
            Thread.sleep(Random.nextLong(50, 150))
        } while (startMark.elapsedNow() < lockTimeout.toJavaDuration())

        error("Failed to acquire exclusive lock after ${startMark.elapsedNow()} and $attempts attempts")
    }
}


private object ExclusiveFileLockSync {
    override fun toString(): String = "ExclusiveFileLockSync"
}


private fun Instant.elapsedNow(): java.time.Duration =
    java.time.Duration.between(this, Instant.now())
