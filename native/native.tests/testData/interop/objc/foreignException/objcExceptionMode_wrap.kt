/*
 *  Test different behavior depending on foreignExceptionMode option
 */

import kotlin.test.*
import objcExceptionMode.*
import kotlinx.cinterop.*
import platform.objc.*
import kotlin.system.exitProcess

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
@Test fun testKT35056() {
    val name = "Some native exception"
    val reason = "Illegal value"
    var finallyBlockTest = "FAILED"
    var catchBlockTest = "FAILED"
    try {
        raiseExc(name, reason)
        assertNotEquals("FAILED", catchBlockTest)  // shall not get here anyway
    } catch (e: ForeignException) {
        val ret = logExc(e.nativeException) // return NSException name
        assertEquals(name, ret)
        assertEquals("$name:: $reason", e.message)
        println("OK: ForeignException")
        catchBlockTest = "PASSED"
    } finally {
        finallyBlockTest = "PASSED"
    }
    assertEquals("PASSED", catchBlockTest)
    assertEquals("PASSED", finallyBlockTest)
}

@Suppress("UNUSED_PARAMETER")
fun abnormal_handler(x: Any?) : Unit {
    println("OK: Ends with uncaught exception handler")
    exitProcess(0)
}

fun main() {
    // Depending on the `foreignxceptionMode` option (def file or cinterop cli) this test should ends
    // normally with `ForeignException` handled or abnormally with `abnormal_handler`.
    // Test shall validate output (golden value) from `abnormal_handler`.

    objc_setUncaughtExceptionHandler(staticCFunction(::abnormal_handler))

    testKT35056()
}