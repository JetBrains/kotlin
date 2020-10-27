/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.atomic0

import kotlin.test.*

import kotlin.native.concurrent.*

fun test1(workers: Array<Worker>) {
    val atomic = AtomicInt(15)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].execute(TransferMode.SAFE, { atomic }) {
            input -> input.addAndGet(1)
        }
    })
    futures.forEach {
        it.result
    }
    println(atomic.value)
}

fun test2(workers: Array<Worker>) {
    val atomic = AtomicInt(1)
    val counter = AtomicInt(0)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].execute(TransferMode.SAFE, { Triple(atomic, workerIndex, counter) }) {
            (place, index, result) ->
            // Here we simulate mutex using [place] location to store tag of the current worker.
            // When it is negative - worker executes exclusively.
            val tag = index + 1
            while (place.compareAndSwap(tag, -tag) != tag) {}
            val ok1 = result.addAndGet(1) == index + 1
            // Now, let the next worker run.
            val ok2 = place.compareAndSwap(-tag, tag + 1) == -tag
            ok1 && ok2
        }
    })
    futures.forEach {
        assertEquals(it.result, true)
    }
    println(counter.value)
}

data class Data(val value: Int)

fun test3(workers: Array<Worker>) {
    val common = AtomicReference<Data?>(null)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].execute(TransferMode.SAFE, { Pair(common, workerIndex) }) {
            (place, index) ->
            val mine = Data(index).freeze()
            // Try to publish our own data, until successful, in a tight loop.
            while (!place.compareAndSet(null, mine)) {}
        }
    })
    val seen = mutableSetOf<Data>()
    for (i in 0 until workers.size) {
        do {
            val current = common.value
            if (current != null && !seen.contains(current)) {
                seen += current
                // Let others publish.
                assertEquals(common.compareAndSwap(current, null), current)
                break
            }
        } while (true)
    }
    futures.forEach {
        it.result
    }
    assertEquals(seen.size, workers.size)
}

fun test4() {
    assertFailsWith<InvalidMutabilityException> {
        AtomicReference(Data(1))
    }
    assertFailsWith<InvalidMutabilityException> {
        AtomicReference<Data?>(null).compareAndSwap(null, Data(2))
    }
}

fun test5() {
    assertFailsWith<InvalidMutabilityException> {
        AtomicReference<Data?>(null).value = Data(2)
    }
    val ref = AtomicReference<Data?>(null)
    val value = Data(3).freeze()
    assertEquals(null, ref.value)
    ref.value = value
    assertEquals(3, ref.value!!.value)
}

fun test6() {
    val int = AtomicInt(0)
    int.value = 239
    assertEquals(239, int.value)
    val long = AtomicLong(0)
    long.value = 239L
    assertEquals(239L, long.value)
}


fun test7() {
    val ref = FreezableAtomicReference(Array(1) { "hey" })
    ref.value[0] = "ho"
    assertEquals(ref.value[0], "ho")
    ref.value = Array(1) { "po" }
    assertEquals(ref.value[0], "po")
    ref.freeze()
    assertFailsWith<InvalidMutabilityException> {
        ref.value = Array(1) { "no" }
    }
    assertFailsWith<InvalidMutabilityException> {
        ref.value[0] = "go"
    }
    ref.value = Array(1) { "so" }.freeze()
    assertEquals(ref.value[0], "so")
}

@Test fun runTest() {
    val COUNT = 20
    val workers = Array(COUNT, { _ -> Worker.start()})

    test1(workers)
    test2(workers)
    test3(workers)
    test4()
    test5()
    test6()
    test7()

    workers.forEach {
        it.requestTermination().result
    }
    println("OK")
}

