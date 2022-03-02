/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.wasm.internal.*

internal fun insertString(array: CharArray, distIndex: Int, value: String, sourceIndex: Int, count: Int): Int {
    var arrayIdx = distIndex
    var stringIdx = sourceIndex
    repeat(count) {
        array[arrayIdx++] = value[stringIdx++]
    }
    return count
}

internal fun unsafeStringFromCharArray(array: CharArray, start: Int, size: Int): String {
    val copy = WasmCharArray(size)
    copy.fill(size) { array[it + start] }
    return String.unsafeFromCharArray(copy)
}

internal fun insertInt(array: CharArray, start: Int, value: Int): Int {
    val valueString = value.toString()
    val length = valueString.length
    insertString(array, start, valueString, 0, length)
    return length
}

internal fun checkBoundsIndexes(startIndex: Int, endIndex: Int, size: Int) {
    if (startIndex < 0 || endIndex > size) {
        throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, size: $size")
    }
    if (startIndex > endIndex) {
        throw IllegalArgumentException("startIndex: $startIndex > endIndex: $endIndex")
    }
}
