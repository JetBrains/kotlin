/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.native.concurrent

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.test.*

class WorkerTest {
    @Test
    fun execute() = withWorker {
        val future = execute(TransferMode.SAFE, { "Input" }) { "$it processed" }
        assertEquals("Input processed", future.result)
    }

    class MyError : Exception("My error")

    @Test
    fun executeWithException() = withWorker(errorReporting = false) {
        val future = execute(TransferMode.SAFE, {}) { throw MyError() }
        // Not `MyError`.
        assertFailsWith<IllegalStateException> { future.result }
        assertEquals("Still working", execute(TransferMode.SAFE, { "Still" }) { "$it working" }.result)
    }

    @OptIn(FreezingIsDeprecated::class)
    @Test
    fun executeWithDetachedObjectGraph() = withWorker {
        data class SharedDataMember(val double: Double)
        data class SharedData(val string: String, val int: Int, val member: SharedDataMember)
        // Here we do rather strange thing. To test object detach API we detach object graph,
        // pass detached graph to a worker, where we manually reattached passed value.
        val future = execute(TransferMode.SAFE, {
            DetachedObjectGraph { SharedData("Hello", 10, SharedDataMember(0.1)) }.asCPointer()
        }) {
            DetachedObjectGraph<SharedData>(it).attach()
        }
        assertEquals(SharedData("Hello", 10, SharedDataMember(double = 0.1)), future.result)
    }

    @Test
    fun executeOnWrongThread() {
        val worker = Worker.start()
        worker.requestTermination().result

        val exception = assertFailsWith<IllegalStateException> {
            worker.execute(TransferMode.SAFE, {}, {}).result
        }
        assertEquals("Worker is already terminated", exception.message)
    }

    @Test
    fun waitForMultipleFutures() {
        val workers = Array(5) { Worker.start() }

        val futures = (1..3).flatMap { attempt ->
            workers.mapIndexed { index, worker ->
                worker.execute(TransferMode.SAFE, { "$attempt: Input $index" }) { "$it processed" }
            }
        }

        val actual = waitForMultipleFutures(futures, 10000)

        val expected = (1..3).flatMap { attempt ->
            (0 until 5).map { index -> "$attempt: Input $index processed" }
        }.toSet()

        // actual cannot be empty.
        assertTrue(actual.isNotEmpty())
        // Everything in actual must be in expected.
        // The reverse is not required to be true: waitForMultipleFutures may return when
        // only some futures have completed.
        actual.forEach { future ->
            // Every actual future is also computed.
            assertEquals(FutureState.COMPUTED, future.state)
            assertTrue(expected.contains(future.result))
        }

        workers.forEach {
            it.requestTermination().result
        }
    }

    @Test
    fun executeWithConcurrentArrayModification() {
        val workers = Array(100) { Worker.start() }
        val array = Array(workers.size) { it }
        val futures = workers.mapIndexed { index, worker ->
            worker.execute(TransferMode.SAFE, { array to index }) { (array, index) ->
                array[index] += index
            }
        }

        while (waitForMultipleFutures(futures, 10000).size < futures.size) {
        }

        array.forEachIndexed { index, value ->
            assertEquals(index * 2, value)
        }

        workers.forEach {
            it.requestTermination().result
        }
    }

    @Test
    fun executeAfter() = withWorker {
        val counter = AtomicInt(0)

        executeAfter(0) {
            assertTrue(Worker.current.park(10_000_000, false))
            assertEquals(counter.value, 0)
            assertTrue(Worker.current.processQueue())
            assertEquals(1, counter.value)
            // Let main proceed.
            counter.incrementAndGet()  // counter becomes 2 here.
            assertTrue(Worker.current.park(10_000_000, true))
            assertEquals(3, counter.value)
        }

        executeAfter(0) {
            counter.incrementAndGet()
            Unit
        }

        while (counter.value < 2) {
            Worker.current.park(1_000)
        }

        executeAfter(0) {
            counter.incrementAndGet()
            Unit
        }

        while (counter.value == 2) {
            Worker.current.park(1_000)
        }
    }

    @Test
    fun executeAfterNegativeDelay() = withWorker {
        assertFailsWith<IllegalArgumentException> { executeAfter(-1) {} }
        Unit
    }

    @Test
    fun executeAfterModify() = withWorker {
        var v = 1
        val done = AtomicInt(0)
        executeAfter(0) {
            v++
            assertEquals(2, v)
            done.value = 1
        }
        while (done.value == 0) {
        }
        assertEquals(2, v)
    }

    @Test
    fun executeAfterOrdering() = withWorker {
        val counter = AtomicInt(0)
        val lastTask = AtomicInt(0)
        executeAfter(500_000) {
            lastTask.value = 1
            counter.incrementAndGet()
        }
        executeAfter(1_000) {
            lastTask.value = 2
            counter.incrementAndGet()
        }
        // Wait for both tasks to complete.
        while (counter.value != 2) {
        }
        // Task with id 1 was scheduled to execute later, so it has won.
        assertEquals(1, lastTask.value)
    }

    @Test
    fun executeAfterCancelled() {
        val worker = Worker.start()

        val future = worker.execute(TransferMode.SAFE, {}) {
            // Here we processed termination request.
            assertEquals(false, Worker.current.processQueue())
        }

        worker.executeAfter(1_000_000_000L) { error("FAILURE") }

        // Request worker to terminate and wait for the request to be processed.
        worker.requestTermination(processScheduledJobs = false).result
        // Now wait for the worker to complete termination, cleaning up after itself.
        waitWorkerTermination(worker)

        // `future` is bound to terminated `worker` and so it's not available anymore.
        assertFailsWith<IllegalStateException> { future.result }
    }

    @Test
    fun executeAfterOnMain() {
        var done = false
        Worker.current.executeAfter(0) {
            done = true
        }
        // Not executed immediately.
        assertFalse(done)
        // The current worker's queue may be filled with other tasks, so we must loop.
        while (!done) {
            Worker.current.processQueue()
        }
    }

    // This test checks that when multiple `executeAfter` jobs are submitted to `targetWorker` and have the
    // same scheduled execution time (in micros since an epoch), nether of them gets lost.
    @Test
    fun executeAfterScheduledTimeClash() = withWorker {
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
                it.executeAfter(0L) {
                    readySubmittersCounter.incrementAndGet()
                    // Wait for other submitters, to make them all start at the same time:
                    while (readySubmittersCounter.value != numberOfSubmitters) {
                    }

                    // Concurrently submit tasks with matching scheduled execution time:
                    repeat(numberOfTasks) {
                        targetWorker.executeAfter(delayInMicroseconds) {
                            executedTasksCounter.incrementAndGet()
                            Unit
                        }
                    }

                    // Use larger delay for the task below, to make sure it gets executed after
                    // the tasks above submitted by the same worker.
                    // If the order is wrong, the test will fail as well.
                    // NOTE: the code below was affected by the same problem with clashing times, so despite all the effort
                    // the test still might hang without a fix.
                    targetWorker.executeAfter(delayInMicroseconds + 1) {
                        mainWorker.executeAfter(0L) {
                            finishedBatchesCounter.incrementAndGet()
                            Unit
                        }
                    }
                }
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

    @Test
    fun executeAfterOnWrongThread() {
        val worker = Worker.start()
        worker.requestTermination().result

        val exception = assertFailsWith<IllegalStateException> {
            worker.executeAfter(0L) {}
        }
        assertEquals("Worker is already terminated", exception.message)
    }

    @Test
    fun withName() = withWorker(name = "Lumberjack") {
        execute(TransferMode.SAFE, {}) {
            assertEquals("Lumberjack", Worker.current.name)
        }.result
        assertEquals("Lumberjack", name)
    }

    @Test
    fun nameOnWrongThread() {
        val worker = Worker.start(name = "Lumberjack")
        worker.requestTermination().result
        val exception = assertFailsWith<IllegalStateException> {
            worker.name
        }
        assertEquals("Worker is already terminated", exception.message)
    }

    @Test
    fun park() = withWorker {
        val counter = AtomicInt(0)
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
        f1.result
        f2.result
        assertEquals(2, counter.value)
    }

    @Test
    fun parkMain() = withWorker {
        val main = Worker.current
        val counter = AtomicInt(0)
        executeAfter(1000) {
            main.executeAfter(1) {
                counter.incrementAndGet()
                Unit
            }
        }
        assertTrue(main.park(1_000_000_000L, process = true))
        assertEquals(1, counter.value)
    }

    @Test
    fun parkOnWrongThread() = withWorker {
        val exception = assertFailsWith<IllegalStateException> {
            park(1L)
        }
        assertEquals("Worker is not current or already terminated", exception.message)
    }

    @Test
    fun parkZeroTimeout() {
        Worker.current.park(0, process = true)
    }

    @Test
    fun processQueue() = withWorker {
        val counter = AtomicInt(0)
        val future1 = execute(TransferMode.SAFE, { counter }) { counter ->
            assertEquals(0, counter.value)
            // Process following request.
            while (!Worker.current.processQueue()) {
            }
            // Ensure it has an effect.
            assertEquals(1, counter.value)
            // No more non-terminating tasks in this worker queue.
            assertEquals(false, Worker.current.processQueue())
        }
        val future2 = execute(TransferMode.SAFE, { counter }) { counter ->
            counter.incrementAndGet()
        }
        future2.result
        future1.result
    }

    @Test
    fun processQueueOnWrongThread() = withWorker {
        val exception = assertFailsWith<IllegalStateException> {
            processQueue()
        }
        assertEquals("Worker is not current or already terminated", exception.message)
    }

    @Test
    fun requestTerminationOnWrongThread() {
        val worker = Worker.start()
        worker.requestTermination().result

        val exception = assertFailsWith<IllegalStateException> {
            worker.requestTermination()
        }
        assertEquals("Worker is already terminated", exception.message)
    }

    @Test
    fun activeWorkers() {
        val workers = Array(10) { Worker.start() }

        val actualWorkers = Worker.activeWorkers.toSet()

        assertTrue(actualWorkers.size - workers.size == 1 || actualWorkers.size - workers.size == 2,
                "actualWorkers.size = ${actualWorkers.size} workers.size = ${workers.size} actual size must be greater by 1 (main worker) or 2 (cleaners worker)")
        workers.forEach {
            actualWorkers.contains(it)
        }
        actualWorkers.contains(Worker.current)

        val terminatedWorkers = mutableSetOf<Worker>()
        (workers.indices step 2).forEach {
            val worker = workers[it]
            worker.requestTermination().result
            terminatedWorkers.add(worker)
        }

        val actualWorkersAfterTermination = Worker.activeWorkers.toSet()
        assertEquals(terminatedWorkers, actualWorkers - actualWorkersAfterTermination)

        (workers.indices step 2).forEach {
            workers[it + 1].requestTermination().result
        }
    }

    @Test
    fun futureConsumedTwice(): Unit = withWorker {
        val future = execute(TransferMode.SAFE, {}) {
            42
        }
        assertEquals(42, future.result)
        val exception = assertFailsWith<IllegalStateException> {
            future.result
        }
        assertEquals("Future is in an invalid state", exception.message)
    }
}