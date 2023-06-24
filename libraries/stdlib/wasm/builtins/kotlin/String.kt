/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*

/**
 * The `String` class represents character strings. All string literals in Kotlin programs, such as `"abc"`, are
 * implemented as instances of this class.
 */
public class String internal @WasmPrimitiveConstructor constructor(
    private var leftIfInSum: String?,
    @kotlin.internal.IntrinsicConstEvaluation
    public override val length: Int,
    internal var _chars: WasmCharArray,
) : Comparable<String>, CharSequence {
    public companion object {}

    /**
     * Returns a string obtained by concatenating this string with the string representation of the given [other] object.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public operator fun plus(other: Any?): String {
        val right = other.toString()
        if (right.isEmpty()) return this
        if (this.isEmpty()) return right
        return String(this, this.length + right.length, right.chars)
    }

    /**
     * Returns the character of this string at the specified [index].
     *
     * If the [index] is out of bounds of this string, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public override fun get(index: Int): Char {
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

    public override fun subSequence(startIndex: Int, endIndex: Int): String {
//        val actualStartIndex = if (startIndex < 0) 0 else startIndex
//        val length1 = this.length
//        val actualEndIndex = if (endIndex > length1) length1 else endIndex
//        val newLength = actualEndIndex - actualStartIndex
        if (startIndex < 0) error("start")
        if (endIndex > length) error("end")

        val newLength = endIndex - startIndex

        when (newLength) {
            0 -> return EMPTY_STRING
            1 -> {
                return String(null, 1, array_new_fixed1(chars.get(startIndex)))
            }
        }

        if (newLength < 0) error("")

        val newChars = WasmCharArray(newLength)
        copyWasmArray(chars, newChars, startIndex, 0, newLength)
        return String(null, newLength, newChars)
    }

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun compareTo(other: String): Int {
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


    @kotlin.internal.IntrinsicConstEvaluation
    public override fun equals(other: Any?): Boolean {
        if (other == null) return false //
        if (other === this) return true
        if (other !is String) return false //

        val otherString: String = other //

        val thisLength = this.length
        if (thisLength != otherString.length) return false

        val thisHash = this._hashCode
        if (thisHash != 0) {
            val otherHash = otherString._hashCode
            if (otherHash != 0 && thisHash != otherHash) return false
        }

        val thisChars = this.chars // _chars
        val otherChars = otherString.chars // _chars
        var index = 0
        while (index < thisLength) {
            if (thisChars.get(index) != otherChars.get(index)) return false
            index = index + 1
        }
        return true
    }

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun toString(): String = this

    public override fun hashCode(): Int {
        if (_hashCode != 0) return _hashCode
        val thisLength = this.length
        if (thisLength == 0) return 0

        val thisChars = chars
        var hash = 0
        repeat(thisLength) {
            hash = (hash shl 5) - hash + thisChars.get(it).toInt()
        }
        _hashCode = hash
        return _hashCode
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun WasmCharArray.createString(): String =
    String(null, this.len(), this)

internal fun stringLiteral(poolId: Int, startAddress: Int, length: Int): String {
    val cached = stringPool[poolId]
    if (cached !== null) {
        return cached
    }

    val chars = array_new_data0<WasmCharArray>(startAddress, length)
    val newString = String(null, length, chars)
    stringPool[poolId] = newString
    return newString
}

internal fun streqeq(this_: String, other: String): Boolean {
    if (other === this_) return true

    val thisChars = this_.chars
    val otherChars = other.chars

    if (otherChars === thisChars) return true

    val thisLength = thisChars.len()
    val otherLength = otherChars.len()

    if (thisLength != otherLength) return false

    var index = 0
    while (index < thisLength) {
        if (thisChars.get(index) != otherChars.get(index)) return false
        index = index + 1
    }
    return true
}
