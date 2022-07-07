/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*
import kotlin.math.min

/**
 * The `String` class represents character strings. All string literals in Kotlin programs, such as `"abc"`, are
 * implemented as instances of this class.
 */
public class String private constructor(internal val chars: WasmCharArray) : Comparable<String>, CharSequence {
    public companion object {
        // Note: doesn't copy the array, use with care.
        internal fun unsafeFromCharArray(chars: WasmCharArray) = String(chars)
    }

    /**
     * Returns a string obtained by concatenating this string with the string representation of the given [other] object.
     */
    public operator fun plus(other: Any?): String {
        val thisChars = chars
        val otherChars = if (other is String) other.chars else other.toString().chars
        val thisLen = thisChars.len()
        val otherLen = otherChars.len()
        if (otherLen == 0) return String(thisChars)

        val newChars = WasmCharArray(thisLen + otherLen)
        copyWasmArray(thisChars, newChars, 0, 0, thisLen)
        copyWasmArray(otherChars, newChars, 0, thisLen, otherLen)
        return String(newChars)
    }

    public override val length: Int
        get() = chars.len()

    /**
     * Returns the character of this string at the specified [index].
     *
     * If the [index] is out of bounds of this string, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    public override fun get(index: Int): Char {
        if (index < 0 || index >= chars.len()) throw IndexOutOfBoundsException()
        return chars.get(index)
    }

    public override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        val actualStartIndex = startIndex.coerceAtLeast(0)
        val thisChars = chars
        val actualEndIndex = endIndex.coerceAtMost(thisChars.len())
        val newCharsLen = actualEndIndex - actualStartIndex
        val newChars = WasmCharArray(newCharsLen)
        copyWasmArray(thisChars, newChars, actualStartIndex, 0, newCharsLen)
        return String(newChars)
    }

    public override fun compareTo(other: String): Int {
        val thisChars = this.chars
        val otherChars = other.chars

        val thisLength = thisChars.len()
        val otherLength = otherChars.len()
        val len = min(thisLength, otherLength)

        for (i in 0 until len) {
            val l = thisChars.get(i)
            val r = otherChars.get(i)
            if (l != r)
                return l - r
        }
        return thisLength - otherLength
    }

    public override fun equals(other: Any?): Boolean =
        other != null &&
        other is String &&
        (this.length == other.length) &&
        this.compareTo(other) == 0

    public override fun toString(): String = this

    public override fun hashCode(): Int {
        val thisLength = length

        if (_hashCode != 0 || thisLength == null)
            return _hashCode

        var hash = 0
        var i = 0
        while (i < thisLength) {
            hash = 31 * hash + chars.get(i).toInt()
            i++
        }
        _hashCode = hash
        return _hashCode
    }
}

internal fun stringLiteral(startAddr: Int, length: Int): String {
    val array = WasmCharArray(length)
    unsafeRawMemoryToWasmCharArray(startAddr, 0, length, array)
    return String.unsafeFromCharArray(array)
}
