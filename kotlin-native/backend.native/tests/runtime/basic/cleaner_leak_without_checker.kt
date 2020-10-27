/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.test.*

import kotlin.native.internal.*
import kotlin.native.Platform

// This cleaner won't be run, because it's deinitialized with globals after
// cleaners are disabled.
val globalCleaner = createCleaner(42) {
    println(it)
}

fun main() {
    // Cleaner holds onto a finalization lambda. If it doesn't get executed,
    // the memory will leak. Suppress memory leak checker to check for cleaners
    // leak only.
    Platform.isMemoryLeakCheckerActive = false
    Platform.isCleanersLeakCheckerActive = false
    // Make sure cleaner is initialized.
    assertNotNull(globalCleaner)
}
