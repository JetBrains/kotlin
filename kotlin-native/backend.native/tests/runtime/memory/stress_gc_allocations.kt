/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

import kotlin.native.concurrent.*
import kotlin.native.internal.MemoryUsageInfo

object Blackhole {
    // On MIPS `AtomicLong` does not support `addAndGet`. TODO: Fix it.
    private val hole = AtomicInt(0)

    fun consume(value: Any) {
        hole.addAndGet(value.hashCode().toInt())
    }

    fun discharge() {
        println(hole.value)
    }
}

// Keep a class to ensure we allocate in heap.
// TODO: Protect it from escape analysis.
class MemoryHog(val size: Int, val value: Byte, val stride: Int) {
    val data = ByteArray(size)

    init {
        for (i in 0 until size step stride) {
            data[i] = value
        }
        Blackhole.consume(data)
    }
}

val peakRssBytes: Long
    get() {
        val value = MemoryUsageInfo.peakResidentSetSizeBytes
        if (value == 0L) {
            fail("Error trying to obtain peak RSS. Check if current platform is supported")
        }
        return value
    }

@Test
fun test() {
    // One item is ~10MiB.
    val size = 10_000_000
    // Total amount is ~1TiB.
    val count = 100_000
    val value: Byte = 42
    // Try to make sure each page is written
    val stride = 4096
    // Limit memory usage at ~700MiB. This limit was exercised by -Xallocator=mimalloc and legacy MM.
    val rssDiffLimit: Long = 700_000_000
    // Trigger GC after ~100MiB are allocated
    val retainLimit: Long = 100_000_000
    val progressReportsCount = 100

    if (Platform.memoryModel == MemoryModel.EXPERIMENTAL) {
        kotlin.native.internal.GC.autotune = false
        kotlin.native.internal.GC.targetHeapBytes = retainLimit
    }

    // On Linux, the child process might immediately commit the same amount of memory as the parent.
    // So, measure difference between peak RSS measurements.
    val initialPeakRss = peakRssBytes

    for (i in 0..count) {
        if (i % (count / progressReportsCount) == 0) {
            println("Allocating iteration ${i + 1} of $count")
        }
        MemoryHog(size, value, stride)
        val diffPeakRss = peakRssBytes - initialPeakRss
        if (diffPeakRss > rssDiffLimit) {
            // If GC does not exist, this should eventually fail.
            fail("Increased peak RSS by $diffPeakRss which is more than $rssDiffLimit")
        }
    }

    // Make sure `Blackhole` does not get optimized out.
    Blackhole.discharge()
}
