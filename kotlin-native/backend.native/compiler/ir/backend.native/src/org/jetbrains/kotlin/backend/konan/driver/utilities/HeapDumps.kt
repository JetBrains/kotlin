/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.utilities

import com.sun.management.HotSpotDiagnosticMXBean
import java.lang.Thread.sleep
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread


private const val MXBEAN_NAME = "com.sun.management:type=HotSpotDiagnostic"

fun dumpHeap(filePath: String, live: Boolean = true) {
    try {
        val mx: HotSpotDiagnosticMXBean =
                ManagementFactory.newPlatformMXBeanProxy<HotSpotDiagnosticMXBean?>(
                        ManagementFactory.getPlatformMBeanServer(),
                        MXBEAN_NAME,
                        HotSpotDiagnosticMXBean::class.java)!!
        mx.dumpHeap(filePath, live) // live=true keeps only reachable objects
        println("Heap dump written to " + filePath)
    } catch (e: Exception) {
        throw RuntimeException("Heap dump failed", e)
    }
}

fun liveHeapSize(): Long {
    System.gc()
    return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
}

fun Long.toMegabytes(): String = "${this / 1024 / 1024} MB"

class HeapMonitor {

    private val isRunning = AtomicBoolean(true)
    private val maxHeapSize = AtomicLong(0)

    private val t = thread(start = true, isDaemon = true, name = "Devirtualization Heap Monitor") {
        println("Monitoring thread started")
        while (isRunning.get()) {
            maxHeapSize.set(maxOf(maxHeapSize.get(), liveHeapSize()))
            sleep(50)
        }
    }

    fun stop() {
        isRunning.set(false)
        t.join()

        println("Max heap size: ${maxHeapSize.get().toMegabytes()}")
    }
}