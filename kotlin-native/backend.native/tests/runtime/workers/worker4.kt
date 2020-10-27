/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.worker4

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest1() {
    withWorker {
        val future = execute(TransferMode.SAFE, { 41 }) { input ->
            input + 1
        }
        future.consume { result ->
            println("Got $result")
        }
    }
    println("OK")
}

@Test fun runTest2() {
    withWorker {
        val counter = AtomicInt(0)

        executeAfter(0, {
            assertTrue(Worker.current.park(10_000_000, false))
            assertEquals(counter.value, 0)
            assertTrue(Worker.current.processQueue())
            assertEquals(1, counter.value)
            // Let main proceed.
            counter.increment()  // counter becomes 2 here.
            assertTrue(Worker.current.park(10_000_000, true))
            assertEquals(3, counter.value)
        }.freeze())

        executeAfter(0, {
            counter.increment()
        }.freeze())

        while (counter.value < 2) {
            Worker.current.park(1_000)
        }

        executeAfter(0, {
            counter.increment()
        }.freeze())

        while (counter.value == 2) {
            Worker.current.park(1_000)
        }
    }
}

@Test fun runTest3() {
    val worker = Worker.start(name = "Lumberjack")
    val counter = AtomicInt(0)
    worker.executeAfter(0, {
        assertEquals("Lumberjack", Worker.current.name)
        counter.increment()
    }.freeze())

    while (counter.value == 0) {
        Worker.current.park(1_000)
    }
    assertEquals("Lumberjack", worker.name)
    worker.requestTermination().result
    assertFailsWith<IllegalStateException> {
        println(worker.name)
    }
}

@Test fun runTest4() {
    val counter = AtomicInt(0)
    Worker.current.executeAfter(10_000, {
        counter.increment()
    }.freeze())
    assertTrue(Worker.current.park(1_000_000, process = true))
    assertEquals(1, counter.value)
}

@Test fun runTest5() {
    val main = Worker.current
    val counter = AtomicInt(0)
    withWorker {
        executeAfter(1000, {
            main.executeAfter(1, {
                counter.increment()
            }.freeze())
        }.freeze())
        assertTrue(main.park(1000L * 1000 * 1000, process = true))
        assertEquals(1, counter.value)
    }
}

@Test fun runTest6() {
    // Ensure zero timeout works properly.
    Worker.current.park(0, process = true)
}

@Test fun runTest7() {
    val counter = AtomicInt(0)
    withWorker {
        val f1 = execute(TransferMode.SAFE, { counter }) { counter ->
            Worker.current.park(Long.MAX_VALUE / 1000L, process = true)
            counter.increment()
        }
        // wait a bit
        Worker.current.park(10_000L)
        // submit a task
        val f2 = execute(TransferMode.SAFE, { counter }) { counter ->
            counter.increment()
        }
        f1.consume {}
        f2.consume {}
        assertEquals(2, counter.value)
    }
}
