/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.test.*

import kotlin.native.concurrent.*
import kotlin.native.internal.*

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
    waitCleanerWorker()

    assertEquals(42, value.value)
}
