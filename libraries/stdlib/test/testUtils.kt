/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test

import kotlin.math.withSign
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

// just a static type check
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> assertStaticTypeIs(@Suppress("UNUSED_PARAMETER") value: @kotlin.internal.NoInfer T) {}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
inline fun <reified T> assertStaticAndRuntimeTypeIs(value: @kotlin.internal.NoInfer T) {
    @Suppress("USELESS_CAST")
    if ((value as Any?) !is T) {
        fail("Expected value $value to have ${T::class} type")
    }
}


fun <T> assertArrayContentEquals(expected: Array<out T>, actual: Array<out T>, message: String? = null) = assertTrue(expected contentEquals actual, message)

fun assertArrayContentEquals(expected: ByteArray, actual: ByteArray, message: String? = null)       = assertTrue(expected contentEquals actual, message)
fun assertArrayContentEquals(expected: ShortArray, actual: ShortArray, message: String? = null)     = assertTrue(expected contentEquals actual, message)
fun assertArrayContentEquals(expected: IntArray, actual: IntArray, message: String? = null)         = assertTrue(expected contentEquals actual, message)
fun assertArrayContentEquals(expected: LongArray, actual: LongArray, message: String? = null)       = assertTrue(expected contentEquals actual, message)
fun assertArrayContentEquals(expected: FloatArray, actual: FloatArray, message: String? = null)     = assertTrue(expected contentEquals actual, message)
fun assertArrayContentEquals(expected: DoubleArray, actual: DoubleArray, message: String? = null)   = assertTrue(expected contentEquals actual, message)
fun assertArrayContentEquals(expected: BooleanArray, actual: BooleanArray, message: String? = null) = assertTrue(expected contentEquals actual, message)
fun assertArrayContentEquals(expected: CharArray, actual: CharArray, message: String? = null)       = assertTrue(expected contentEquals actual, message)

fun assertArrayContentEquals(expected: UIntArray, actual: UIntArray, message: String? = null)     = assertTrue(expected contentEquals actual, message)
fun assertArrayContentEquals(expected: ULongArray, actual: ULongArray, message: String? = null)   = assertTrue(expected contentEquals actual, message)
fun assertArrayContentEquals(expected: UShortArray, actual: UShortArray, message: String? = null) = assertTrue(expected contentEquals actual, message)
fun assertArrayContentEquals(expected: UByteArray, actual: UByteArray, message: String? = null)   = assertTrue(expected contentEquals actual, message)

fun assertIsNegativeZero(value: Double) {
    assertEquals(-0.0, value)
    assertEquals(-1.0, 1.0.withSign(value))
}
fun assertIsPositiveZero(value: Double) {
    assertEquals(0.0, value)
    assertEquals(1.0, 1.0.withSign(value))
}