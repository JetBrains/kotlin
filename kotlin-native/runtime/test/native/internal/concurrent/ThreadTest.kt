/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.native.internal.concurrent

import kotlin.concurrent.AtomicLong
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.native.internal.concurrent.currentThreadId
import kotlin.native.internal.concurrent.startThread
import kotlin.test.Test
import kotlin.test.assertNotEquals

@OptIn(ExperimentalAtomicApi::class)
class ThreadTest {

    @Test
    fun concurrentExecution() {
        val otherTid = AtomicLong(0L)
        val threadReady = AtomicBoolean(false)

        startThread {
            otherTid.value = currentThreadId().toLong()
            threadReady.store(true)
        }

        while (!threadReady.load()) {
        }

        assertNotEquals(currentThreadId().toLong(), otherTid.value)
    }
}