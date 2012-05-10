package test.collections

import org.junit.Test as test
import kotlin.test.*

class PreconditionsTest() {

    test fun passingRequire() {
        require(true)

        var called = false
        require(true) { called = true; "some message" }
        assertFalse(called)
    }

    test fun failingRequire() {
        val error = failsWith<IllegalArgumentException> {
            require(false)
        }
        assertNotNull(error.getMessage())
    }

    test fun passingRequireWithMessage() {
        require(true, "Hello")
    }

    test fun failingRequireWithMessage() {
        val error = failsWith<IllegalArgumentException> {
            require(false, "Hello")
        }
        assertEquals("Hello", error.getMessage())
    }

    test fun failingRequireWithLazyMessage() {
        val error = failsWith<IllegalArgumentException> {
            require(false) {"Hello"}
        }
        assertEquals("Hello", error.getMessage())
    }

    test fun passingCheck() {
        check(true)

        var called = false
        check(true) { called = true; "some message" }
        assertFalse(called)
    }

    test fun failingCheck() {
        val error = failsWith<IllegalStateException> {
            check(false)
        }
        assertNotNull(error.getMessage())
    }

    test fun passingCheckWithMessage() {
        check(true, "Hello")
    }

    test fun failingCheckWithMessage() {
        val error = failsWith<IllegalStateException> {
            check(false, "Hello")
        }
        assertEquals("Hello", error.getMessage())
    }

    test fun failingCheckWithLazyMessage() {
        val error = failsWith<IllegalStateException> {
            check(false) {"Hello"}
        }
        assertEquals("Hello", error.getMessage())
    }

    test fun requireNotNull() {
        val s1: String? = "S1"
        val r1: String = requireNotNull(s1)
        assertEquals("S1", r1)
    }

    test fun requireNotNullFails() {
        failsWith<IllegalArgumentException> {
            val s2: String? = null
            requireNotNull(s2)
        }
    }

    test fun checkNotNull() {
        val s1: String? = "S1"
        val r1: String = checkNotNull(s1)
        assertEquals("S1", r1)
    }

    test fun checkNotNullFails() {
        failsWith<IllegalStateException> {
            val s2: String? = null
            checkNotNull(s2)
        }
    }

// TODO: uncomment when KT-1540 is resolved.
//    test fun passingAssert() {
//        assert(true)
//        var called = false
//        assert(true) { called = true; "some message" }
//
//        assertFalse(called)
//    }
//
//
//    test fun failingAssert() {
//        val error = fails {
//            assert(false)
//        }
//        if(error is IllegalStateException) {
//            assertNull(error.getMessage())
//        } else {
//            fail("Invalid exception type: "+error)
//        }
//    }
//
//    test fun passingAssertWithMessage() {
//        assert(true, "Hello")
//    }
//
//    test fun failingAssertWithMessage() {
//        val error = fails {
//            assert(false, "Hello")
//        }
//        if(error is IllegalStateException) {
//            assertEquals("Hello", error.getMessage())
//        } else {
//            fail("Invalid exception type: "+error)
//        }
//    }
//
//    test fun failingAssertWithLazyMessage() {
//        val error = fails {
//            assert(false) {"Hello"}
//        }
//        if(error is IllegalStateException) {
//            assertEquals("Hello", error.getMessage())
//        } else {
//            fail("Invalid exception type: "+error)
//        }
//    }
}