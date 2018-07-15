/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.kotlin.coroutines

import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.coroutines.*
import kotlin.coroutines.experimental.startCoroutine
import kotlin.test.*

/**
 * Tests on coroutines standard library functions.
 */
class CoroutinesTest {
    /**
     * Makes sure that using [startCoroutine] with suspending references properly establishes intercepted context.
     */
    @Test
    fun testStartInterceptedSuspendReference() {
        val done = Semaphore(0)
        TestDispatcher("Result").use { resumeDispatcher ->
            TestDispatcher("Context").use { contextDispatcher ->
                val switcher = DispatcherSwitcher(contextDispatcher, resumeDispatcher)
                val ref = switcher::run // callable reference
                ref.startCoroutine(Continuation(contextDispatcher) { result ->
                    contextDispatcher.assertThread()
                    // todo: below does not work due to a bug in inline classes
//                    assertEquals(42, result.getOrThrow())
                    done.release()
                })
                done.acquire()
            }
        }
    }

}

class DispatcherSwitcher(
    private val contextDispatcher: TestDispatcher,
    private val resumeDispatcher: TestDispatcher
) {
    suspend fun run(): Int = suspendCoroutine { cont ->
        contextDispatcher.assertThread()
        resumeDispatcher.executor.execute {
            resumeDispatcher.assertThread()
            cont.resume(42)
        }
    }
}

class TestDispatcher(
    private val name: String
) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor, Closeable {
    private lateinit var thread: Thread

    val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, name).also { thread = it }
    }

    fun assertThread() {
        assertEquals(thread, Thread.currentThread())
    }

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        DispatchedContinuation(continuation)

    override fun close() {
        executor.shutdown()
    }

    inner class DispatchedContinuation<T>(val delegate: Continuation<T>) : Continuation<T> {
        override val context: CoroutineContext = delegate.context

        override fun resumeWith(result: SuccessOrFailure<T>) {
            executor.execute {
                delegate.resumeWith(result)
            }
        }
    }
}

