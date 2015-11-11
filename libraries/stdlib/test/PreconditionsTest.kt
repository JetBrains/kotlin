package test.collections

import org.junit.Test as test
import kotlin.test.*

class PreconditionsTest() {

    @test fun passingRequire() {
        require(true)

        var called = false
        require(true) { called = true; "some message" }
        assertFalse(called)
    }

    @test fun failingRequire() {
        val error = assertFailsWith(IllegalArgumentException::class) {
            require(false)
        }
        assertNotNull(error.getMessage())
    }

    @test fun failingRequireWithLazyMessage() {
        val error = assertFailsWith(IllegalArgumentException::class) {
            require(false) { "Hello" }
        }
        assertEquals("Hello", error.getMessage())
    }

    @test fun passingCheck() {
        check(true)

        var called = false
        check(true) { called = true; "some message" }
        assertFalse(called)
    }

    @test fun failingCheck() {
        val error = assertFailsWith(IllegalStateException::class) {
            check(false)
        }
        assertNotNull(error.getMessage())
    }

    @test fun failingCheckWithLazyMessage() {
        val error = assertFailsWith(IllegalStateException::class) {
            check(false) { "Hello" }
        }
        assertEquals("Hello", error.getMessage())
    }

    @test fun requireNotNull() {
        val s1: String? = "S1"
        val r1: String = requireNotNull(s1)
        assertEquals("S1", r1)
    }

    @test fun requireNotNullFails() {
        assertFailsWith(IllegalArgumentException::class) {
            val s2: String? = null
            requireNotNull(s2)
        }
    }

    @test fun requireNotNullWithLazyMessage() {
        val error = assertFailsWith(IllegalArgumentException::class) {
            val obj: Any? = null
            requireNotNull(obj) { "Message" }
        }
        assertEquals("Message", error.getMessage())

        var lazyCalled: Boolean = false
        requireNotNull("not null") {
            lazyCalled = true
            "Message"
        }
        assertFalse(lazyCalled, "Message is not evaluated if the condition is met")
    }

    @test fun checkNotNull() {
        val s1: String? = "S1"
        val r1: String = checkNotNull(s1)
        assertEquals("S1", r1)
    }

    @test fun checkNotNullFails() {
        assertFailsWith(IllegalStateException::class) {
            val s2: String? = null
            checkNotNull(s2)
        }
    }

    @test fun passingAssert() {
        assert(true)
        var called = false
        assert(true) { called = true; "some message" }

        assertFalse(called)
    }


    @test fun failingAssert() {
        val error = assertFails {
            assert(false)
        }
        if (error is AssertionError) {
            assertEquals("Assertion failed", error.getMessage())
        } else {
            fail("Invalid exception type: " + error)
        }
    }

    @test fun error() {
        val error = assertFails {
            error("There was a problem")
        }
        if (error is IllegalStateException) {
            assertEquals("There was a problem", error.getMessage())
        } else {
            fail("Invalid exception type: " + error)
        }
    }

    @test fun passingAssertWithMessage() {
        assert(true) { "Hello" }
    }

    @test fun failingAssertWithMessage() {
        val error = assertFails {
            assert(false) { "Hello" }
        }
        if (error is AssertionError) {
            assertEquals("Hello", error.getMessage())
        } else {
            fail("Invalid exception type: " + error)
        }
    }

    @test fun failingAssertWithLazyMessage() {
        val error = assertFails {
            assert(false) { "Hello" }
        }
        if (error is AssertionError) {
            assertEquals("Hello", error.getMessage())
        } else {
            fail("Invalid exception type: " + error)
        }
    }
}