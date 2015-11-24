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

    @Test(expected = AssertionError::class)
    fun testAssertEqualsFails() {
        assertEquals(1, 2)
    }

    @Test
    fun testAssertTrue() {
        assertTrue(true)
    }

    @Test
    fun testAssertTrueWithLambda() {
        assertTrue { true }
    }

    @Test(expected = AssertionError::class)
    fun testAssertTrueFails() {
        assertTrue(false)
    }

    @Test(expected = AssertionError::class)
    fun testAssertTrueWithLambdaFails() {
        assertTrue { false }
    }

    @Test
    fun testAssertFalse() {
        assertFalse(false)
    }

    @Test
    fun testAssertFalseLambda() {
        assertFalse { false }
    }

    @Test(expected = AssertionError::class)
    fun testAssertFalseFails() {
        assertFalse(true)
    }

    @Test(expected = AssertionError::class)
    fun testAssertFalseWithLambdaFails() {
        assertFalse { true }
    }

    @Test
    fun testAssertFails() {
        assertFails { throw IllegalStateException() }
    }

    @Test(expected = AssertionError::class)
    fun testAssertFailsFails() {
        assertFails {  }
        Assert.fail("Shouldn't pass here")
    }


    @Test
    fun testAssertNotEquals() {
        assertNotEquals(1, 2)
    }

    @Test(expected = AssertionError::class)
    fun testAssertNotEqualsFails() {
        assertNotEquals(1, 1)
    }

    @Test
    fun testAssertNotNull() {
        assertNotNull(true)
    }

    @Test(expected = AssertionError::class)
    fun testAssertNotNullFails() {
        assertNotNull(null)
    }

    @Test
    fun testAssertNotNullLambda() {
        assertNotNull("") { assertEquals("", it) }
    }

    @Test(expected = AssertionError::class)
    fun testAssertNotNullLambdaFails() {
        assertNotNull(null) {
            @Suppress("UNREACHABLE_CODE")
            assertNotNull(it)
        }
    }

    @Test
    fun testAssertNull() {
        assertNull(null)
    }

    @Test(expected = AssertionError::class)
    fun testAssertNullFails() {
        assertNull(true)
    }

    @Test(expected = AssertionError::class)
    fun testFail() {
        fail("should fail")
    }

    @Test
    fun testExpect() {
        expect(1) { 1 }
    }

    @Test(expected = AssertionError::class)
    fun testExpectFails() {
        expect(1) { 2 }
    }
}