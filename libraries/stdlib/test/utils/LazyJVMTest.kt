@file:kotlin.jvm.JvmVersion
package test.utils


import kotlin.*
import kotlin.test.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import test.io.serializeAndDeserialize
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class LazyJVMTest {

    @Test fun synchronizedLazy() {
        val counter = AtomicInteger(0)
        val lazy = lazy {
            val value = counter.incrementAndGet()
            Thread.sleep(80)
            value
        }

        val accessThreads = List(3) { thread(start = false) { lazy.value } }
        accessThreads.forEach { it.start() }
        accessThreads.forEach { it.join() }

        assertEquals(1, counter.get())
    }

    @Test fun externallySynchronizedLazy() {
        val counter = AtomicInteger(0)
        var initialized: Boolean = false
        val runs = ConcurrentHashMap<Int, Boolean>()
        val lock = Any()

        val initializer = {
            val value = counter.incrementAndGet()
            runs += (value to initialized)
            Thread.sleep(50)
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

    @Test fun publishOnceLazy() {
        val counter = AtomicInteger(0)
        val initialized = AtomicBoolean(false)
        val threads = 3
        val values = Random().let { r -> List(threads) { 50 + r.nextInt(50) } }
        data class Run(val id: Int, val value: Int, val initialized: Boolean)
        val runs = ConcurrentLinkedQueue<Run>()

        val initializer = {
            val id = counter.getAndIncrement()
            val value = values[id]
            runs += Run(id, value, initialized.get())
            Thread.sleep(value.toLong())
            initialized.set(true)
            value
        }
        val lazy = lazy(LazyThreadSafetyMode.PUBLICATION, initializer)

        val accessThreads = List(threads) { thread(start = false) { lazy.value } }
        accessThreads.forEach { it.start() }
        val result = run { while (!lazy.isInitialized()) /* wait */; lazy.value }
        accessThreads.forEach { it.join() }

        assertEquals(threads, counter.get())
        assertEquals(result, lazy.value, "Value must not change after isInitialized is set: $lazy, runs: $runs")

        runs.forEach {
            assertFalse(it.initialized, "Expected uninitialized on all initializer executions, runs: $runs")
        }
    }

    @Test fun lazyInitializationForcedOnSerialization() {
        for(mode in listOf(LazyThreadSafetyMode.SYNCHRONIZED, LazyThreadSafetyMode.PUBLICATION, LazyThreadSafetyMode.NONE)) {
            val lazy = lazy(mode) { "initialized" }
            assertFalse(lazy.isInitialized())
            val lazy2 = serializeAndDeserialize(lazy)
            assertTrue(lazy.isInitialized())
            assertTrue(lazy2.isInitialized())
            assertEquals(lazy.value, lazy2.value)
        }
    }
}