/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.test.*

import kotlin.native.concurrent.*
import kotlin.native.internal.*
import kotlin.native.Platform

@ThreadLocal
var tlsCleaner: Cleaner? = null

fun main() {
    // Cleaner holds onto a finalization lambda. If it doesn't get executed,
    // the memory will leak. Suppress memory leak checker to check for cleaners
    // leak only.
    Platform.isMemoryLeakCheckerActive = false
    Platform.isCleanersLeakCheckerActive = true
    // This cleaner won't be run
    tlsCleaner = createCleaner(42) {
        println(it)
    }
}
