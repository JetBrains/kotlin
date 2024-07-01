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
fun plain() = runTest(LoadBuffering.Plain)

@Test
fun volatileAnnotated() = runTest(LoadBuffering.VolatileAnnotated)

@Test
fun plainWithFakeDependencies() = runTest(LoadBuffering.PlainWithFakeDependencies)

@Test
fun noOutOfThinAirValues() = runTest(LoadBuffering.NoOutOfThinAirValues)
