/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class NativeDistributionCommonizerLock @JvmOverloads constructor(
    private val outputDirectory: File,
    private val logInfo: (message: String) -> Unit = {}
) {
    private companion object {
        val intraProcessLock: ReentrantLock = ReentrantLock()
        val lockedOutputDirectories = hashSetOf<File>()
    }

    fun <T> withLock(action: (lockFile: File) -> T): T {
        /* Enter intra-process wide lock */
        intraProcessLock.withLock {
            val lockFile = outputDirectory.resolve(".lock")
            if (outputDirectory in lockedOutputDirectories) {
                /* Already acquired this directory and re-entered: We can just execute the action */
                return action(lockFile)
            }

            /* Lock output directory inter-process wide */
            outputDirectory.mkdirs()
            logInfo("Acquire lock: ${lockFile.path} ...")
            FileOutputStream(outputDirectory.resolve(".lock")).use { stream ->
                val lock: FileLock = stream.channel.lockWithRetries(lockFile)
                assert(lock.isValid)
                return try {
                    logInfo("Lock acquired: ${lockFile.path}")
                    lockedOutputDirectories.add(outputDirectory)
                    action(lockFile)
                } finally {
                    lockedOutputDirectories.remove(outputDirectory)
                    lock.release()
                    logInfo("Lock released: ${lockFile.path}")
                }
            }
        }
    }

    private fun FileChannel.lockWithRetries(file: File): FileLock {
        var retries = 0
        while (true) {
            try {
                return lock()
            }
            /*
            Catching the OverlappingFileLockException which is caused by the same jvm (process) already having locked the file.
            Since we do use a static re-entrant lock as a monitor to the cache, this can only happen
            when this code is running in the same JVM but with in complete isolation
            (e.g. Gradle classpath isolation, or composite builds).

            If we detect this case, we retry the locking after a short period, constantly logging that we're blocked
            by some other thread using the cache.

            The risk of deadlocking here is low, since we can only get into this code path, *if*
            the code is very isolated and somebody locked the file.
             */
            catch (t: OverlappingFileLockException) {
                Thread.sleep(25)
                retries++
                if (retries % 10 == 0) {
                    logInfo("Waiting to acquire lock: $file")
                }
            }
        }
    }

    fun checkLocked(outputDirectory: File) {
        check(intraProcessLock.isHeldByCurrentThread) {
            "Expected lock to be held by current thread ${Thread.currentThread().name}"
        }

        check(outputDirectory in lockedOutputDirectories) {
            "Expected $outputDirectory to be locked. Locked directories: $lockedOutputDirectories"
        }
    }
}
