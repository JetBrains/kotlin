/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// OUTPUT_DATA_FILE: cleaner_in_main_with_checker.out

// Ideally, this test must fail with gcType=NOOP with any cache mode.
// KT-63944: unfortunately, GC flavours are silently not switched in presence of caches.
// As soon the issue would be fixed, please remove `&& cacheMode=NO` from next line.
// IGNORE_NATIVE: gcType=NOOP && cacheMode=NO

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner
import kotlin.native.Platform

fun box(): String {
    // Cleaner holds onto a finalization lambda. If it doesn't get executed,
    // the memory will leak. Suppress memory leak checker to check for cleaners
    // leak only.
    Platform.isMemoryLeakCheckerActive = false
    Platform.isCleanersLeakCheckerActive = true
    // This cleaner will run, because with the checker active this cleaner
    // will get collected, block scheduled and executed before cleaners are disabled.
    createCleaner(42) {
        println(it)
    }

    return "OK"
}
