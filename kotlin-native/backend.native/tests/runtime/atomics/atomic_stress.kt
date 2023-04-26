/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(FreezingIsDeprecated::class)
package runtime.atomics.atomic_stress

import kotlin.test.*
import kotlin.native.concurrent.*
import kotlin.concurrent.*
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicLong
import kotlin.concurrent.AtomicReference
import kotlin.native.internal.NativePtr

fun testAtomicIntStress(workers: Array<Worker>) {
    val atomic = AtomicInt(10)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].execute(TransferMode.SAFE, { atomic }) {
            atomic -> atomic.addAndGet(1000)
        }
    })
    futures.forEach {
        it.result
    }
    assertEquals(10 + 1000 * workers.size, atomic.value)
}

fun testAtomicLongStress(workers: Array<Worker>) {
    val atomic = AtomicLong(10L)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].execute(TransferMode.SAFE, { atomic }) {
            atomic -> atomic.addAndGet(9999999999)
        }
    })
    futures.forEach {
        it.result
    }
    assertEquals(10L + 9999999999 * workers.size, atomic.value)
}

private class LockFreeStack<T> {
    private val top = AtomicReference<Node<T>?>(null)

    private class Node<T>(val value: T, val next: Node<T>?)

    fun isEmpty(): Boolean = top.value == null

    fun push(value: T) {
        while(true) {
            val cur = top.value
            val upd = Node(value, cur)
            if (top.compareAndSet(cur, upd)) return
        }
    }

    fun pop(): T? {
        while(true) {
            val cur = top.value
            if (cur == null) return null
            if (top.compareAndSet(cur, cur.next)) return cur.value
        }
    }
}

fun testAtomicReferenceStress(workers: Array<Worker>) {
    val stack = LockFreeStack<Int>()
    val writers = Array(workers.size, { workerIndex ->
        workers[workerIndex].execute(TransferMode.SAFE, { stack to workerIndex}) {
            (stack, workerIndex) -> stack.push(workerIndex)
        }
    })
    writers.forEach { it.result }

    val seen = mutableSetOf<Int>()
    while(!stack.isEmpty()) {
        val value = stack.pop()
        assertNotNull(value)
        seen.add(value)
    }
    assertEquals(workers.size, seen.size)
}

@Test
fun runStressTest() {
    val COUNT = 20
    val workers = Array(COUNT, { _ -> Worker.start()})
    testAtomicIntStress(workers)
    testAtomicLongStress(workers)
    testAtomicReferenceStress(workers)
}
