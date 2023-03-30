/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(FreezingIsDeprecated::class)
package runtime.workers.worker9_experimentalMM

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest1() {
    withLock { println("zzz") }
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        withLock {
            println("42")
        }
    }
    future.result
    worker.requestTermination().result
    println("OK")
}

fun withLock(op: () -> Unit) {
    op()
}

@Test fun runTest2() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        val me = Worker.current
        var x = 1
        me.executeAfter (20000) {
            println("second ${++x}")
        }
        me.executeAfter(10000) {
            println("first ${++x}")
        }
    }
    worker.requestTermination().result
}

@Test fun runTest3() {
    val worker = Worker.start()
    if (Platform.memoryModel == MemoryModel.EXPERIMENTAL) {
        worker.executeAfter {
            println("unfrozen OK")
        }
    } else {
        assertFailsWith<IllegalStateException> {
            worker.executeAfter {
                println("shall not happen")
            }
        }
    }
    assertFailsWith<IllegalArgumentException> {
        worker.executeAfter(-1, {
            println("shall not happen")
        }.freeze())
    }

    worker.executeAfter(0, {
        println("frozen OK")
    }.freeze())

    worker.requestTermination().result
}

class Node(var node: Node?, var outher: Node?)

fun makeCyclic(): Node {
    val inner = Node(null, null)
    inner.node = inner
    val outer = Node(null, null)
    inner.outher = outer
    return outer
}

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
@Test fun runTest4() {
    val worker = Worker.start()

    val future = worker.execute(TransferMode.SAFE, { }) {
        makeCyclic().also {
            kotlin.native.runtime.GC.collect()
        }
    }
    assert(future.result != null)
    worker.requestTermination().result
}
