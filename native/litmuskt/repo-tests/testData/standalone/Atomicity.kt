// KIND: STANDALONE

import kotlin.test.*
import org.jetbrains.litmuskt.*
import org.jetbrains.litmuskt.autooutcomes.*
import org.jetbrains.litmuskt.barriers.*
import org.jetbrains.litmuskt.tests.*

fun runTest(test: LitmusTest<*>) {
    val result = runTestWithSampleParams(test)
    println(result)
    assertFalse(result.hasForbidden())
}

@Test
fun int() = runTest(Atomicity.Int)

@Test
fun long() = runTest(Atomicity.Long)

@Test
fun longVolatile() = runTest(Atomicity.LongVolatile)
