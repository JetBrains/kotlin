/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.tostring3

import kotlin.test.*

fun testByte() {
    val values = ByteArray(2)
    values[0] = Byte.MIN_VALUE
    values[1] = Byte.MAX_VALUE
    for (v in values) {
        println(v)
    }
}

fun testShort() {
    val values = ShortArray(2)
    values[0] = Short.MIN_VALUE
    values[1] = Short.MAX_VALUE
    for (v in values) {
        println(v)
    }
}

fun testInt() {
    val values = IntArray(2)
    values[0] = Int.MIN_VALUE
    values[1] = Int.MAX_VALUE
    for (v in values) {
        println(v)
    }
}

fun testLong() {
    val values = LongArray(2)
    values[0] = Long.MIN_VALUE
    values[1] = Long.MAX_VALUE
    for (v in values) {
        println(v)
    }
}

fun testFloat() {
    val values = FloatArray(5)
    values[0] = Float.MIN_VALUE
    values[1] = Float.MAX_VALUE
    values[2] = Float.NEGATIVE_INFINITY
    values[3] = Float.POSITIVE_INFINITY
    values[4] = Float.NaN
    for (v in values) {
        println(v)
    }
}

fun testDouble() {
    val values = DoubleArray(5)
    values[0] = Double.MIN_VALUE
    values[1] = Double.MAX_VALUE
    values[2] = Double.NEGATIVE_INFINITY
    values[3] = Double.POSITIVE_INFINITY
    values[4] = Double.NaN
    for (v in values) {
        println(v)
    }
}

@Test fun runTest() {
    testByte()
    testShort()
    testInt()
    testLong()
    testFloat()
    testDouble()

}
