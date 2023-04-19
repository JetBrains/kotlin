/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(FreezingIsDeprecated::class, ObsoleteWorkersApi::class)

import kotlin.test.*

import kotlin.native.concurrent.*

fun runTest0() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { "zzz" }) {
        input -> input.length
    }
    future.consume {
        result -> println("Got $result")
    }
    worker.requestTermination().result
    println("OK")
}

var done = false

fun runTest1() {
    val worker = Worker.current
    done = false
    // Here we request execution of the operation on the current worker.
    worker.executeAfter(0, {
        done = true
    }.freeze())
    while (!done)
        worker.processQueue()
}

// Ensure that termination of current worker on main thread doesn't lead to problems.
fun runTest2() {
    val worker = Worker.current
    val future = worker.requestTermination(false)
    worker.processQueue()
    assertEquals(future.state, FutureState.COMPUTED)
    future.consume {}
    // After termination request this worker is no longer addressable.
    assertFailsWith<IllegalStateException> { worker.executeAfter(0, {
        println("BUG!")
    }.freeze()) }
}

fun main() {
    runTest0()
    runTest1()
    runTest2()
}
