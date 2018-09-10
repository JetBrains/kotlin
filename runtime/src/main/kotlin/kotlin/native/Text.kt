/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native

/**
 * Converts an UTF-8 array into a [String]. Replaces invalid input sequences with a default character.
 */
public fun ByteArray.stringFromUtf8(start: Int = 0, size: Int = this.size) : String =
        stringFromUtf8Impl(start, size)

@SymbolName("Kotlin_ByteArray_stringFromUtf8")
private external fun ByteArray.stringFromUtf8Impl(start: Int, size: Int) : String

/**
 * Converts an UTF-8 array into a [String].
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
public fun ByteArray.stringFromUtf8OrThrow(start: Int = 0, size: Int = this.size) : String =
        stringFromUtf8OrThrowImpl(start, size)

@SymbolName("Kotlin_ByteArray_stringFromUtf8OrThrow")
private external fun ByteArray.stringFromUtf8OrThrowImpl(start: Int, size: Int) : String

/**
 * Converts a [String] into an UTF-8 array. Replaces invalid input sequences with a default character.
 */
public fun String.toUtf8(start: Int = 0, size: Int = this.length) : ByteArray =
        toUtf8Impl(start, size)

@SymbolName("Kotlin_String_toUtf8")
private external fun String.toUtf8Impl(start: Int, size: Int) : ByteArray

/**
 * Converts a [String] into an UTF-8 array.
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
public fun String.toUtf8OrThrow(start: Int = 0, size: Int = this.length) : ByteArray =
        toUtf8OrThrowImpl(start, size)

@SymbolName("Kotlin_String_toUtf8OrThrow")
private external fun String.toUtf8OrThrowImpl(start: Int, size: Int) : ByteArray

@SymbolName("Kotlin_String_fromCharArray")
internal external fun fromCharArray(array: CharArray, start: Int, size: Int) : String

@SymbolName("Kotlin_StringBuilder_insertString")
internal external fun insertString(array: CharArray, start: Int, value: String): Int

@SymbolName("Kotlin_StringBuilder_insertInt")
internal external fun insertInt(array: CharArray, start: Int, value: Int): Int