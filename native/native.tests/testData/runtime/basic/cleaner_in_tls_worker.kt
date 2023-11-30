/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// Ideally, this test must fail with gcType=NOOP with any cache mode.
// KT-63944: unfortunately, GC flavours are silently not switched in presence of caches.
// As soon the issue would be fixed, please remove `&& cacheMode=NO` from next line.
// IGNORE_NATIVE: gcType=NOOP && cacheMode=NO

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class, ObsoleteWorkersApi::class)

import kotlin.test.*

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.native.internal.*
import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner
import kotlin.native.runtime.GC

@ThreadLocal
var tlsCleaner: Cleaner? = null

val value = AtomicInt(0)

fun box(): String {
    val worker = Worker.start()

    worker.execute(TransferMode.SAFE, {}) {
        tlsCleaner = createCleaner(42) {
            value.value = it
        }
    }

    worker.requestTermination().result
    waitWorkerTermination(worker)
    GC.collect()
    waitCleanerWorker()

    assertEquals(42, value.value)

    return "OK"
}
