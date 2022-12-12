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

    fun <T> withLock(action: () -> T): T {
        /* Enter intra-process wide lock */
        intraProcessLock.withLock {
            if (outputDirectory in lockedOutputDirectories) {
                /* Already acquired this directory and re-entered: We can just execute the action */
                return action()
            }

            /* Lock output directory inter-process wide */
            outputDirectory.mkdirs()
            val lockfile = outputDirectory.resolve(".lock")
            logInfo("Acquire lock: ${lockfile.path} ...")
            FileOutputStream(outputDirectory.resolve(".lock")).use { stream ->
                val lock = stream.channel.lock()
                assert(lock.isValid)
                return try {
                    logInfo("Lock acquired: ${lockfile.path}")
                    lockedOutputDirectories.add(outputDirectory)
                    action()
                } finally {
                    lockedOutputDirectories.remove(outputDirectory)
                    lock.release()
                    logInfo("Lock released: ${lockfile.path}")
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
