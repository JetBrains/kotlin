package test.collections

import junit.framework.TestCase
import kotlin.test.*
import java.lang.IllegalArgumentException

class PreconditionsTest() : TestCase() {

    fun testPassingRequire() {
        require(true)
    }

    fun testFailingRequire() {
        val error = fails {
            require(false)
        }
        if(error is IllegalArgumentException) {
            assertNull(error.getMessage())
        } else {
            fail("Invalid exception type: "+error)
        }
    }

    fun testPassingRequireWithMessage() {
        require(true, "Hello")
    }

    fun testFailingRequireWithMessage() {
        val error = fails {
            require(false, "Hello")
        }
        if(error is IllegalArgumentException) {
            assertEquals("Hello", error.getMessage())
        } else {
            fail("Invalid exception type: "+error)
        }
    }

    fun testPassingCheck() {
        check(true)
    }

    fun testFailingCheck() {
        val error = fails {
            check(false)
        }
        if(error is IllegalStateException) {
            assertNull(error.getMessage())
        } else {
            fail("Invalid exception type: "+error)
        }
    }

    fun testPassingCheckWithMessage() {
        check(true, "Hello")
    }

    fun testFailingCheckWithMessage() {
        val error = fails {
            check(false, "Hello")
        }
        if(error is IllegalStateException) {
            assertEquals("Hello", error.getMessage())
        } else {
            fail("Invalid exception type: "+error)
        }
    }


// TODO: uncomment when KT-1540 is resolved.
//    fun testPassingAssert() {
//        assert(true)
//    }
//
//
//    fun testFailingAssert() {
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
//    fun testPassingAssertWithMessage() {
//        assert(true, "Hello")
//    }
//
//    fun testFailingAssertWithMessage() {
//        val error = fails {
//            assert(false, "Hello")
//        }
//        if(error is IllegalStateException) {
//            assertEquals("Hello", error.getMessage())
//        } else {
//            fail("Invalid exception type: "+error)
//        }
//    }
}