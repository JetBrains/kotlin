/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*
import kotlin.math.min

/**
 * The `String` class represents character strings. All string literals in Kotlin programs, such as `"abc"`, are
 * implemented as instances of this class.
 */
public actual class String internal @WasmPrimitiveConstructor constructor(
    private var leftIfInSum: String?,
    @kotlin.internal.IntrinsicConstEvaluation
    public actual override val length: Int,
    private var _chars: WasmCharArray,
) : Comparable<String>, CharSequence {
    public actual companion object {}

    /**
     * Returns a string obtained by concatenating this string with the string representation of the given [other] object.
     *
     * @sample samples.text.Strings.stringPlus
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public actual operator fun plus(other: Any?): String {
        val right = other.toString()
        return String(this, this.length + right.length, right.chars)
    }

    /**
     * Returns the character of this string at the specified [index].
     *
     * If the [index] is out of bounds of this string, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public actual override fun get(index: Int): Char {
        rangeCheck(index, this.length)
        return chars.get(index)
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

    public actual override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        val actualStartIndex = startIndex.coerceAtLeast(0)
        val actualEndIndex = endIndex.coerceAtMost(this.length)
        val newLength = actualEndIndex - actualStartIndex
        if (newLength <= 0) return ""
        val newChars = WasmCharArray(newLength)
        copyWasmArray(chars, newChars, actualStartIndex, 0, newLength)
        return newChars.createString()
    }

    @kotlin.internal.IntrinsicConstEvaluation
    public actual override fun compareTo(other: String): Int {
        if (this === other) return 0
        val thisChars = this.chars
        val otherChars = other.chars
        val thisLength = thisChars.len()
        val otherLength = otherChars.len()
        val minimumLength = if (thisLength < otherLength) thisLength else otherLength

        repeat(minimumLength) {
            val l = thisChars.get(it)
            val r = otherChars.get(it)
            if (l != r) return l - r
        }
        return thisLength - otherLength
    }

    /**
     * Indicates if [other] object is equal to this [String].
     *
     * An [other] object is equal to this [String] if and only if it is also a [String],
     * it has the same [length] as this String,
     * and characters at the same positions in each string are equal to each other.
     *
     * @sample samples.text.Strings.stringEquals
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public actual override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        val otherString = other as? String ?: return false

        val thisLength = this.length
        val otherLength = otherString.length
        if (thisLength != otherLength) return false

        val thisHash = this._hashCode
        val otherHash = other._hashCode
        if (thisHash != otherHash && thisHash != 0 && otherHash != 0) return false

        val thisChars = this.chars
        val otherChars = other.chars
        repeat(thisLength) {
            if (thisChars.get(it) != otherChars.get(it)) return false
        }
        return true
    }

    @kotlin.internal.IntrinsicConstEvaluation
    public actual override fun toString(): String = this

    public override fun hashCode(): Int {
        if (_hashCode != 0) return _hashCode
        val thisLength = this.length
        if (thisLength == 0) return 0

        val thisChars = chars
        var hash = 0
        repeat(thisLength) {
            hash = (hash shl 5) - hash + thisChars.get(it).code
        }
        _hashCode = hash
        return _hashCode
    }
}

internal fun WasmCharArray.createString(): String =
    String(null, this.len(), this)

internal fun stringLiteral(poolId: Int, startAddress: Int, length: Int): String {
    if (poolId == -1) {
        return ""
    }

    val cached = stringPool[poolId]
    if (cached !== null) {
        return cached
    }

    val chars = array_new_data0<WasmCharArray>(startAddress, length)
    val newString = String(null, length, chars)
    stringPool[poolId] = newString
    return newString
}
