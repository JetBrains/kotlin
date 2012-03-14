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

// TODO: uncomment when KT-1540 is resolved.
//    fun testPassingCheck() {
//        check1(true)
//    }
//
//    fun testFailingCheck() {
//        val error = fails {
//            check1(false)
//        }
//        if(error is IllegalStateException) {
//            assertNull(error.getMessage())
//        } else {
//            fail("Invalid exception type: "+error)
//        }
//    }
//
//    fun testPassingCheckWithMessage() {
//        check2(true, "Hello")
//    }
//
//    fun testFailingCheckWithMessage() {
//        val error = fails {
//            check2(false, "Hello")
//        }
//        if(error is IllegalStateException) {
//            assertEquals("Hello", error.getMessage())
//        } else {
//            fail("Invalid exception type: "+error)
//        }
//    }


// TODO: uncomment when KT-1540 is resolved.
//    fun testPassingAssert() {
//        assert1(true)
//    }
//
//    fun testFailingAssert() {
//        val error = fails {
//            assert1(false)
//        }
//        if(error is IllegalStateException) {
//            assertNull(error.getMessage())
//        } else {
//            fail("Invalid exception type: "+error)
//        }
//    }
//
//    fun testPassingAssertWithMessage() {
//        assert2(true, "Hello")
//    }
//
//    fun testFailingAssertWithMessage() {
//        val error = fails {
//            assert2(false, "Hello")
//        }
//        if(error is IllegalStateException) {
//            assertEquals("Hello", error.getMessage())
//        } else {
//            fail("Invalid exception type: "+error)
//        }
//    }
}