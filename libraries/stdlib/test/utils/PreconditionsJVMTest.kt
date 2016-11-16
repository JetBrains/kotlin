@file:kotlin.jvm.JvmVersion
package test.utils

import org.junit.Test
import kotlin.test.*

class PreconditionsJVMTest() {

    @Test fun passingRequire() {
        require(true)

        var called = false
        require(true) { called = true; "some message" }
        assertFalse(called)
    }

    @Test fun failingRequire() {
        val error = assertFailsWith(IllegalArgumentException::class) {
            require(false)
        }
        assertNotNull(error.message)
    }

    @Test fun failingRequireWithLazyMessage() {
        val error = assertFailsWith(IllegalArgumentException::class) {
            require(false) { "Hello" }
        }
        assertEquals("Hello", error.message)
    }

    @Test fun passingCheck() {
        check(true)

        var called = false
        check(true) { called = true; "some message" }
        assertFalse(called)
    }

    @Test fun failingCheck() {
        val error = assertFailsWith(IllegalStateException::class) {
            check(false)
        }
        assertNotNull(error.message)
    }

    @Test fun failingCheckWithLazyMessage() {
        val error = assertFailsWith(IllegalStateException::class) {
            check(false) { "Hello" }
        }
        assertEquals("Hello", error.message)
    }

    @Test fun requireNotNull() {
        val s1: String? = "S1"
        val r1: String = requireNotNull(s1)
        assertEquals("S1", r1)
    }

    @Test fun requireNotNullFails() {
        assertFailsWith(IllegalArgumentException::class) {
            val s2: String? = null
            requireNotNull(s2)
        }
    }

    @Test fun requireNotNullWithLazyMessage() {
        val error = assertFailsWith(IllegalArgumentException::class) {
            val obj: Any? = null
            requireNotNull(obj) { "Message" }
        }
        assertEquals("Message", error.message)

        var lazyCalled: Boolean = false
        requireNotNull("not null") {
            lazyCalled = true
            "Message"
        }
        assertFalse(lazyCalled, "Message is not evaluated if the condition is met")
    }

    @Test fun checkNotNull() {
        val s1: String? = "S1"
        val r1: String = checkNotNull(s1)
        assertEquals("S1", r1)
    }

    @Test fun checkNotNullFails() {
        assertFailsWith(IllegalStateException::class) {
            val s2: String? = null
            checkNotNull(s2)
        }
    }

    @Test fun passingAssert() {
        assert(true)
        var called = false
        assert(true) { called = true; "some message" }

        assertFalse(called)
    }


    @Test fun failingAssert() {
        val error = assertFails {
            assert(false)
        }
        if (error is AssertionError) {
            assertEquals("Assertion failed", error.message)
        } else {
            fail("Invalid exception type: " + error)
        }
    }

    @Test fun error() {
        val error = assertFails {
            error("There was a problem")
        }
        if (error is IllegalStateException) {
            assertEquals("There was a problem", error.message)
        } else {
            fail("Invalid exception type: " + error)
        }
    }

    @Test fun passingAssertWithMessage() {
        assert(true) { "Hello" }
    }

    @Test fun failingAssertWithMessage() {
        val error = assertFails {
            assert(false) { "Hello" }
        }
        if (error is AssertionError) {
            assertEquals("Hello", error.message)
        } else {
            fail("Invalid exception type: " + error)
        }
    }

    @Test fun failingAssertWithLazyMessage() {
        val error = assertFails {
            assert(false) { "Hello" }
        }
        if (error is AssertionError) {
            assertEquals("Hello", error.message)
        } else {
            fail("Invalid exception type: " + error)
        }
    }
}