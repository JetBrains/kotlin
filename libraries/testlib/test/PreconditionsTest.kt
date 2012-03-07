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

//    // Not sure why the assert method is not being resolved.
//    fun ignorePassingAssert() {
//        assert(true)
//    }
//
//    fun ignoreFailingAssert() {
//        failsWith<AssertionError> {
//            assert(false)
//        }
//    }
}