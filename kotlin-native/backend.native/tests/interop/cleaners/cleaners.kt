/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.native.internal.*

fun ensureInitialized() {}

fun createCleaner() {
    createCleaner(42) {
        println(it)
    }
}

fun performGC() {
    GC.collect()
    performGCOnCleanerWorker()
}

private var globalCleaner: Cleaner? = null

fun initializeGlobalCleaner() {
    globalCleaner = createCleaner(11) {
        println(it)
    }
}
