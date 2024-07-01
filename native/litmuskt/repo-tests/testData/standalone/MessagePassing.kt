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
fun plain() = runTest(MessagePassing.Plain)

@Test
fun volatileAnnotated() = runTest(MessagePassing.VolatileAnnotated)

@Test
fun raceFree() = runTest(MessagePassing.RaceFree)

@Test
fun missingVolatile() = runTest(MessagePassing.MissingVolatile)
