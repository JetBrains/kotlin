/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// Note: This test reproduces a race, so it'll start flaking if problem is reintroduced.
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.test.*

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.native.internal.*
import kotlin.native.runtime.GC

val thrashGC = AtomicInt(1)
val canStartCreating = AtomicInt(0)
val createdCount = AtomicInt(0)
val canStartReading = AtomicInt(0)
const val atomicsCount = 1000
const val workersCount = 10

fun main() {
    val gcWorker = Worker.start()
    val future = gcWorker.execute(TransferMode.SAFE, {}, {
        canStartCreating.value = 1
        while (thrashGC.value != 0) {
            GC.collectCyclic()
        }
        GC.collect()
    })

    while (canStartCreating.value == 0) {}

    val workers = Array(workersCount) { Worker.start() }
    val futures = workers.map {
        it.execute(TransferMode.SAFE, {}, {
            val atomics = Array(atomicsCount) {
                AtomicReference<Any?>(Any().freeze())
            }
            createdCount.incrementAndGet()
            while (canStartReading.value == 0) {}
            GC.collect()
            atomics.all { it.value != null }
        })
    }

    while (createdCount.value != workersCount) {}

    thrashGC.value = 0
    future.result
    GC.collect()
    canStartReading.value = 1

    assertTrue(futures.all { it.result })

    for (worker in workers) {
        worker.requestTermination().result
    }
    gcWorker.requestTermination().result
}
