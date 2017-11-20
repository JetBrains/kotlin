@file:kotlin.jvm.JvmVersion
package test.utils


import kotlin.*
import kotlin.test.*
import java.io.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import test.io.serializeAndDeserialize

class LazyJVMTest {

    @Test fun synchronizedLazy() {
        val counter = AtomicInteger(0)
        val lazy = lazy {
            val value = counter.incrementAndGet()
            Thread.sleep(100)
            value
        }

        val accessThreads = listOf(lazy, lazy).map { thread { it.value } }
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
            Thread.sleep(100)
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
        var initialized: Boolean = false
        val runs = ConcurrentHashMap<Int, Boolean>()

        val initializer = {
            val value = counter.incrementAndGet()
            runs += (value to initialized)
            Thread.sleep((3 - value) * 100L)
            initialized = true
            value
        }
        val lazy = lazy(LazyThreadSafetyMode.PUBLICATION, initializer)

        val accessThreads = listOf(lazy, lazy).map { thread { it.value } }
        accessThreads.forEach { it.join() }

        assertEquals(2, counter.get())
        assertEquals(2, lazy.value)
        @Suppress("NAME_SHADOWING")
        for ((_, initialized) in runs) {
            assertFalse(initialized, "Expected uninitialized on first and second run")
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