/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.wasm.internal.*

internal fun insertString(array: CharArray, destinationIndex: Int, value: String, sourceIndex: Int, count: Int): Int {
    copyWasmArray(value.chars, array.storage, sourceIndex, destinationIndex, count)
    return count
}

internal fun unsafeStringFromCharArray(array: CharArray, start: Int, size: Int): String {
    val copy = WasmCharArray(size)
    copyWasmArray(array.storage, copy, start, 0, size)
    return copy.createString()
}

internal fun insertInt(array: CharArray, start: Int, value: Int): Int {
    val valueString = value.toString()
    val length = valueString.length
    insertString(array, start, valueString, 0, length)
    return length
}
