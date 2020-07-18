/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.tests

import kotlin.reflect.typeOf
import kotlin.test.*

class BasicAssertionsTest {
    @Test
    fun testAssertEquals() {
        assertEquals(1, 1)
    }

    @Test
    fun testAssertSame() {
        val instance: Any = object {}
        assertSame(instance, instance)
    }

    @Test
    fun testAssertEqualsString() {
        assertEquals("a", "a")
    }

    @Test
    fun testAssertFailsWith() {
        assertFailsWith<IllegalStateException> { throw IllegalStateException() }
        assertFailsWith<AssertionError> { throw AssertionError() }
    }

    @Test
    fun testAssertFailsWithFails() {
        assertTrue(true) // at least one assertion required for qunit

        withDefaultAsserter run@{
            val rootCause = IllegalArgumentException()
            try {
                assertFailsWith<IllegalStateException> { throw rootCause }
            } catch (e: AssertionError) {
                if (e.cause !== rootCause) throw AssertionError("Expected to fail with correct cause")
                return@run
            }
            throw AssertionError("Expected to fail")
        }

        withDefaultAsserter run@{
            try {
                assertFailsWith<IllegalStateException> { }
            } catch (e: AssertionError) {
                return@run
            }
            throw AssertionError("Expected to fail")
        }
    }

    @Test
    fun testAssertFailsWithClass() {
        assertFailsWith(IllegalArgumentException::class) {
            throw IllegalArgumentException("This is illegal")
        }
    }

    @Test
    fun testAssertFailsWithClassFails() {
        val rootCause = IllegalStateException()
        val actual = checkFailedAssertion {
            assertFailsWith(IllegalArgumentException::class) { throw rootCause }
        }
        assertSame(rootCause, actual.cause, "Expected to fail with correct cause")

        checkFailedAssertion {
            assertFailsWith(Exception::class) { }
        }
    }

    @Test
    fun testAssertEqualsFails() {
        checkFailedAssertion { assertEquals(1, 2) }
    }

    @Test
    fun testAssertSameFails() {
        val instance1: Any = object {}
        val instance2: Any = object {}
        checkFailedAssertion { assertSame(instance1, instance2) }
    }

    @Test
    fun testAssertEqualsDouble() {
        assertEquals(0.01, 0.02, .01)

        // 0.0, -0.0
        assertEquals(0.0, 0.0, 0.0)
        assertEquals(0.0, -0.0, 0.0)
        assertEquals(-0.0, 0.0, 0.0)
        assertEquals(0.0, 0.0, -0.0)
        assertEquals(0.0, -0.0, -0.0)
        assertEquals(-0.0, 0.0, -0.0)

        // NaN
        val nans = doubleArrayOf(Double.NaN, Double.fromBits(0xFFF80000L shl 32))
        for (nan1 in nans) {
            assertTrue(nan1.isNaN())
            for (nan2 in nans) {
                assertEquals(nan1, nan2, 0.1)
                assertEquals(nan1, nan2, 0.0)
            }
        }

        // MIN_VALUE, MAX_VALUE
        assertEquals(Double.MAX_VALUE, Double.MAX_VALUE, 0.0)
        assertEquals(Double.MIN_VALUE, Double.MIN_VALUE, 0.0)
        assertEquals(Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE)
        assertEquals(Double.MIN_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)

        // POSITIVE_INFINITY, NEGATIVE_INFINITY
        assertEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0)
        assertEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0)
        assertEquals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        assertEquals(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
    }

    @Test
    fun testAssertEqualsDoubleFails() {
        checkFailedAssertion { assertEquals(0.01, 1.03, .01) }

        // negative absoluteTolerance
        assertFailsWith<IllegalArgumentException> { assertEquals(0.01, 1.03, -5.0) }

    }

    @Test
    fun testAssertEqualsFloat() {
        assertEquals(0.01f, 0.02f, .01f)

        // 0.0, -0.0
        assertEquals(0.0f, 0.0f, 0.0f)
        assertEquals(0.0f, -0.0f, 0.0f)
        assertEquals(-0.0f, 0.0f, 0.0f)
        assertEquals(0.0f, 0.0f, -0.0f)
        assertEquals(0.0f, -0.0f, -0.0f)
        assertEquals(-0.0f, 0.0f, -0.0f)

        // NaN
        val nans = floatArrayOf(Float.NaN, Float.fromBits(0xFFC00000.toInt()))
        for (nan1 in nans) {
            assertTrue(nan1.isNaN())
            for (nan2 in nans) {
                assertEquals(nan1, nan2, 0.1f)
                assertEquals(nan1, nan2, 0.0f)
            }
        }

        // MIN_VALUE, MAX_VALUE
        assertEquals(Float.MAX_VALUE, Float.MAX_VALUE, 0.0f)
        assertEquals(Float.MIN_VALUE, Float.MIN_VALUE, 0.0f)
        assertEquals(Float.MAX_VALUE, Float.MIN_VALUE, Float.MAX_VALUE)
        assertEquals(Float.MIN_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)

        // POSITIVE_INFINITY, NEGATIVE_INFINITY
        assertEquals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0.0f)
        assertEquals(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f)
        assertEquals(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
        assertEquals(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    }

    @Test
    fun testAssertEqualsFloatFails() {
        checkFailedAssertion { assertEquals(0.01f, 1.03f, .01f) }

        // negative absoluteTolerance
        assertFailsWith<IllegalArgumentException> { assertEquals(0.01f, 1.03f, -5.0f) }
    }

    @Test
    fun testAssertNotEqualsDouble() {
        assertNotEquals(0.1, 0.3, 0.1)
    }

    @Test
    fun testAssertNotEqualsDoubleFails() {
        checkFailedAssertion { assertNotEquals(0.1, 0.11, 0.1) }

        // negative absoluteTolerance
        assertFailsWith<IllegalArgumentException> { assertNotEquals(0.1, 0.11, -0.001) }
    }

    @Test
    fun testAssertNotEqualsFloat() {
        assertNotEquals(0.1f, 0.3f, 0.1f)
    }

    @Test
    fun testAssertNotEqualsFloatFails() {
        checkFailedAssertion { assertNotEquals(0.1f, 0.11f, .1f) }

        // negative absoluteTolerance
        assertFailsWith<IllegalArgumentException> { assertNotEquals(0.1f, 0.11f, -0.001f) }
    }

    @Test
    fun testAssertTrue() {
        assertTrue(true)
        assertTrue { true }
    }

    @Test()
    fun testAssertTrueFails() {
        checkFailedAssertion { assertTrue(false) }
        checkFailedAssertion { assertTrue { false } }
    }

    @Test
    fun testAssertFalse() {
        assertFalse(false)
        assertFalse { false }
    }

    @Test
    fun testAssertFalseFails() {
        checkFailedAssertion { assertFalse(true) }
        checkFailedAssertion { assertFalse { true } }
    }

    @Test
    fun testAssertFails() {
        assertFails { throw IllegalStateException() }
    }

    @Test()
    fun testAssertFailsFails() {
        checkFailedAssertion { assertFails { } }
    }


    @Test
    fun testAssertNotEquals() {
        assertNotEquals(1, 2)
    }

    @Test
    fun testAssertNotSame() {
        val instance1: Any = object {}
        val instance2: Any = object {}
        assertNotSame(instance1, instance2)
    }

    @Test()
    fun testAssertNotEqualsFails() {
        checkFailedAssertion { assertNotEquals(1, 1) }
    }

    @Test
    fun testAssertNotSameFails() {
        val instance: Any = object {}
        checkFailedAssertion { assertNotSame(instance, instance) }
    }

    @Test
    fun testAssertNotNull() {
        assertNotNull(true)
    }

    @Test()
    fun testAssertNotNullFails() {
        checkFailedAssertion { assertNotNull<Any>(null) }
    }

    @Test
    fun testAssertNotNullLambda() {
        assertNotNull("") { assertEquals("", it) }
    }

    @Test
    fun testAssertNotNullLambdaFails() {
        checkFailedAssertion {
            val value: String? = null
            assertNotNull(value) {
                it.substring(0, 0)
            }
        }
    }

    @Test
    fun testAssertNull() {
        assertNull(null)
    }

    @Test
    fun testAssertNullFails() {
        checkFailedAssertion { assertNull("") }
    }

    @Test()
    fun testFail() {
        val message = "should fail"
        val actual = checkFailedAssertion { fail(message) }
        assertEquals(message, actual.message)
    }

    @Test
    fun testFailWithCause() {
        val message = "should fail due to"
        val cause = IllegalStateException()
        val actual = checkFailedAssertion { fail(message, cause) }
        assertEquals(message, actual.message)
        assertSame(cause, actual.cause)
    }

    @Test
    fun testExpect() {
        expect(1) { 1 }
    }

    @Test
    fun testExpectFails() {
        checkFailedAssertion { expect(1) { 2 } }
    }

    @Test
    fun testAssertIsOfType() {
        val s: Any = "test"
        val result = assertIs<String>(s)
        assertEquals(4, s.length)
        assertEquals(s, result)
        assertEquals(4, result.length)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testAssertIsOfTypeFails() {
        val error = checkFailedAssertion { assertIs<Int>("test") }
        val message = assertNotNull(error.message)
        val onFailure = "Actual message: $message"
        assertTrue(message.startsWith("Expected value to be of type"), onFailure)
        assertTrue(message.contains(typeOf<Int>().toString()), onFailure)
        assertTrue(message.contains("String"), onFailure)
    }

    @Test
    fun testAssertIsNotOfType() {
        assertIsNot<Int>("test")
    }

    @Test
    @OptIn(ExperimentalStdlibApi::class)
    fun testAssertIsNotOfTypeFails() {
        val error = checkFailedAssertion { assertIsNot<Int>(1) }
        val message = assertNotNull(error.message)
        val onFailure = "Actual message: $message"
        assertTrue(message.startsWith("Expected value to not be of type"), onFailure)
        assertTrue(message.contains(typeOf<Int>().toString()), onFailure)
    }
}


internal fun testFailureMessage(expected: String, block: () -> Unit) {
    val exception = checkFailedAssertion(block)
    assertEquals(expected, exception.message, "Wrong assertion message")
}

internal fun checkFailedAssertion(assertion: () -> Unit): AssertionError {
    return assertFailsWith<AssertionError> { withDefaultAsserter(assertion) }
}

private fun withDefaultAsserter(block: () -> Unit) {
    val current = overrideAsserter(DefaultAsserter)
    try {
        block()
    } finally {
        overrideAsserter(current)
    }
}
