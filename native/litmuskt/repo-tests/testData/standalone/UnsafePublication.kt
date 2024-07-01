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
fun plain() = runTest(UnsafePublication.Plain)

@Test
fun volatileAnnotated() = runTest(UnsafePublication.VolatileAnnotated)

@Test
fun plainWithConstructor() = runTest(UnsafePublication.PlainWithConstructor)

@Test
fun plainArray() = runTest(UnsafePublication.PlainArray)

@Test
fun reference() = runTest(UnsafePublication.Reference)

@Test
fun plainWithLeakingConstructor() = runTest(UnsafePublication.PlainWithLeakingConstructor)
