/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.concurrent

import kotlin.concurrent.*
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.*

object ThreadPool {
    private var _workers: Array<Worker>? = null

    public fun init(threadCount: Int) {
        _workers = Array(threadCount) { Worker.start() }
    }

    public fun deinit() {
        _workers?.forEach {
            it.requestTermination().result
        }
        _workers = null
    }

    public fun <T> execute(f: (Int) -> T): List<Future<T>> = _workers?.mapIndexed { index, worker ->
        worker.execute(TransferMode.SAFE, { Pair(f, index) }) { (f, index) ->
            f(index)
        }
    } ?: error("Call ThreadPool.init() first")
}

class AtomicIntStressTest {
    @BeforeTest fun init() = ThreadPool.init(20)
    @AfterTest fun deinit() = ThreadPool.deinit()

    @Test fun addAndGet() {
        val atomic = AtomicInt(10)
        val futures = ThreadPool.execute {
            atomic.addAndGet(1000)
        }
        futures.forEach {
            it.result
        }
        assertEquals(10 + 1000 * futures.size, atomic.value)
    }

    @Test fun incrementAndGet() {
        val initial = 15
        val atomic = AtomicInt(initial)
        val futures = ThreadPool.execute {
            atomic.incrementAndGet()
        }
        futures.forEach {
            it.result
        }
        assertEquals(initial + futures.size, atomic.value)
    }

    @Test fun mutex() {
        val place = AtomicInt(1)
        val counter = AtomicInt(0)
        val futures = ThreadPool.execute { index ->
            // Here we simulate mutex using [place] location to store tag of the current worker.
            // When it is negative - worker executes exclusively.
            val tag = index + 1
            while (place.compareAndExchange(tag, -tag) != tag) {}
            assertEquals(index + 1, counter.incrementAndGet())
            // Now, let the next worker run.
            val previousPlace = place.compareAndExchange(-tag, tag + 1)
            assertEquals(-tag, previousPlace)
        }
        futures.forEach {
            it.result
        }
        assertEquals(futures.size, counter.value)
    }
}

class AtomicLongStressTest {
    @BeforeTest fun init() = ThreadPool.init(20)
    @AfterTest fun deinit() = ThreadPool.deinit()

    @Test fun addAndGet() {
        val atomic = AtomicLong(10L)
        val futures = ThreadPool.execute {
            atomic.addAndGet(9999999999)
        }
        futures.forEach {
            it.result
        }
        assertEquals(10L + 9999999999 * futures.size, atomic.value)
    }
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

class AtomicStressTest {
    @BeforeTest fun init() = ThreadPool.init(20)
    @AfterTest fun deinit() = ThreadPool.deinit()

    @Test fun mutex() {
        val common = AtomicReference<Data?>(null)
        val futures = ThreadPool.execute { index ->
            // Try to publish our own data, until successful, in a tight loop.
            while (!common.compareAndSet(null, Data(index))) {}
        }
        val seen = mutableListOf<Int>()
        futures.forEach {
            while(true) {
                val current = common.value ?: continue
                // Each worker publishes exactly once
                assertFalse(seen.contains(current.value))
                seen.add(current.value)
                // Let others publish.
                common.compareAndExchange(current, null)
                break
            }
        }
        futures.forEach {
            it.result
        }
        assertContentEquals(futures.indices, seen.sorted())
    }

    @Test fun stackConcurrentPush() {
        val stack = LockFreeStack<Int>()
        val writers = ThreadPool.execute {
            stack.push(it)
        }
        writers.forEach { it.result }

        val seen = mutableSetOf<Int>()
        while(!stack.isEmpty()) {
            val value = stack.pop()
            assertNotNull(value)
            seen.add(value)
        }
        assertEquals(writers.size, seen.size)
    }
}

class AtomicIntArrayStressTest {
    @BeforeTest fun init() = ThreadPool.init(20)
    @AfterTest fun deinit() = ThreadPool.deinit()

    @Test fun incrementAndGet() {
        val intArr = AtomicIntArray(10)
        val futures = ThreadPool.execute {
            for (i in 0 until 500) {
                val index = (0 until intArr.length).random()
                intArr.incrementAndGet(index)
            }
        }
        futures.forEach {
            it.result
        }
        var sum = 0
        for (i in 0 until intArr.length) {
            sum += intArr[i]
        }
        assertEquals(futures.size * 500, sum)
    }
}

class AtomicLongArrayStressTest {
    @BeforeTest fun init() = ThreadPool.init(20)
    @AfterTest fun deinit() = ThreadPool.deinit()

    @Test fun incrementAndGet() {
        val longArr = AtomicLongArray(10)
        val futures = ThreadPool.execute {
            for (i in 0 until 500) {
                val index = (0 until longArr.length).random()
                longArr.incrementAndGet(index)
            }
        }
        futures.forEach {
            it.result
        }
        var sum = 0L
        for (i in 0 until longArr.length) {
            sum += longArr[i]
        }
        assertEquals(futures.size.toLong() * 500, sum)
    }
}

class AtomicArrayStressTest {
    @BeforeTest fun init() = ThreadPool.init(20)
    @AfterTest fun deinit() = ThreadPool.deinit()

    @Test fun compareAndSet() {
        val refArr = AtomicArray(10) { Data(0) }
        val futures = ThreadPool.execute {
            for (i in 0 until 500) {
                val index = (0 until refArr.length).random()
                while(true) {
                    val cur = refArr[index]
                    val newValue = Data(cur.value + 1)
                    if (refArr.compareAndSet(index, cur, newValue)) break
                }
            }
        }
        futures.forEach {
            it.result
        }
        var sum = 0
        for (i in 0 until refArr.length) {
            sum += refArr[i].value
        }
        assertEquals(futures.size * 500, sum)
    }
}
