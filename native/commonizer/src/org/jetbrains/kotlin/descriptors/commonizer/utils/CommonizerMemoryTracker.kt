/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import java.io.Writer
import java.util.concurrent.atomic.AtomicReference

/**
 * Internal tool for tracking the memory used by the commonizer.
 */
@Suppress("unused")
internal object CommonizerMemoryTracker {
    private class CommonizerMemoryTrackerRunner(
        phaseName: String,
        private val intervalMillis: Long,
        private val forceGC: Boolean,
        private val writerFactory: () -> Writer
    ) : Thread("CommonizerMemoryTrackerRunner") {
        private val runtime = Runtime.getRuntime()
        private val phaseName = AtomicReference(phaseName)

        fun updatePhase(phaseName: String) {
            this.phaseName.set(phaseName)
        }

        override fun run() {
            writerFactory().use { writer ->
                writer.writeHeader()

                val startTime = System.currentTimeMillis()

                try {
                    while (!interrupted()) {
                        if (forceGC) {
                            runtime.gc()
                        }

                        val currentTime = System.currentTimeMillis() - startTime

                        val free = runtime.freeMemory()
                        val total = runtime.totalMemory()
                        val used = total - free
                        val max = runtime.maxMemory()

                        writer.writeRow(currentTime, phaseName.get(), used, free, total, max)

                        sleep(intervalMillis)
                    }
                } catch (_: InterruptedException) {
                    // do nothing, just leave the loop
                }
            }
        }
    }

    private val activeRunner = AtomicReference<CommonizerMemoryTrackerRunner>()

    fun startTracking(phaseName: String, intervalMillis: Long, forceGC: Boolean) {
        val runner = CommonizerMemoryTrackerRunner(phaseName, intervalMillis, forceGC) {
            System.out.writer() // TODO: add ability to supply custom writer here
        }

        check(activeRunner.compareAndSet(null, runner)) { "There is another active runner" }

        runner.start()
    }

    fun updatePhase(phaseName: String) {
        activeRunner.get()?.updatePhase(phaseName) ?: error("No active runner")
    }

    fun stopTracking() {
        val runner = activeRunner.getAndSet(null) ?: error("No active runner")
        runner.interrupt()
    }

    private fun Writer.writeHeader() {
        write("time;phase;used;free;total;max\n")
    }

    private fun Writer.writeRow(time: Long, phaseName: String, used: Long, free: Long, total: Long, max: Long) {
        fun Long.toMBs() = (this / 1024 / 1024).toString()

        write(time.toString())
        write(";")
        write(phaseName)
        write(";")
        write(used.toMBs())
        write(";")
        write(free.toMBs())
        write(";")
        write(total.toMBs())
        write(";")
        write(max.toMBs())
        write("\n")
    }
}
