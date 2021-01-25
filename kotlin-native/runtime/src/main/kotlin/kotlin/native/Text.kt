/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native

import kotlinx.cinterop.toKString
import kotlin.native.internal.GCCritical

/**
 * Converts an UTF-8 array into a [String]. Replaces invalid input sequences with a default character.
 */
@Deprecated(
        "Use toKString or decodeToString instead",
        ReplaceWith("toKString()", "kotlinx.cinterop.toKString"),
        DeprecationLevel.ERROR
)
public fun ByteArray.stringFromUtf8() : String {
    @Suppress("DEPRECATION_ERROR")
    return this.stringFromUtf8(0, this.size)
}

/**
 * Converts an UTF-8 array into a [String]. Replaces invalid input sequences with a default character.
 */
@Deprecated(
        "Use toKString or decodeToString instead",
        ReplaceWith("toKString(start, start + size)", "kotlinx.cinterop.toKString"),
        DeprecationLevel.ERROR
)
public fun ByteArray.stringFromUtf8(start: Int = 0, size: Int = this.size) : String {
    return toKString(start, start + size)
}

/**
 * Converts an UTF-8 array into a [String].
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
@Deprecated(
        "Use toKString or decodeToString instead",
        ReplaceWith("toKString(throwOnInvalidSequence = true)", "kotlinx.cinterop.toKString"),
        DeprecationLevel.ERROR
)
public fun ByteArray.stringFromUtf8OrThrow() : String {
    @Suppress("DEPRECATION_ERROR")
    return this.stringFromUtf8OrThrow(0, this.size)
}

/**
 * Converts an UTF-8 array into a [String].
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
@Deprecated(
        "Use toKString or decodeToString instead",
        ReplaceWith("toKString(start, start + size, throwOnInvalidSequence = true)", "kotlinx.cinterop.toKString"),
        DeprecationLevel.ERROR
)
public fun ByteArray.stringFromUtf8OrThrow(start: Int = 0, size: Int = this.size) : String {
    try {
        return toKString(start, start + size, throwOnInvalidSequence = true)
    } catch (e: CharacterCodingException) {
        @Suppress("DEPRECATION_ERROR")
        throw IllegalCharacterConversionException()
    }
}

/**
 * Converts a [String] into an UTF-8 array. Replaces invalid input sequences with a default character.
 */
@Deprecated(
        "Use encodeToByteArray instead",
        ReplaceWith("encodeToByteArray()"),
        DeprecationLevel.ERROR
)
public fun String.toUtf8() : ByteArray {
    @Suppress("DEPRECATION_ERROR")
    return this.toUtf8(0, this.length)
}

/**
 * Converts a [String] into an UTF-8 array. Replaces invalid input sequences with a default character.
 */
@Deprecated(
        "Use encodeToByteArray instead",
        ReplaceWith("encodeToByteArray(start, start + size)"),
        DeprecationLevel.ERROR
)
public fun String.toUtf8(start: Int = 0, size: Int = this.length) : ByteArray {
    checkBoundsIndexes(start, start + size, this.length)
    return unsafeStringToUtf8(start, size)
}

/**
 * Converts a [String] into an UTF-8 array.
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
@Deprecated(
        "Use encodeToByteArray instead",
        ReplaceWith("encodeToByteArray(throwOnInvalidSequence = true)"),
        DeprecationLevel.ERROR
)
public fun String.toUtf8OrThrow() : ByteArray {
    @Suppress("DEPRECATION_ERROR")
    return this.toUtf8OrThrow(0, this.length)
}

/**
 * Converts a [String] into an UTF-8 array.
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
@Deprecated(
        "Use encodeToByteArray instead",
        ReplaceWith("encodeToByteArray(start, start + size, throwOnInvalidSequence = true)"),
        DeprecationLevel.ERROR
)
public fun String.toUtf8OrThrow(start: Int = 0, size: Int = this.length) : ByteArray {
    checkBoundsIndexes(start, start + size, this.length)
    try {
        return unsafeStringToUtf8OrThrow(start, size)
    } catch (e: CharacterCodingException) {
        @Suppress("DEPRECATION_ERROR")
        throw IllegalCharacterConversionException()
    }
}

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
@GCCritical
internal external fun ByteArray.unsafeStringFromUtf8(start: Int, size: Int) : String

@SymbolName("Kotlin_ByteArray_unsafeStringFromUtf8OrThrow")
@GCCritical
internal external fun ByteArray.unsafeStringFromUtf8OrThrow(start: Int, size: Int) : String

@SymbolName("Kotlin_String_unsafeStringToUtf8")
@GCCritical
internal external fun String.unsafeStringToUtf8(start: Int, size: Int) : ByteArray

@SymbolName("Kotlin_String_unsafeStringToUtf8OrThrow")
@GCCritical
internal external fun String.unsafeStringToUtf8OrThrow(start: Int, size: Int) : ByteArray

@SymbolName("Kotlin_String_unsafeStringFromCharArray")
@GCCritical
internal external fun unsafeStringFromCharArray(array: CharArray, start: Int, size: Int) : String

@SymbolName("Kotlin_StringBuilder_insertString")
@GCCritical
internal external fun insertString(array: CharArray, distIndex: Int, value: String, sourceIndex: Int, count: Int): Int

@SymbolName("Kotlin_StringBuilder_insertInt")
@GCCritical
internal external fun insertInt(array: CharArray, start: Int, value: Int): Int