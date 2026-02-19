/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAtomicApi::class)

package test.concurrent.atomics

import kotlin.concurrent.atomics.*
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
            atomic.addAndFetch(1000)
        }
        futures.forEach {
            it.result
        }
        assertEquals(10 + 1000 * futures.size, atomic.load())
    }

    @Test fun incrementAndGet() {
        val initial = 15
        val atomic = AtomicInt(initial)
        val futures = ThreadPool.execute {
            atomic.incrementAndFetch()
        }
        futures.forEach {
            it.result
        }
        assertEquals(initial + futures.size, atomic.load())
    }

    @Test fun mutex() {
        val place = AtomicInt(1)
        val counter = AtomicInt(0)
        val futures = ThreadPool.execute { index ->
            // Here we simulate mutex using [place] location to store tag of the current worker.
            // When it is negative - worker executes exclusively.
            val tag = index + 1
            while (place.compareAndExchange(tag, -tag) != tag) {}
            assertEquals(index + 1, counter.incrementAndFetch())
            // Now, let the next worker run.
            val previousPlace = place.compareAndExchange(-tag, tag + 1)
            assertEquals(-tag, previousPlace)
        }
        futures.forEach {
            it.result
        }
        assertEquals(futures.size, counter.load())
    }

    @Test fun update() {
        class Updater(val shiftSelf: Int,
                      val shiftCompetitor: Int,
                      val updates: Int,
                      val atomic: AtomicInt) {
            var oldSelfValue = -1
            var oldCompetitorValue = -1

            fun run() {
                var run = true
                while (run) {

                    atomic.update {
                        val self = it.shr(shiftSelf).and(0x0ffff)
                        val other = it.shr(shiftCompetitor).and(0x0ffff)

                        run = self < updates

                        // Either our update failed, and the competitor succeeded, or the opposite.
                        if (self == oldSelfValue) {
                            assertTrue(oldCompetitorValue < other || other >= updates)
                            oldCompetitorValue = other
                        } else {
                            assertTrue(oldSelfValue == -1 || self == oldSelfValue + 1)
                            oldSelfValue = self
                        }
                        (self + 1).shl(shiftSelf).or(other.shl(shiftCompetitor))
                    }
                }
            }
        }

        val atomic = AtomicInt(0)
        val adversary = Updater(shiftSelf = 16, shiftCompetitor = 0, updates = 10000, atomic = atomic)
        val self = Updater(shiftSelf = 0, shiftCompetitor = 16, updates = 10000, atomic = atomic)
        val futures = ThreadPool.execute {
            when (it) {
                0 -> {
                    adversary.run()
                }
                1 -> {
                    self.run()
                }
                else -> { /* do nothing */ }
            }
        }
        futures.forEach { it.result }
    }
}

class AtomicLongStressTest {
    @BeforeTest fun init() = ThreadPool.init(20)
    @AfterTest fun deinit() = ThreadPool.deinit()

    @Test fun addAndGet() {
        val atomic = AtomicLong(10L)
        val futures = ThreadPool.execute {
            atomic.addAndFetch(9999999999)
        }
        futures.forEach {
            it.result
        }
        assertEquals(10L + 9999999999 * futures.size, atomic.load())
    }

    @OptIn(ExperimentalAtomicApi::class)
    @Test fun update() {
        class Updater(val shiftSelf: Int,
                      val shiftCompetitor: Int,
                      val updates: Int,
                      val atomic: AtomicLong) {
            var oldSelfValue = -1L
            var oldCompetitorValue = -1L

            fun run() {
                var run = true
                while (run) {

                    atomic.update {
                        val self = it.shr(shiftSelf).and(0x0ffffffffL)
                        val other = it.shr(shiftCompetitor).and(0x0ffffffffL)

                        run = self < updates.toLong()

                        // Either our update failed, and the competitor succeeded, or the opposite.
                        if (self == oldSelfValue) {
                            assertTrue(oldCompetitorValue < other)
                            oldCompetitorValue = other
                        } else {
                            assertTrue(oldSelfValue == -1L || self == oldSelfValue + 1)
                            oldSelfValue = self
                        }
                        (self + 1).shl(shiftSelf).or(other.shl(shiftCompetitor))
                    }
                }
            }
        }

        val atomic = AtomicLong(0)
        val adversary = Updater(shiftSelf = 32, shiftCompetitor = 0, updates = 100000, atomic = atomic)
        val self = Updater(shiftSelf = 0, shiftCompetitor = 32, updates = 100000, atomic = atomic)
        val futures = ThreadPool.execute {
            when (it) {
                0 -> adversary.run()
                1 -> self.run()
                else -> { /* do nothing */ }
            }
        }
        futures.forEach { it.result }
    }
}

private class LockFreeStack<T> {
    private val top = AtomicReference<Node<T>?>(null)

    private class Node<T>(val value: T, val next: Node<T>?)

    fun isEmpty(): Boolean = top.load() == null

    fun push(value: T) {
        while(true) {
            val cur = top.load()
            val upd = Node(value, cur)
            if (top.compareAndSet(cur, upd)) return
        }
    }

    fun pop(): T? {
        while(true) {
            val cur = top.load()
            if (cur == null) return null
            if (top.compareAndSet(cur, cur.next)) return cur.value
        }
    }
}

class AtomicStressTest {
    private data class Data(val value: Int)

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
                val current = common.load() ?: continue
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
                val index = (0 until intArr.size).random()
                intArr.incrementAndFetchAt(index)
            }
        }
        futures.forEach {
            it.result
        }
        var sum = 0
        for (i in 0 until intArr.size) {
            sum += intArr.loadAt(i)
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
                val index = (0 until longArr.size).random()
                longArr.incrementAndFetchAt(index)
            }
        }
        futures.forEach {
            it.result
        }
        var sum = 0L
        for (i in 0 until longArr.size) {
            sum += longArr.loadAt(i)
        }
        assertEquals(futures.size.toLong() * 500, sum)
    }
}

class AtomicArrayStressTest {
    private data class Data(val value: Int)

    @BeforeTest fun init() = ThreadPool.init(20)
    @AfterTest fun deinit() = ThreadPool.deinit()

    @Test fun compareAndSet() {
        val refArr = AtomicArray(10) { Data(0) }
        val futures = ThreadPool.execute {
            for (i in 0 until 500) {
                val index = (0 until refArr.size).random()
                while(true) {
                    val cur = refArr.loadAt(index)
                    val newValue = Data(cur.value + 1)
                    if (refArr.compareAndSetAt(index, cur, newValue)) break
                }
            }
        }
        futures.forEach {
            it.result
        }
        var sum = 0
        for (i in 0 until refArr.size) {
            sum += refArr.loadAt(i).value
        }
        assertEquals(futures.size * 500, sum)
    }
}
