/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.coroutines

import kotlin.test.*
import org.junit.Test
import kotlin.coroutines.experimental.*

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
        assertTrue(getBadClassViaSuspend() is BadClass)
        assertTrue(getBadClassViaSuspend() is BadClass)
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

            override fun resume(value: BadClass) {
                assertTrue(result == null)
                result = value
            }

            override fun resumeWithException(exception: Throwable) {
                throw exception
            }
        })
        assertTrue(result is BadClass)
        assertEquals(3, counter)
    }
}