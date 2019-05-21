/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native


/**
 * Converts an UTF-8 array into a [String]. Replaces invalid input sequences with a default character.
 */
@Deprecated("Use decodeToString instead", ReplaceWith("decodeToString()"))
public fun ByteArray.stringFromUtf8() : String {
    @Suppress("DEPRECATION")
    return this.stringFromUtf8(0, this.size)
}

/**
 * Converts an UTF-8 array into a [String]. Replaces invalid input sequences with a default character.
 */
@Deprecated("Use decodeToString instead", ReplaceWith("decodeToString(start, start + size)"))
public fun ByteArray.stringFromUtf8(start: Int = 0, size: Int = this.size) : String {
    checkBoundsIndexes(start, start + size, this.size)
    return stringFromUtf8Impl(start, size)
}

@SymbolName("Kotlin_ByteArray_stringFromUtf8")
internal external fun ByteArray.stringFromUtf8Impl(start: Int, size: Int) : String

/**
 * Converts an UTF-8 array into a [String].
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
@Deprecated("Use decodeToString instead", ReplaceWith("decodeToString(throwOnInvalidSequence = true)"))
public fun ByteArray.stringFromUtf8OrThrow() : String {
    @Suppress("DEPRECATION")
    return this.stringFromUtf8OrThrow(0, this.size)
}

/**
 * Converts an UTF-8 array into a [String].
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
@Deprecated("Use decodeToString instead", ReplaceWith("decodeToString(start, start + size, throwOnInvalidSequence = true)"))
@UseExperimental(ExperimentalStdlibApi::class)
public fun ByteArray.stringFromUtf8OrThrow(start: Int = 0, size: Int = this.size) : String {
    checkBoundsIndexes(start, start + size, this.size)
    try {
        return stringFromUtf8OrThrowImpl(start, size)
    } catch (e: CharacterCodingException) {
        @Suppress("DEPRECATION")
        throw IllegalCharacterConversionException()
    }
}

@SymbolName("Kotlin_ByteArray_stringFromUtf8OrThrow")
internal external fun ByteArray.stringFromUtf8OrThrowImpl(start: Int, size: Int) : String

/**
 * Converts a [String] into an UTF-8 array. Replaces invalid input sequences with a default character.
 */
@Deprecated("Use encodeToByteArray instead", ReplaceWith("encodeToByteArray()"))
public fun String.toUtf8() : ByteArray {
    @Suppress("DEPRECATION")
    return this.toUtf8(0, this.length)
}

/**
 * Converts a [String] into an UTF-8 array. Replaces invalid input sequences with a default character.
 */
@Deprecated("Use encodeToByteArray instead", ReplaceWith("encodeToByteArray(start, start + size)"))
public fun String.toUtf8(start: Int = 0, size: Int = this.length) : ByteArray {
    checkBoundsIndexes(start, start + size, this.length)
    return toUtf8Impl(start, size)
}

@SymbolName("Kotlin_String_toUtf8")
internal external fun String.toUtf8Impl(start: Int, size: Int) : ByteArray

/**
 * Converts a [String] into an UTF-8 array.
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
@Deprecated("Use encodeToByteArray instead", ReplaceWith("encodeToByteArray(throwOnInvalidSequence = true)"))
public fun String.toUtf8OrThrow() : ByteArray {
    @Suppress("DEPRECATION")
    return this.toUtf8OrThrow(0, this.length)
}

/**
 * Converts a [String] into an UTF-8 array.
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
@Deprecated("Use encodeToByteArray instead", ReplaceWith("encodeToByteArray(start, start + size, throwOnInvalidSequence = true)"))
@UseExperimental(ExperimentalStdlibApi::class)
public fun String.toUtf8OrThrow(start: Int = 0, size: Int = this.length) : ByteArray {
    checkBoundsIndexes(start, start + size, this.length)
    try {
        return toUtf8OrThrowImpl(start, size)
    } catch (e: CharacterCodingException) {
        @Suppress("DEPRECATION")
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

@SymbolName("Kotlin_String_toUtf8OrThrow")
internal external fun String.toUtf8OrThrowImpl(start: Int, size: Int) : ByteArray

@SymbolName("Kotlin_String_fromCharArray")
internal external fun fromCharArray(array: CharArray, start: Int, size: Int) : String

@SymbolName("Kotlin_StringBuilder_insertString")
internal external fun insertString(array: CharArray, start: Int, value: String): Int

@SymbolName("Kotlin_StringBuilder_insertInt")
internal external fun insertInt(array: CharArray, start: Int, value: Int): Int