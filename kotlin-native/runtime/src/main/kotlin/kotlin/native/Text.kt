/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native


internal fun checkBoundsIndexes(startIndex: Int, endIndex: Int, size: Int) {
    if (startIndex < 0 || endIndex > size) {
        throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, size: $size")
    }
    if (startIndex > endIndex) {
        throw IllegalArgumentException("startIndex: $startIndex > endIndex: $endIndex")
    }
}

internal fun insertString(array: CharArray, start: Int, value: String): Int =
        insertString(array, start, value, 0, value.length)

@SymbolName("Kotlin_ByteArray_unsafeStringFromUtf8")
internal external fun ByteArray.unsafeStringFromUtf8(start: Int, size: Int) : String

@SymbolName("Kotlin_ByteArray_unsafeStringFromUtf8OrThrow")
internal external fun ByteArray.unsafeStringFromUtf8OrThrow(start: Int, size: Int) : String

@SymbolName("Kotlin_String_unsafeStringToUtf8")
internal external fun String.unsafeStringToUtf8(start: Int, size: Int) : ByteArray

@SymbolName("Kotlin_String_unsafeStringToUtf8OrThrow")
internal external fun String.unsafeStringToUtf8OrThrow(start: Int, size: Int) : ByteArray

@SymbolName("Kotlin_String_unsafeStringFromCharArray")
internal external fun unsafeStringFromCharArray(array: CharArray, start: Int, size: Int) : String

@SymbolName("Kotlin_StringBuilder_insertString")
internal external fun insertString(array: CharArray, distIndex: Int, value: String, sourceIndex: Int, count: Int): Int

@SymbolName("Kotlin_StringBuilder_insertInt")
internal external fun insertInt(array: CharArray, start: Int, value: Int): Int