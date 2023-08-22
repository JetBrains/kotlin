/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, FreezingIsDeprecated::class, ObsoleteWorkersApi::class)
package runtime.workers.lazy4

import kotlin.test.*
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.*
import kotlin.native.concurrent.*

const val WORKERS_COUNT = 20

class IntHolder(val value:Int)

class C(mode: LazyThreadSafetyMode, private val initializer: () -> IntHolder) {
    val data by lazy(mode) { initializer() }
}

fun concurrentLazyAccess(freeze: Boolean, mode: LazyThreadSafetyMode) {
    // in old mm PUBLICATION is in fact SYNCHRONIZED, while SYNCHRONIZED is not supported
    val argumentMode = if (Platform.memoryModel == MemoryModel.EXPERIMENTAL) mode else LazyThreadSafetyMode.PUBLICATION
    val initializerCallCount = AtomicInt(0)

    val c = C(argumentMode) {
        initializerCallCount.incrementAndGet()
        IntHolder(42)
    }
    if (freeze) {
        c.freeze()
    }

    val workers = Array(WORKERS_COUNT, { Worker.start() })
    val inited = AtomicInt(0)
    val canStart = AtomicInt(0)
    val futures = Array(workers.size) { i ->
        workers[i].execute(TransferMode.SAFE, { Triple(inited, canStart, c) }) { (inited, canStart, c) ->
            inited.incrementAndGet()
            while (canStart.value != 1) {}
            c.data
        }
    }

    while (inited.value < workers.size) {}
    canStart.value = 1

    val results = futures.map { it.result }
    results.forEach {
        assertEquals(42, it.value)
        assertSame(results[0], it)
    }
    workers.forEach {
        it.requestTermination().result
    }

    if (mode == LazyThreadSafetyMode.SYNCHRONIZED) {
        assertEquals(1, initializerCallCount.value)
    }
}

@Test
fun concurrentLazyAccessUnfrozen() {
    if (Platform.memoryModel != MemoryModel.EXPERIMENTAL) {
        return
    }
    concurrentLazyAccess(false, LazyThreadSafetyMode.SYNCHRONIZED)
    concurrentLazyAccess(false, LazyThreadSafetyMode.PUBLICATION)
}

@Test
fun concurrentLazyAccessFrozen() {
    concurrentLazyAccess(true, LazyThreadSafetyMode.SYNCHRONIZED)
    concurrentLazyAccess(true, LazyThreadSafetyMode.PUBLICATION)
}

