package kotlin.test.tests

import org.junit.*
import kotlin.test.*

class BasicAssertionsTest {
    @Test
    fun testAssertEquals() {
        assertEquals(1, 1)
    }

    @Test
    fun testAssertEqualsString() {
        assertEquals("a", "a")
    }

    @Test
    fun testAssertFailsWith() {
        assertFailsWith<IllegalStateException> { throw IllegalStateException() }
        assertFailsWith<AssertionError> { throw AssertionError() }

        run {
            try {
                assertFailsWith<IllegalStateException> { throw IllegalArgumentException() }
            }
            catch (e: AssertionError) {
                return@run
            }
            throw AssertionError("Expected to fail")
        }

        run {
            try {
                assertFailsWith<IllegalStateException> {  }
            }
            catch (e: AssertionError) {
                return@run
            }
            throw AssertionError("Expected to fail")
        }
    }

    @Test
    fun testAssertEqualsFails() {
        assertFailsWith<AssertionError> { assertEquals(1, 2) }
    }

    @Test
    fun testAssertTrue() {
        assertTrue(true)
        assertTrue { true }
    }

    @Test()
    fun testAssertTrueFails() {
        assertFailsWith<AssertionError> { assertTrue(false) }
        assertFailsWith<AssertionError> { assertTrue { false } }
    }

    @Test
    fun testAssertFalse() {
        assertFalse(false)
        assertFalse { false }
        assertFailsWith<AssertionError> { assertFalse(true) }
        assertFailsWith<AssertionError> { assertFalse { true } }
    }

    @Test
    fun testAssertFails() {
        assertFails { throw IllegalStateException() }
    }

    @Test()
    fun testAssertFailsFails() {
        assertFailsWith<AssertionError> { assertFails {  } }
    }


    @Test
    fun testAssertNotEquals() {
        assertNotEquals(1, 2)
    }

    @Test()
    fun testAssertNotEqualsFails() {
        assertFailsWith<AssertionError> { assertNotEquals(1, 1) }
    }

    @Test
    fun testAssertNotNull() {
        assertNotNull(true)
    }

    @Test()
    fun testAssertNotNullFails() {
        assertFailsWith<AssertionError> { assertNotNull(null) }
    }

    @Test
    fun testAssertNotNullLambda() {
        assertNotNull("") { assertEquals("", it) }
    }

    @Test
    fun testAssertNotNullLambdaFails() {
        assertFailsWith<AssertionError> {
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
        assertFailsWith<AssertionError> { assertNull("") }
    }

    @Test()
    fun testFail() {
        assertFailsWith<AssertionError> { fail("should fail") }
    }

    @Test
    fun testExpect() {
        expect(1) { 1 }
        assertFailsWith<AssertionError> { expect(1) { 2 } }
    }

}