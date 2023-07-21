/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

package runtime.atomics.atomic_array_stress

import kotlin.test.*
import kotlin.concurrent.*
import kotlin.native.concurrent.*

fun testAtomicIntArrayStress(workers: Array<Worker>) {
    val intArr = AtomicIntArray(10)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].execute(TransferMode.SAFE, { intArr }) {  intArr ->
            for (i in 0 until 500) {
                val index = (0 until intArr.length).random()
                intArr.incrementAndGet(index)
            }
        }
    })
    futures.forEach {
        it.result
    }
    var sum = 0
    for (i in 0 until intArr.length) {
        sum += intArr[i]
    }
    assertEquals(workers.size * 500, sum)
}

fun testAtomicLongArrayStress(workers: Array<Worker>) {
    val longArr = AtomicLongArray(10)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].execute(TransferMode.SAFE, { longArr }) { longArr ->
            for (i in 0 until 500) {
                val index = (0 until longArr.length).random()
                longArr.incrementAndGet(index)
            }
        }
    })
    futures.forEach {
        it.result
    }
    var sum = 0L
    for (i in 0 until longArr.length) {
        sum += longArr[i]
    }
    assertEquals(workers.size.toLong() * 500, sum)
}

private class A(val n: Int)

fun testAtomicArrayStress(workers: Array<Worker>) {
    val refArr = AtomicArray(10) { A(0) }
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].execute(TransferMode.SAFE, { refArr }) { refArr ->
            for (i in 0 until 500) {
                val index = (0 until refArr.length).random()
                while(true) {
                    val cur = refArr[index]
                    val newValue = A(((cur as A).n + 1))
                    if (refArr.compareAndSet(index, cur, newValue)) break
                }
            }
        }
    })
    futures.forEach {
        it.result
    }
    var sum = 0
    for (i in 0 until refArr.length) {
        sum += (refArr[i] as A).n
    }
    assertEquals(workers.size * 500, sum)
}

@Test
fun runStressTest() {
    val COUNT = 20
    val workers = Array(COUNT, { _ -> Worker.start()})
    testAtomicIntArrayStress(workers)
    testAtomicLongArrayStress(workers)
    testAtomicArrayStress(workers)
}