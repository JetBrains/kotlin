/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.array0

import kotlin.test.*

@Test fun runTest() {
    // Create instances of all array types.
    val byteArray = ByteArray(5)
    println(byteArray.size.toString())

    val charArray = CharArray(6)
    println(charArray.size.toString())

    val shortArray = ShortArray(7)
    println(shortArray.size.toString())

    val intArray = IntArray(8)
    println(intArray.size.toString())

    val longArray = LongArray(9)
    println(longArray.size.toString())

    val floatArray = FloatArray(10)
    println(floatArray.size.toString())

    val doubleArray = FloatArray(11)
    println(doubleArray.size.toString())

    val booleanArray = BooleanArray(12)
    println(booleanArray.size.toString())

    val stringArray = Array<String>(13, { i -> ""})
    println(stringArray.size.toString())
}