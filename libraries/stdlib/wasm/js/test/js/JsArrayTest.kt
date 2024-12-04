/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.js

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

private fun <T : JsAny?> testJsRoundTrip(array: Array<T>) {
    val jsArray = array.toJsArray()
    for (i in array.indices) {
        assertEquals(array[i], jsArray[i])
    }
    assertEquals(array.size, jsArray.length)
    val roundTrippedArray = jsArray.toArray()
    assertContentEquals(array, roundTrippedArray)
}

private fun <T : JsAny?> testJsRoundTrip(array: List<T>) {
    val jsArray = array.toJsArray()
    for (i in array.indices) {
        assertEquals(array[i], jsArray[i])
    }
    assertEquals(array.size, jsArray.length)
    val roundTrippedList = jsArray.toList()
    assertEquals(array, roundTrippedList)
}

class JsArrayTest {
    @Test
    fun testConversionToArray() {
        testJsRoundTrip(arrayOf<JsNumber>())
        testJsRoundTrip(
            arrayOf(
                null,
                (-42).toJsNumber(),
                "123".toJsString(),
                (object {}).toJsReference(),
            )
        )
        testJsRoundTrip(Array<JsAny>(1000) { it.toJsNumber() })
    }

    @Test
    fun testConversionToList() {
        testJsRoundTrip(listOf<JsNumber>())
        testJsRoundTrip(
            listOf(
                null,
                (-42).toJsNumber(),
                "123".toJsString(),
                (object {}).toJsReference(),
            )
        )
        testJsRoundTrip(List<JsAny>(1000) { it.toJsNumber() })
    }
}