/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class NativeDistributionCommonizerLock(
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
                val lock = stream.channel.lock()
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

    fun checkLocked(outputDirectory: File) {
        check(intraProcessLock.isHeldByCurrentThread) {
            "Expected lock to be held by current thread ${Thread.currentThread().name}"
        }

        check(outputDirectory in lockedOutputDirectories) {
            "Expected $outputDirectory to be locked. Locked directories: $lockedOutputDirectories"
        }
    }
}
