/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class, kotlin.native.concurrent.ObsoleteWorkersApi::class)

import kotlin.concurrent.AtomicInt
import kotlin.concurrent.Volatile
import kotlin.native.concurrent.*
import kotlin.native.identityHashCode
import kotlin.native.internal.MemoryUsageInfo
import kotlin.native.ref.createCleaner
import kotlin.random.Random

// Copying what's done in kotlinx.benchmark
// TODO: Could we benefit, if this was in stdlib, and the compiler just new about it?
object Blackhole {
    @Volatile
    var i0: Int = Random.nextInt()
    var i1 = i0 + 1

    fun consume(value: Any?) {
        consume(value.identityHashCode())
    }

    fun consume(i: Int) {
        if ((i0 == i) && (i1 == i)) {
            i0 = i
        }
    }
}

class ArrayOfBytes(bytes: Int) {
    val data = ByteArray(bytes)
    init {
        // Write into every OS page.
        for (i in 0 until data.size step 4096) {
            data[i] = 42
        }
        Blackhole.consume(data)
    }
}

class ArrayOfBytesWithFinalizer(bytes: Int) {
    val impl = ArrayOfBytes(bytes)
    val cleaner = createCleaner(impl) {
        Blackhole.consume(it)
    }
}

fun allocateGarbage() {
    // Total amount of objects here:
    // - 1 big object with finalizer
    // - 9 big objects
    // - 2490 small objects with finalizers
    // - 97500 small objects without finalizers
    // And total size is ~50MiB
    for (i in 0..100_000) {
        val obj: Any = when {
            i == 50_000 -> ArrayOfBytesWithFinalizer(1_000_000) // ~1MiB
            i % 10_000 == 0 -> ArrayOfBytes(1_000_000) // ~1MiB
            i % 40 == 0 -> ArrayOfBytesWithFinalizer(((i / 100) % 10) * 80) // ~1-100 pointers
            else -> ArrayOfBytes(((i / 100) % 10) * 80) // ~1-100 pointers
        }
        Blackhole.consume(obj)
    }
}

class PeakRSSChecker(private val rssDiffLimitBytes: Long) {
    // On Linux, the child process might immediately commit the same amount of memory as the parent.
    // So, measure difference between peak RSS measurements.
    private val initialBytes = MemoryUsageInfo.peakResidentSetSizeBytes.also {
        check(it != 0L) { "Error trying to obtain peak RSS. Check if current platform is supported" }
    }

    fun check(): Long {
        val diffBytes = MemoryUsageInfo.peakResidentSetSizeBytes - initialBytes
        check(diffBytes <= rssDiffLimitBytes) { "Increased peak RSS by $diffBytes bytes which is more than $rssDiffLimitBytes" }
        return diffBytes
    }
}

fun main() {
    // allocateGarbage allocates ~50MiB. Make total amount per mutator ~5GiB.
    val count = 100
    // Total amount overall is ~20GiB
    val threadCount = 4
    val progressReportsCount = 10
    // Setting the initial boundary to ~50MiB. The scheduler will adapt this value
    // dynamically with no upper limit.
    kotlin.native.runtime.GC.targetHeapBytes = 50_000_000
    kotlin.native.runtime.GC.minHeapBytes = 50_000_000
    // Limit memory usage at ~200MiB. 4 times the initial boundary yet still
    // way less than total expected allocated amount.
    val peakRSSChecker = PeakRSSChecker(200_000_000L)

    val workers = Array(threadCount) { Worker.start() }
    val globalCount = AtomicInt(0)
    val finalGlobalCount = count * workers.size
    workers.forEach {
        it.executeAfter(0L) {
            for (i in 0 until count) {
                allocateGarbage()
                peakRSSChecker.check()
                globalCount.getAndAdd(1)
            }
        }
    }

    val reportStep = finalGlobalCount / progressReportsCount
    var lastReportCount = -reportStep
    while (true) {
        val diffPeakRss = peakRSSChecker.check()
        val currentCount = globalCount.value
        if (currentCount >= finalGlobalCount) {
            break
        }
        if (lastReportCount + reportStep <= currentCount) {
            println("Allocating iteration $currentCount of $finalGlobalCount with peak RSS increase: $diffPeakRss bytes")
            lastReportCount = currentCount
        }
    }

    workers.forEach {
        it.requestTermination().result
    }
    peakRSSChecker.check()
}
