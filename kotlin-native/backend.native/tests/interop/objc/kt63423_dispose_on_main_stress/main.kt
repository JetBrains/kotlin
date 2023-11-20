/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import objclib.*

import kotlin.native.internal.MemoryUsageInfo
import kotlin.test.*
import kotlinx.cinterop.*

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

fun alloc(): Unit = autoreleasepool {
    OnDestroyHook()
    Unit
}

fun waitDestruction() {
    assertTrue(isMainThread())
    kotlin.native.internal.GC.collect()
    spin()
}

fun main() = startApp {
    repeat(500000) {
        alloc()
    }
    val peakRSSChecker = PeakRSSChecker(10_000_000L) // ~10MiB allowed difference for running finalizers
    waitDestruction()
    peakRSSChecker.check()
}