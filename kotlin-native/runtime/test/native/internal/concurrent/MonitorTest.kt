/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.native.internal.concurrent

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.native.internal.concurrent.Monitor
import kotlin.native.internal.concurrent.startThread
import kotlin.native.internal.concurrent.synchronized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

@OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class)
class MonitorTest {

    @Test
    fun mutualExclusion() {
        val numThreads = 10

        val monitor = Monitor.allocate()

        val ready = AtomicBoolean(false)
        var visitorCounter = 0
        val finishedCounter = AtomicInt(0)

        repeat(numThreads) {
            startThread {
                while (!ready.load()) {}
                synchronized(monitor) {
                    ++visitorCounter
                    assertEquals(1, visitorCounter)
                    --visitorCounter
                }
                finishedCounter.incrementAndGet()
            }
        }

        ready.store(true)

        while (finishedCounter.value < numThreads) {}

        assertEquals(0, visitorCounter)
    }

    @Test
    fun waitNotify() {
        val monitor = Monitor.allocate()

        val stage = AtomicInt(0)
        var release = false

        startThread {
            synchronized(monitor) {
                stage.value = 1
                while (!release) wait()
                stage.value = 2
            }
        }

        while (stage.value < 1) {}

        synchronized(monitor) {
            release = true
            notify()
        }

        while (stage.value < 2) {}
    }

    @Test
    fun waitTimeout() {
        val monitor = Monitor.allocate()

        synchronized(monitor) {
            wait(100.milliseconds)
        }
    }

    @Test
    fun waitUntil() {
        val monitor = Monitor.allocate()

        synchronized(monitor) {
            waitUntil(TimeSource.Monotonic.markNow() + 100.milliseconds)
        }
    }

    @Test
    fun notifyAll() {
        val numThreads = 10

        val monitor = Monitor.allocate()

        val ready = AtomicBoolean(false)
        val finishedCounter = AtomicInt(0)
        var release = false

        repeat(numThreads) {
            startThread {
                while (!ready.load()) {}
                synchronized(monitor) {
                    while (!release) wait()
                }
                finishedCounter.incrementAndGet()
            }
        }

        ready.store(true)

        synchronized(monitor) {
            release = true
            notifyAll()
        }

        while (finishedCounter.value < numThreads) {}
    }
}