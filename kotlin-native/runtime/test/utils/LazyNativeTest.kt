/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.test.*

abstract class AbstractExplicitModeLazyTest {
    abstract val mode: LazyThreadSafetyMode

    @Test
    fun finiteRecursion() {
        var y = 20

        class C {
            val finiteRecursion: Int by lazy(mode) {
                if (y < 17) 42 else {
                    y -= 1
                    finiteRecursion + 1
                }
            }
        }
        assertEquals(if (mode == LazyThreadSafetyMode.SYNCHRONIZED) 46 else 42, C().finiteRecursion)
    }

    @Test
    fun captureThis() {
        class C {
            val self by lazy(mode) { this }
        }

        val self = C()
        assertEquals(self, self.self)
    }

    @Test
    fun throwException() {
        class C {
            val thrower by lazy<String>(mode) {
                error("failure")
            }
        }

        val self = C()
        repeat(10) {
            assertFailsWith<IllegalStateException> {
                self.thrower
            }
        }
    }

    @Test
    fun multiThreadedInit() {
        val initializerCallCount = AtomicInt(0)

        class C {
            val data by lazy(mode) {
                initializerCallCount.getAndIncrement()
                Any()
            }
        }

        val self = C()

        val workers = Array(20) { Worker.start() }
        val initialized = AtomicInt(0)
        val canStart = AtomicInt(0)
        val futures = workers.map {
            it.execute(TransferMode.SAFE, { Triple(initialized, canStart, self) }) { (initialized, canStart, self) ->
                initialized.incrementAndGet()
                while (canStart.value != 1) {
                }
                self.data
            }
        }

        while (initialized.value < workers.size) {
        }
        canStart.value = 1

        val results = mutableSetOf<Any>()
        futures.forEach {
            results += it.result
        }

        if (mode == LazyThreadSafetyMode.SYNCHRONIZED) {
            assertEquals(1, initializerCallCount.value)
        } else {
            assertTrue(initializerCallCount.value > 0)
            assertTrue(initializerCallCount.value <= workers.size)
        }
        assertSame(self.data, results.single())

        workers.forEach {
            it.requestTermination().result
        }
    }
}

class SynchronizedExplicitModeLazyTest : AbstractExplicitModeLazyTest() {
    override val mode: LazyThreadSafetyMode
        get() = LazyThreadSafetyMode.SYNCHRONIZED
}

class PublicationExplicitModeLazyTest : AbstractExplicitModeLazyTest() {
    override val mode: LazyThreadSafetyMode
        get() = LazyThreadSafetyMode.PUBLICATION
}