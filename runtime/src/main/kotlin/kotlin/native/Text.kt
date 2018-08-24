/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Converts an UTF-8 array into a [String]. Throws [IllegalCharacterConversionException] if the input is invalid.
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
 * Converts a [String] into an UTF-8 array. Throws [IllegalCharacterConversionException] if the input is invalid.
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