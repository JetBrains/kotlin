/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.concurrent

import java.util.*
import kotlin.concurrent.*
import kotlin.test.*

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


class TimerTest {
    @Test fun scheduledTask() {
        val timer = Timer()

        val latch = CountDownLatch(10)
        val startedAt = System.nanoTime()
        lateinit var callbackTask: TimerTask
        val task = timer.scheduleAtFixedRate(100, 10) {
            callbackTask = this
            latch.countDown()
            if (latch.count == 0L) this.cancel()
        }
        if (!latch.await(2500, TimeUnit.MILLISECONDS)) throw TimeoutException()
        val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
        val expectedAtLeast = 180L
        assertTrue(elapsed > expectedAtLeast, "Expected elapsed ($elapsed ms) to be at least $expectedAtLeast ms")
        assertSame(task, callbackTask)
    }
}