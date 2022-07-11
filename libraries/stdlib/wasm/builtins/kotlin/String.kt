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
public class String private constructor(
    private var leftIfInSum: String?,
    private var _chars: WasmCharArray,
) : Comparable<String>, CharSequence {
    public companion object {}

    internal constructor(chars: WasmCharArray) : this(null, chars) { }

    /**
     * Returns a string obtained by concatenating this string with the string representation of the given [other] object.
     */
    public operator fun plus(other: Any?): String {
        val right = if (other is String) other else other.toString()
        return String(this, right.chars)
    }

    public override val length: Int get() {
        var currentLeftString = leftIfInSum
        var currentLength = _chars.len()
        while (currentLeftString != null) {
            currentLength += currentLeftString._chars.len()
            currentLeftString = currentLeftString.leftIfInSum
        }
        return currentLength
    }

    /**
     * Returns the character of this string at the specified [index].
     *
     * If the [index] is out of bounds of this string, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    public override fun get(index: Int): Char {
        if (index < 0) throw IndexOutOfBoundsException()
        val folded = chars
        val length = folded.len()
        if (index >= length) throw IndexOutOfBoundsException()
        return folded.get(index)
    }

    internal fun foldChars() {
        val stringLength = this.length
        val newArray = WasmCharArray(stringLength)

        var currentStartIndex = stringLength
        var currentLeftString: String? = this
        while (currentLeftString != null) {
            val currentLeftStringChars = currentLeftString._chars
            val currentLeftStringLen = currentLeftStringChars.len()
            currentStartIndex -= currentLeftStringLen
            copyWasmArray(currentLeftStringChars, newArray, 0, currentStartIndex, currentLeftStringLen)
            currentLeftString = currentLeftString.leftIfInSum
        }
        check(currentStartIndex == 0)
        _chars = newArray
        leftIfInSum = null
    }

    internal inline val chars: WasmCharArray get() {
        if (leftIfInSum != null) {
            foldChars()
        }
        return _chars
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
        if (_hashCode != 0) return _hashCode
        val thisChars = chars
        val thisLength = thisChars.len()
        if (thisLength == 0) return 0

        var hash = 0
        var i = 0
        while (i < thisLength) {
            hash = 31 * hash + thisChars.get(i).toInt()
            i++
        }

        _hashCode = hash
        return _hashCode
    }
}

internal fun stringLiteral(startAddr: Int, length: Int): String {
    val array = WasmCharArray(length)
    unsafeRawMemoryToWasmCharArray(startAddr, 0, length, array)
    return String(array)
}
