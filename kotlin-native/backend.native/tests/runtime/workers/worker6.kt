/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(FreezingIsDeprecated::class, ObsoleteWorkersApi::class)
package runtime.workers.worker6

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest1() {
    withWorker {
        val future = execute(TransferMode.SAFE, { 42 }) { input ->
            input.toString()
        }
        future.consume { result ->
            println("Got $result")
        }
    }
    println("OK")
}

var int1 = 1
val int2 = 77

@Test fun runTest2() {
    int1++
    withWorker {
        executeAfter(0, {
            if (kotlin.native.Platform.memoryModel == kotlin.native.MemoryModel.EXPERIMENTAL) {
                int1++
                assertEquals(3, int1)
            } else {
                assertFailsWith<IncorrectDereferenceException> {
                    int1++
                }
                assertEquals(2, int1)
            }
            assertEquals(77, int2)
        }.freeze())
    }
}
