/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.coroutines

import test.assertStaticAndRuntimeTypeIs
import kotlin.test.*
import kotlin.coroutines.*

/**
 * Test to ensure that coroutine machinery does not call equals/hashCode/toString anywhere.
 */
class CoroutinesReferenceValuesTest {
    class BadClass {
        override fun equals(other: Any?): Boolean = error("equals")
        override fun hashCode(): Int = error("hashCode")
        override fun toString(): String = error("toString")
    }

    var counter = 0

    // tail-suspend function via suspendCoroutine (test SafeContinuation)
    suspend fun getBadClassViaSuspend(): BadClass = suspendCoroutine { cont ->
        counter++
        cont.resume(BadClass())
    }

    // state machine
    suspend fun checkBadClassTwice() {
        assertStaticAndRuntimeTypeIs<BadClass>(getBadClassViaSuspend())
        assertStaticAndRuntimeTypeIs<BadClass>(getBadClassViaSuspend())
    }

    fun <T> suspend(block: suspend () -> T) = block

    @Test
    fun testBadClass() {
        val bad = suspend {
            checkBadClassTwice()
            getBadClassViaSuspend()
        }
        var result: BadClass? = null
        bad.startCoroutine(object : Continuation<BadClass> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(result_: Result<BadClass>) {
                assertTrue(result == null)
                result = result_.getOrThrow()
            }
        })
        assertTrue(result is BadClass)
        assertEquals(3, counter)
    }
}