/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(ObsoleteWorkersApi::class)
package runtime.workers.worker0

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { "Input" }) {
        input ->
        assertEquals(1, 1)
        assertFailsWith<AssertionError> { assertEquals(1, 2) }
        input + " processed"
    }
    future.consume {
        result -> println("Got $result")
    }
    worker.requestTermination().result
    println("OK")
}
