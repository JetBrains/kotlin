/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.cache

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class KotlinConcurrentGetOrComputeStorageTest {

    private val storage = KotlinConcurrentGetOrComputeStorage()

    @Test
    fun testValueIsComputedOnFirstAccess() {
        assertEquals("value", storage.getOrCompute("key") { "value" })
    }

    @Test
    fun testValueIsComputedOnlyOncePerKey() {
        val computations = AtomicInteger()
        val values = List(3) {
            storage.getOrCompute("key") {
                computations.incrementAndGet()
                Any()
            }
        }

        assertEquals(1, computations.get(), "Expected exactly one computation for the same key")
        values.forEach { assertSame(values.first(), it, "Expected all calls to return the very same instance") }
    }

    @Test
    fun testDifferentKeysAreComputedIndependently() {
        assertEquals("a-value", storage.getOrCompute("a") { "a-value" })
        assertEquals("b-value", storage.getOrCompute("b") { "b-value" })

        assertEquals("a-value", storage.getOrCompute<String>("a") { fail("'a' should be already cached") })
        assertEquals("b-value", storage.getOrCompute<String>("b") { fail("'b' should be already cached") })
    }

    @Test
    fun testNullValueIsCached() {
        val computations = AtomicInteger()
        repeat(2) {
            val value = storage.getOrCompute<String?>("key") {
                computations.incrementAndGet()
                null
            }
            assertNull(value)
        }

        assertEquals(1, computations.get(), "'null' is a valid value and should be cached as any other one")
    }

    @Test
    fun testValueIsComputedOnTheCallingThread() {
        val computationThread = storage.getOrCompute("key") { Thread.currentThread() }
        assertSame(Thread.currentThread(), computationThread)
    }

    @Test
    fun testFailureIsRethrownAsIsWithoutExecutionExceptionWrapper() {
        val expectedFailure = TestException()
        val actualFailure = assertFailsWith<TestException> {
            storage.getOrCompute<Unit>("key") { throw expectedFailure }
        }

        assertSame(expectedFailure, actualFailure)
    }

    @Test
    fun testFailedComputationIsRememberedAndNotRetried() {
        val computations = AtomicInteger()
        val expectedFailure = TestException()

        repeat(3) {
            val actualFailure = assertFailsWith<TestException> {
                storage.getOrCompute<Unit>("key") {
                    computations.incrementAndGet()
                    throw expectedFailure
                }
            }
            assertSame(expectedFailure, actualFailure)
        }

        assertEquals(1, computations.get(), "Failed computation should be stored and never retried")
    }

    @Test
    fun testConcurrentAccessToTheSameKeyComputesValueOnlyOnce() {
        val threadCount = 16
        val computations = AtomicInteger()
        val startLine = CountDownLatch(threadCount)

        val values = runConcurrently(threadCount) {
            startLine.countDown()
            assertTrue(startLine.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Threads failed to reach the start line")
            storage.getOrCompute("key") {
                computations.incrementAndGet()
                Any()
            }
        }

        assertEquals(1, computations.get(), "Expected exactly one computation even under concurrent access")
        values.forEach { assertSame(values.first(), it, "Expected all threads to observe the very same instance") }
    }

    @Test
    fun testConcurrentAccessToTheSameKeyPropagatesFailureToAllThreads() {
        val threadCount = 8
        val computations = AtomicInteger()
        val expectedFailure = TestException()
        val startLine = CountDownLatch(threadCount)

        val failures = runConcurrently(threadCount) {
            startLine.countDown()
            assertTrue(startLine.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Threads failed to reach the start line")
            assertFailsWith<TestException> {
                storage.getOrCompute<Unit>("key") {
                    computations.incrementAndGet()
                    throw expectedFailure
                }
            }
        }

        assertEquals(1, computations.get(), "Expected exactly one computation even under concurrent access")
        failures.forEach { assertSame(expectedFailure, it, "Every thread should observe the original failure") }
    }

    /**
     * Once the computation has started, its task is already published in the map,
     * so every other thread is guaranteed to reuse it instead of computing its own value.
     */
    @Test
    fun testWaitingThreadsGetTheValueComputedByAnotherThread() {
        val computationStarted = CountDownLatch(1)
        val releaseComputation = CountDownLatch(1)
        val computations = AtomicInteger()

        val computingThread = thread(name = "computing") {
            storage.getOrCompute<String>("key") {
                computations.incrementAndGet()
                computationStarted.countDown()
                releaseComputation.awaitOrFail("computation to be released")
                "value"
            }
        }

        try {
            computationStarted.awaitOrFail("computation to start")

            val waiters = List(4) { index ->
                val result = AtomicReference<Result<String>>()
                val waitingThread = thread(name = "waiting-$index") {
                    result.set(
                        runCatching {
                            storage.getOrCompute<String>("key") { fail("Value is already being computed by another thread") }
                        }
                    )
                }
                waitingThread to result
            }

            releaseComputation.countDown()

            waiters.forEach { (waitingThread, result) ->
                waitingThread.join(TIMEOUT_SECONDS * 1000)
                assertEquals("value", result.get().getOrThrow())
            }
            assertEquals(1, computations.get())
        } finally {
            releaseComputation.countDown()
            computingThread.join(TIMEOUT_SECONDS * 1000)
        }
    }

    @Test
    fun testSlowComputationDoesNotBlockOtherKeys() {
        val slowComputationStarted = CountDownLatch(1)
        val releaseSlowComputation = CountDownLatch(1)

        val slowThread = thread(name = "slow-computation") {
            storage.getOrCompute<String>("slow") {
                slowComputationStarted.countDown()
                releaseSlowComputation.awaitOrFail("slow computation to be released")
                "slow-value"
            }
        }

        try {
            slowComputationStarted.awaitOrFail("slow computation to start")
            // must not block on the in-flight computation of another key
            assertEquals("fast-value", storage.getOrCompute("fast") { "fast-value" })
        } finally {
            releaseSlowComputation.countDown()
            slowThread.join(TIMEOUT_SECONDS * 1000)
        }
    }

    @Test
    fun testInterruptedWaitingThreadThrowsAndKeepsInterruptedFlag() {
        val computationStarted = CountDownLatch(1)
        val releaseComputation = CountDownLatch(1)

        val computingThread = thread(name = "computing") {
            storage.getOrCompute<String>("key") {
                computationStarted.countDown()
                releaseComputation.awaitOrFail("computation to be released")
                "value"
            }
        }

        try {
            computationStarted.awaitOrFail("computation to start")

            val failure = AtomicReference<Throwable>()
            val interruptedFlagWasSet = AtomicBoolean()
            val waitingThread = thread(name = "waiting") {
                try {
                    storage.getOrCompute<String>("key") { fail("Value is already being computed by another thread") }
                } catch (e: Throwable) {
                    failure.set(e)
                    interruptedFlagWasSet.set(Thread.currentThread().isInterrupted)
                }
            }

            // no need to wait for the thread to actually park: 'FutureTask.get' checks the interrupted status upfront
            waitingThread.interrupt()
            waitingThread.join(TIMEOUT_SECONDS * 1000)

            assertTrue(
                failure.get() is InterruptedException,
                "Expected InterruptedException, but got: ${failure.get()}"
            )
            assertTrue(interruptedFlagWasSet.get(), "Interrupted status should be restored before rethrowing")
        } finally {
            releaseComputation.countDown()
            computingThread.join(TIMEOUT_SECONDS * 1000)
        }
    }

    @Test
    fun testNestedGetOrComputeWithTheSameKeyIsRejected() {
        val failure = assertFailsWith<IllegalStateException> {
            storage.getOrCompute<String>("key") {
                storage.getOrCompute<String>("key") { "value" }
            }
        }

        assertEquals(DOUBLE_ENTRY_MESSAGE, failure.message)
    }

    /**
     * Nesting is rejected for a different key as well, mostly to prevent from complex scenarios and potential deadlocks
     * on multiple threads.
     */
    @Test
    fun testNestedGetOrComputeWithADifferentKeyIsRejected() {
        val failure = assertFailsWith<IllegalStateException> {
            storage.getOrCompute<String>("outer") {
                storage.getOrCompute<String>("inner") { "inner-value" }
            }
        }

        assertEquals(DOUBLE_ENTRY_MESSAGE, failure.message)
    }

    /**
     * The guard must not leak into the following top-level calls of the same thread: the flag it relies on is reset
     * in a `finally`, otherwise a single rejected nesting would break every later call on that thread.
     */
    @Test
    fun testRejectedNestingDoesNotBreakFollowingCallsOnTheSameThread() {
        assertFailsWith<IllegalStateException> {
            storage.getOrCompute<String>("outer") {
                storage.getOrCompute<String>("inner") { "inner-value" }
            }
        }

        assertEquals("value", storage.getOrCompute("unrelated") { "value" })
    }

    private class TestException : RuntimeException("Computation failed")

    private fun <T> runConcurrently(threadCount: Int, action: () -> T): List<T> {
        val executor = Executors.newFixedThreadPool(threadCount)
        try {
            return List(threadCount) { executor.submit(Callable(action)) }
                .map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun CountDownLatch.awaitOrFail(what: String) {
        if (!await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) fail("Timed out waiting for $what")
    }

    private companion object {
        const val TIMEOUT_SECONDS = 30L
        const val DOUBLE_ENTRY_MESSAGE = "Double entry is not accepted!"
    }
}
