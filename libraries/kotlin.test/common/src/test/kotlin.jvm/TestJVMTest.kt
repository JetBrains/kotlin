/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.test.tests

import kotlin.test.*
import org.junit.Test

class TestJVMTest {

    private fun expectAssertion(checkAssertion: (String?) -> Unit, block: () -> Unit) {
        try {
            block()
        }
        catch (e: AssertionError) {
            checkAssertion(e.message)
            return
        }
        fail("Expected AssertionError to be thrown.")
    }

    private val message = "Some details"
    private val expected = object {
        override fun toString() = "expected"
    }
    private val actual = object {
        override fun toString() = "actual"
    }

    @Test
    fun assertEqualsMessage() {
        expectAssertion( { msg ->
            assertNotNull(msg); msg!!
            assertTrue { msg.contains(expected.toString()) }
            assertTrue { msg.contains(actual.toString()) }
            assertFalse { msg.startsWith(".") }
        }, { assertEquals<Any>(expected, actual) })

        expectAssertion( { msg ->
            assertNotNull(msg); msg!!
            assertTrue { msg.contains(message) }
            assertTrue { msg.contains(expected.toString()) }
            assertTrue { msg.contains(actual.toString()) }
        } , { assertEquals<Any>(expected, actual, message) })
    }

    @Test
    fun assertNotEqualsMessage() {
        expectAssertion( { msg ->
            assertNotNull(msg); msg!!
            assertTrue { msg.contains(actual.toString()) }
            assertFalse { msg.startsWith(".") }
        }, { assertNotEquals(actual, actual) })

        expectAssertion( { msg ->
            assertNotNull(msg); msg!!
            assertTrue { msg.contains(message) }
            assertTrue { msg.contains(actual.toString()) }
        } , { assertNotEquals(actual, actual, message) })
    }


    @Test
    fun assertTrueMessage() {
        expectAssertion( { msg -> assertNotNull(msg) }, { assertTrue(false) })
        expectAssertion( { msg -> assertNotNull(msg) }, { assertTrue { false } })
        expectAssertion( { msg -> assertNotNull(msg) }, { assertTrue(null) { false } })
        expectAssertion( { msg -> msg!!.contains(message)}, { assertTrue(false, message)})
        expectAssertion( { msg -> msg!!.contains(message)}, { assertTrue(message) { false }})
    }

    @Test
    fun assertFalseMessage() {
        expectAssertion( { msg -> assertNotNull(msg) }, { assertFalse(true) })
        expectAssertion( { msg -> assertNotNull(msg) }, { assertFalse { true } })
        expectAssertion( { msg -> assertNotNull(msg) }, { assertFalse(null) { true } })
        expectAssertion( { msg -> assertTrue(msg!!.contains(message)) }, { assertFalse(true, message)})
        expectAssertion( { msg -> assertTrue(msg!!.contains(message)) }, { assertFalse(message) { true }})
    }

    @Test
    fun assertNotNullMessage() {
        expectAssertion( { msg -> msg!! }, { assertNotNull(null) })
        expectAssertion( { msg -> assertTrue(msg!!.contains(message)) }, { assertNotNull(null, message)})
    }

    @Test
    fun assertNullMessage() {
        expectAssertion( { msg -> msg!! }, { assertNull(actual) })
        expectAssertion( { msg -> msg!!
            assertTrue(msg.contains(message))
            assertTrue(msg.contains(actual.toString()))
        }, { assertNull(actual, message)})
    }
}