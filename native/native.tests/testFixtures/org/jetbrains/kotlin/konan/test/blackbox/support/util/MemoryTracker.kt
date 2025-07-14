/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

/**
 * Internal tool for tracking used memory.
 */
internal object MemoryTracker {
    data class MemoryMark(
        val timestamp: LocalDateTime,
        val usedMemory: Long,
        val freeMemory: Long,
        val totalMemory: Long,
        val maxMemory: Long
    )

    private class MemoryTrackerRunner(
        private val intervalMillis: Long,
        private val logger: (MemoryMark) -> Unit
    ) : Thread("NativeTestMemoryTrackerRunner") {
        private val runtime = Runtime.getRuntime()

        override fun run() {
            try {
                while (!interrupted()) {
                    val timestamp = LocalDateTime.now()

                    val free = runtime.freeMemory()
                    val total = runtime.totalMemory()
                    val used = total - free
                    val max = runtime.maxMemory()

                    logger(
                        MemoryMark(
                            timestamp = timestamp,
                            usedMemory = used,
                            freeMemory = free,
                            totalMemory = total,
                            maxMemory = max
                        )
                    )

                    sleep(intervalMillis)
                }
            } catch (_: InterruptedException) {
                // do nothing, just leave the loop
            }
        }
    }

    private val activeRunner = AtomicReference<MemoryTrackerRunner>()

    fun startTracking(intervalMillis: Long, logger: (MemoryMark) -> Unit) {
        val runner = MemoryTrackerRunner(intervalMillis, logger)
        check(activeRunner.compareAndSet(null, runner)) { "There is another active runner" }
        runner.start()
    }

    fun stopTracking() {
        val runner = activeRunner.getAndSet(null) ?: error("No active runner")
        runner.interrupt()
    }
}
