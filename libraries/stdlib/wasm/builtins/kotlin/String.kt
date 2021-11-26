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
public class String private constructor(internal val chars: CharArray) : Comparable<String>, CharSequence {
    public companion object {
        // Note: doesn't copy the array, use with care.
        internal fun unsafeFromCharArray(chars: CharArray) = String(chars)
    }

    /**
     * Returns a string obtained by concatenating this string with the string representation of the given [other] object.
     */
    public operator fun plus(other: Any?): String =
        String(chars + other.toString().chars)

    public override val length: Int
        get() = chars.size

    /**
     * Returns the character of this string at the specified [index].
     *
     * If the [index] is out of bounds of this string, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    public override fun get(index: Int): Char = chars[index]

    public override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return String(chars.sliceArray(startIndex until endIndex))
    }

    public override fun compareTo(other: String): Int {
        val len = min(this.length, other.length)

        for (i in 0 until len) {
            val l = this[i]
            val r = other[i]
            if (l != r)
                return l - r
        }
        return this.length - other.length
    }

    public override fun equals(other: Any?): Boolean {
        if (other is String)
            return this.compareTo(other) == 0
        return false
    }

    public override fun toString(): String = this

    public override fun hashCode(): Int {
        if (_hashCode != 0 || this.isEmpty())
            return _hashCode
        var hash = 0
        for (c in chars)
            hash = 31 * hash + c.toInt()
        _hashCode = hash
        return _hashCode
    }
}

internal fun stringLiteral(startAddr: Int, length: Int) = String.unsafeFromCharArray(unsafeRawMemoryToCharArray(startAddr, length))
