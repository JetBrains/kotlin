/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.test.tests

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
    }

   @Test
   fun testAssertFailsWithFails() {
       withDefaultAsserter run@ {
           try {
               assertFailsWith<IllegalStateException> { throw IllegalArgumentException() }
           }
           catch (e: AssertionError) {
               return@run
           }
           throw AssertionError("Expected to fail")
       }
       withDefaultAsserter run@ {
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
    fun testAssertFailsWithClass() {
        assertFailsWith<IllegalArgumentException> {
            throw IllegalArgumentException("This is illegal")
        }
    }

    @Test
    fun testAssertFailsWithClassFails() {
        checkFailedAssertion {
            assertFailsWith<IllegalArgumentException> { throw IllegalStateException() }
        }

        checkFailedAssertion {
            assertFailsWith<Exception> { }
        }
    }

    @Test
    fun testAssertEqualsFails() {
        checkFailedAssertion { assertEquals(1, 2) }
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
        checkFailedAssertion{ assertFalse { true } }
    }

    @Test
    fun testAssertFails() {
        assertFails { throw IllegalStateException() }
    }

    @Test()
    fun testAssertFailsFails() {
        checkFailedAssertion { assertFails {  } }
    }


    @Test
    fun testAssertNotEquals() {
        assertNotEquals(1, 2)
    }

    @Test()
    fun testAssertNotEqualsFails() {
        checkFailedAssertion { assertNotEquals(1, 1) }
    }

    @Test
    fun testAssertNotNull() {
        assertNotNull(true)
    }

    @Test()
    fun testAssertNotNullFails() {
        checkFailedAssertion { assertNotNull(null) }
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
        checkFailedAssertion { fail("should fail") }
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
    fun testContracts() {
        open class S
        class P(val str: String = "P") : S()

        val s: S = P()
        val p: Any = P("A")

        assertTrue(s is P)
        assertEquals("P", s.str)
        assertFalse(p !is P)
        assertEquals("A", p.str)

        val nullableT: P? = P("N")
        assertNotNull(nullableT)
        assertEquals("N", nullableT.str)
    }
}


private fun checkFailedAssertion(assertion: () -> Unit) {
    assertFailsWith<AssertionError> { withDefaultAsserter(assertion) }
}

@Suppress("INVISIBLE_MEMBER")
private fun withDefaultAsserter(block: () -> Unit) {
    block()
}
