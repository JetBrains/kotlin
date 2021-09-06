/*
 *  Test different types of callable with foreignExceptionMode=objc-wrap
 */

import kotlin.test.*
//import objcTests.*
import objc_wrap.*
import kotlin.native.internal.*
import kotlinx.cinterop.*

fun testInner(name: String, reason: String) {
    val frame = runtimeGetCurrentFrame()
    var finallyBlockTest = "FAILED"
    var catchBlockTest = "NOT EXPECTED"
    try {
        raiseExc(name, reason)
    } catch (e: RuntimeException) {
        assertEquals(frame, runtimeGetCurrentFrame())
        catchBlockTest = "This shouldn't happen"
    } finally {
        finallyBlockTest = "PASSED"
    }
    assertEquals("NOT EXPECTED", catchBlockTest)
    assertEquals("PASSED", finallyBlockTest)
}

typealias CallMe = (String, String) -> Unit

@Test fun testExceptionWrap(raise: CallMe) {
    val frame = runtimeGetCurrentFrame()
    val name = "Some native exception"
    val reason = "Illegal value"
    var finallyBlockTest = "FAILED"
    var catchBlockTest = "FAILED"
    try {
        raise(name, reason)
    } catch (e: ForeignException) {
        assertEquals(frame, runtimeGetCurrentFrame())
        val ret = logExc(e.nativeException) // return NSException name
        assertEquals(name, ret)
        assertEquals("$name:: $reason", e.message)
        catchBlockTest = "PASSED"
    } finally {
        finallyBlockTest = "PASSED"
    }
    assertEquals("PASSED", catchBlockTest)
    assertEquals("PASSED", finallyBlockTest)
}

class Bar() : Foo()

fun main() {
    testExceptionWrap(::raiseExc)   // simple
    testExceptionWrap(::testInner)  // nested try block
    testExceptionWrap(Foo::classMethodThrow)  // class method
    testExceptionWrap(Foo()::instanceMethodThrow)    // instance method
    testExceptionWrap(Bar()::instanceMethodThrow)    // fake override
}