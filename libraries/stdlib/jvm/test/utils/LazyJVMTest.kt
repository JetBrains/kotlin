/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.*
import kotlin.concurrent.thread
import test.io.serializeAndDeserialize
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class LazyJVMTest {

    @Test fun synchronizedLazy() {
        val counter = AtomicInteger(0)
        val lazy = lazy {
            val value = counter.incrementAndGet()
            Thread.sleep(16)
            value
        }

        val threads = 3
        val barrier = CyclicBarrier(threads)
        val accessThreads = List(threads) { thread { barrier.await(); lazy.value } }
        accessThreads.forEach { it.join() }

        assertEquals(1, counter.get())
    }

    @Test fun synchronizedLazyRace() {
        racyTest(initialize = {
                    val counter = AtomicInteger(0)
                    lazy { counter.incrementAndGet() }
                 },
                 access = { lazy, _ -> lazy.value },
                 validate = { result -> result.all { it == 1 } }
        )
    }

    @Test fun externallySynchronizedLazy() {
        val counter = AtomicInteger(0)
        var initialized: Boolean = false
        val runs = ConcurrentHashMap<Int, Boolean>()
        val lock = Any()

        val initializer = {
            val value = counter.incrementAndGet()
            runs += (value to initialized)
            Thread.sleep(16)
            initialized = true
            value
        }
        val lazy1 = lazy(lock, initializer)
        val lazy2 = lazy(lock, initializer)

        val accessThreads = listOf(lazy1, lazy2).map { thread { it.value } }
        accessThreads.forEach { it.join() }

        assertEquals(2, counter.get())
        @Suppress("NAME_SHADOWING")
        for ((counter, initialized) in runs) {
            assertEquals(initialized, counter == 2, "Expected uninitialized on first, initialized on second call: initialized=$initialized, counter=$counter")
        }
    }

    @Test fun externallySynchronizedLazyRace() {
        val threads = 3
        racyTest(threads,
                 initialize = {
                     val counter = AtomicInteger(0)
                     var initialized = false
                     val initializer = {
                         (counter.incrementAndGet() to initialized).also {
                             initialized = true
                         }
                     }
                     val lock = Any()

                     List(threads) { lazy(lock, initializer) }
                 },
                 access = { lazies, runnerIndex -> lazies[runnerIndex].value },
                 validate = { result -> result.all { (id, initialized) -> initialized == (id != 1) } })
    }

    @Test fun publishOnceLazy() {
        val counter = AtomicInteger(0)
        val initialized = AtomicBoolean(false)
        val threads = 3
        val values = Random().let { r -> List(threads) { 50 + r.nextInt(50) } }

        data class Run(val value: Int, val initialized: Boolean)

        val runs = ConcurrentLinkedQueue<Run>()

        val initializer = {
            val id = counter.getAndIncrement()
            val value = values[id]
            runs += Run(value, initialized.get())
            Thread.sleep(value.toLong())
            initialized.set(true)
            value
        }
        val lazy = lazy(LazyThreadSafetyMode.PUBLICATION, initializer)

        val barrier = CyclicBarrier(threads)
        val accessThreads = List(threads) { thread { barrier.await(); lazy.value } }
        val result = run { while (!lazy.isInitialized()) /* wait */; lazy.value }
        accessThreads.forEach { it.join() }

        assertEquals(threads, counter.get())
        assertEquals(result, lazy.value, "Value must not change after isInitialized is set: $lazy, runs: $runs")

        runs.forEach {
            assertFalse(it.initialized, "Expected uninitialized on all initializer executions, runs: $runs")
        }
    }

    @Test fun publishOnceLazyRace() {
        racyTest(initialize = { lazy(LazyThreadSafetyMode.PUBLICATION) { Thread.currentThread().id } },
                 access = { lazy, _ -> lazy.value },
                 validate = { result -> result.all { v -> v == result[0] } })
    }

    @Test fun lazyInitializationForcedOnSerialization() {
        for (mode in listOf(LazyThreadSafetyMode.SYNCHRONIZED, LazyThreadSafetyMode.PUBLICATION, LazyThreadSafetyMode.NONE)) {
            val lazy = lazy(mode) { "initialized" }
            assertFalse(lazy.isInitialized())
            val lazy2 = serializeAndDeserialize(lazy)
            assertTrue(lazy.isInitialized())
            assertTrue(lazy2.isInitialized())
            assertEquals(lazy.value, lazy2.value)
        }
    }

    private fun <TState : Any, TResult> racyTest(
        threads: Int = 3, runs: Int = 5000,
        initialize: () -> TState,
        access: (TState, runnerIndex: Int) -> TResult,
        validate: (List<TResult>) -> Boolean
    ) {

        val runResult = java.util.Collections.synchronizedList(mutableListOf<TResult>())
        val invalidResults = mutableListOf<Pair<Int, List<TResult>>>()
        lateinit var state: TState

        var runId = -1
        val barrier = CyclicBarrier(threads) {
            if (runId >= 0) {
                if (!validate(runResult))
                    invalidResults.add(runId to runResult.toList())
                runResult.clear()
            }
            state = initialize()
            runId += 1
        }

        val runners = List(threads) { index ->
            thread {
                barrier.await()
                repeat(runs) {
                    runResult += access(state, index)
                    barrier.await()
                }
            }
        }

        runners.forEach { it.join() }

        assertTrue(invalidResults.isEmpty(), invalidResults.joinToString("\n") { (index, result) -> "At run #$index: $result" })
    }
}