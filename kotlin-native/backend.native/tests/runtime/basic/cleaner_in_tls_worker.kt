/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
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

fun main() {
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
}
