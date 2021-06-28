/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.lazy4

import kotlin.test.*

import kotlin.native.concurrent.*

const val WORKERS_COUNT = 20

class C(private val initializer: () -> Int) {
    val data by lazy { initializer() }
}

fun concurrentLazyAccess(freeze: Boolean) {
    val initializerCallCount = AtomicInt(0)

    val c = C {
        initializerCallCount.increment()
        42
    }
    if (freeze) {
        c.freeze()
    }

    val workers = Array(WORKERS_COUNT, { Worker.start() })
    val inited = AtomicInt(0)
    val canStart = AtomicInt(0)
    val futures = Array(workers.size) { i ->
        workers[i].execute(TransferMode.SAFE, { Triple(inited, canStart, c) }) { (inited, canStart, c) ->
            inited.increment()
            while (canStart.value != 1) {}
            c.data
        }
    }

    while (inited.value < workers.size) {}
    canStart.value = 1

    futures.forEach {
        assertEquals(42, it.result)
    }
    workers.forEach {
        it.requestTermination().result
    }

    assertEquals(1, initializerCallCount.value)
}

@Test
fun concurrentLazyAccessUnfrozen() {
    if (Platform.memoryModel != MemoryModel.EXPERIMENTAL) {
        return
    }
    concurrentLazyAccess(false)
}

@Test
fun concurrentLazyAccessFrozen() {
    concurrentLazyAccess(true)
}

