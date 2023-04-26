/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(FreezingIsDeprecated::class, ObsoleteWorkersApi::class)
package runtime.workers.worker4

import kotlin.test.*
import kotlin.native.concurrent.*
import kotlin.concurrent.AtomicInt

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
            counter.incrementAndGet()  // counter becomes 2 here.
            assertTrue(Worker.current.park(10_000_000, true))
            assertEquals(3, counter.value)
        }.freeze())

        executeAfter(0, {
            counter.incrementAndGet()
            Unit
        }.freeze())

        while (counter.value < 2) {
            Worker.current.park(1_000)
        }

        executeAfter(0, {
            counter.incrementAndGet()
            Unit
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
        counter.incrementAndGet()
        Unit
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
        counter.incrementAndGet()
        Unit
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
                counter.incrementAndGet()
                Unit
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
            counter.incrementAndGet()
        }
        // wait a bit
        Worker.current.park(10_000L)
        // submit a task
        val f2 = execute(TransferMode.SAFE, { counter }) { counter ->
            counter.incrementAndGet()
        }
        f1.consume {}
        f2.consume {}
        assertEquals(2, counter.value)
    }
}

// This test checks that when multiple `executeAfter` jobs are submitted to `targetWorker` and have the
// same scheduled execution time (in micros since an epoch), nether of them gets lost.
@Test fun testExecuteAfterScheduledTimeClash() = withWorker {
    val targetWorker = this
    val mainWorker = Worker.current

    // Configuration of the test.
    val numberOfSubmitters = 2
    val numberOfTasks = 100
    val delayInMicroseconds = 100L

    val submitters = Array(numberOfSubmitters) { Worker.start() }
    try {
        val readySubmittersCounter = AtomicInt(0)
        val executedTasksCounter = AtomicInt(0)
        val finishedBatchesCounter = AtomicInt(0)

        submitters.forEach {
            it.executeAfter(0L, {
                readySubmittersCounter.incrementAndGet()
                // Wait for other submitters, to make them all start at the same time:
                while (readySubmittersCounter.value != numberOfSubmitters) {}

                // Concurrently submit tasks with matching scheduled execution time:
                repeat(numberOfTasks) {
                    targetWorker.executeAfter(delayInMicroseconds, {
                        executedTasksCounter.incrementAndGet()
                        Unit
                    }.freeze())
                }

                // Use larger delay for the task below, to make sure it gets executed after
                // the tasks above submitted by the same worker.
                // If the order is wrong, the test will fail as well.
                // NOTE: the code below was affected by the same problem with clashing times, so despite all the effort
                // the test still might hang without a fix.
                targetWorker.executeAfter(delayInMicroseconds + 1, {
                    mainWorker.executeAfter(0L, {
                        finishedBatchesCounter.incrementAndGet()
                        Unit
                    }.freeze())
                }.freeze())
            }.freeze())
        }

        while (finishedBatchesCounter.value != numberOfSubmitters) {
            // Wait and allow processing the `finishedBatchesCounter.increment()` tasks above:
            Worker.current.park(delayInMicroseconds, process = true)
        }

        // Note: we could have just waited for the condition above to become true,
        // but this would mean that the test would hang in case of failure, which is not quite convenient.

        assertEquals(numberOfSubmitters * numberOfTasks, executedTasksCounter.value)
    } finally {
        submitters.forEach { it.requestTermination().result }
    }
}
