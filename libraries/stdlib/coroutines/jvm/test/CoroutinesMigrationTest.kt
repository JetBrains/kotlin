/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test.kotlin.coroutines.experimental.migration

import test.kotlin.coroutines.TestDispatcher
import java.util.concurrent.Semaphore
import kotlin.coroutines.experimental.buildSequence as experimentalBuildSequence
import kotlin.coroutines.experimental.SequenceBuilder as ExperimentalSequenceBuilder
import kotlin.coroutines.experimental.Continuation as ExperimentalContinuation
import kotlin.coroutines.experimental.CoroutineContext as ExperimentalCoroutineContext
import kotlin.coroutines.experimental.AbstractCoroutineContextElement as ExperimentalAbstractCoroutineContextElement
import kotlin.coroutines.experimental.EmptyCoroutineContext as ExperimentalEmptyCoroutineContext
import kotlin.coroutines.experimental.ContinuationInterceptor as ExperimentalContinuationInterceptor
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED as EXPERIMENTAL_COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.migration.*
import kotlin.coroutines.*
import kotlin.coroutines.experimental.startCoroutine
import kotlin.coroutines.jvm.internal.runSuspend
import kotlin.test.*

/**
 * Tests on coroutines migrations utilities.
 */
class CoroutinesMigrationTest {
    @Test
    fun testContextMigration() {
        assertTrue(EmptyCoroutineContext === ExperimentalEmptyCoroutineContext.toCoroutineContext())
        assertTrue(ExperimentalEmptyCoroutineContext === EmptyCoroutineContext.toExperimentalCoroutineContext())
        MyElement().let { e ->
            assertTrue(e === e.toExperimentalCoroutineContext().toCoroutineContext())
            val ee = MyExperimentalElement()
            val ctx = (e.toExperimentalCoroutineContext() + ee).toCoroutineContext()
            assertTrue(e === ctx[MyElement.Key])
            assertTrue(ee === ctx.toExperimentalCoroutineContext()[MyExperimentalElement.Key])
        }
        MyExperimentalElement().let { ee ->
            assertTrue(ee === ee.toCoroutineContext().toExperimentalCoroutineContext())
            val e = MyElement()
            val ctx = (ee.toCoroutineContext() + e).toExperimentalCoroutineContext()
            assertTrue(ee === ctx[MyExperimentalElement.Key])
            assertTrue(e === ctx.toCoroutineContext()[MyElement.Key])
        }
    }

    @Test
    fun testFunctionMigration() {
        var fooCnt = 0
        suspend fun foo() {
            fooCnt++
        }
        runSuspend {
            foo()
        }
        assertEquals(1, fooCnt)
        val foo2 = ::foo.toExperimentalSuspendFunction().toSuspendFunction()
        runSuspend {
            foo2()
        }
        assertEquals(2, fooCnt)
        var barCnt = 0
        suspend fun bar(x: Int) {
            barCnt += x
        }
        runSuspend {
            bar(2)
        }
        assertEquals(2, barCnt)
        val bar2 = ::bar.toExperimentalSuspendFunction().toSuspendFunction()
        runSuspend {
            bar2(3)
        }
        assertEquals(5, barCnt)
    }

    @Test
    fun testInvokeExperimental0() {
        testInvokeExperimentalImpl(false)
    }

    @Test
    fun testInvokeExperimental1() {
        testInvokeExperimentalImpl(true)
    }

    private fun testInvokeExperimentalImpl(shallSuspend: Boolean) {
        TestDispatcher("testInvokeExperimental").use { dispatcher ->
            val semaphore = Semaphore(0)
            val foo: suspend (Int) -> String = {
                dispatcher.assertThread()
                if (shallSuspend) {
                    dispatcher.yield()
                }
                "OK$it"
            }
            val fooExp = foo.toExperimentalSuspendFunction()
            val testCode: suspend () -> String = {
                dispatcher.assertThread()
                val result = invokeExperimentalSuspend<String> {
                    fooExp(42, it)
                }
                assertEquals("OK42", result)
                dispatcher.assertThread()
                "DONE"
            }
            testCode.startCoroutine(Continuation(dispatcher) { result ->
                dispatcher.assertThread()
                // todo: below does not work due to a bug in inline classes
//                    assertEquals("DONE", result.getOrThrow())
                semaphore.release()
            })
            semaphore.acquire()
        }
    }

    @Test
    fun testExperimentalBuildSequence() {
        suspend fun action(builder: ExperimentalSequenceBuilder<Int>) {
            invokeExperimentalSuspend<Unit> { builder.yield(1, it) }
            invokeExperimentalSuspend<Unit> { builder.yield(2, it) }
            invokeExperimentalSuspend<Unit> { builder.yield(3, it) }
        }
        val seq = experimentalBuildSequence(::action.toExperimentalSuspendFunction())
        assertEquals(listOf(1, 2, 3), seq.toList())
    }

    class MyElement : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<MyElement>
    }

    class MyExperimentalElement : ExperimentalAbstractCoroutineContextElement(Key) {
        companion object Key : ExperimentalCoroutineContext.Key<MyExperimentalElement>
    }
}

