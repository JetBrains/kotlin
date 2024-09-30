/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.wasm.internal.*

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Byte): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Short): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Int): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Long): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Float): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Double): StringBuilder = append(value).appendLine()

internal actual fun unsafeStringFromCharArray(array: CharArray, start: Int, size: Int): String {
    val copy = WasmCharArray(size)
    copyWasmArray(array.storage, copy, start, 0, size)
    return copy.createString()
}

internal actual fun unsafeStringCopy(string: String, length: Int): String {
    val copy = WasmCharArray(length)
    copyWasmArray(string.chars, copy, 0, 0, string.length)
    return copy.createString()
}

internal actual fun unsafeStringSetChar(string: String, index: Int, c: Char): String {
    string.chars.set(index, c)
    return string
}

internal actual fun unsafeStringSetArray(string: String, index: Int, value: CharArray, start: Int, end: Int): String {
    copyWasmArray(value.storage, string.chars, start, index, end - start)
    return string
}

internal actual fun unsafeStringSetString(string: String, index: Int, value: String, start: Int, end: Int): String {
    copyWasmArray(value.chars, string.chars, start, index, end - start)
    return string
}

internal actual fun unsafeStringSetInt(string: String, index: Int, value: Int): Int {
    val valueString = value.toString()
    unsafeStringSetString(string, index, valueString, 0, valueString.length)
    return valueString.length
}
