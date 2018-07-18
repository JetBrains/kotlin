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
                    assertEquals(42, result.getOrThrow())
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
