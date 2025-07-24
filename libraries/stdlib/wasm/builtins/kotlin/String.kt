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
     * In Kotlin/Wasm, a [trap](https://webassembly.github.io/spec/core/intro/overview.html#trap)
     * will be raised if the [index] is out of bounds of this string,
     * unless `-Xwasm-enable-array-range-checks` compiler flag was specified when linking an executable.
     * With `-Xwasm-enable-array-range-checks` flag, [IndexOutOfBoundsException] will be thrown.
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

    internal val chars: WasmCharArray get() {
        if (leftIfInSum != null) {
            foldChars()
        }
        return _chars
    }

    public actual override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        checkStringBounds(startIndex, endIndex, length)
        val newLength = endIndex - startIndex
        val newChars = WasmCharArray(newLength)
        copyWasmArray(chars, newChars, startIndex, 0, newLength)
        return newChars.createString()
    }

    private fun checkStringBounds(startIndex: Int, endIndex: Int, length: Int) {
        if (startIndex < 0 || endIndex > length) {
            throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, length: $length")
        }
        if (startIndex > endIndex) {
            throw IndexOutOfBoundsException("startIndex: $startIndex > endIndex: $endIndex")
        }
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

@Suppress("NOTHING_TO_INLINE")
internal inline fun WasmCharArray.createString(): String =
    String(null, this.len(), this)

internal fun stringLiteralUtf16(poolId: Int): String {
    val cached = stringPool[poolId]
    if (cached !== null) {
        return cached
    }

    val addressAndLength = stringAddressesAndLengths.get(poolId)
    val length = (addressAndLength shr 32).toInt()
    val startAddress = (addressAndLength and ((1L shl 32) - 1L)).toInt()

    val chars = array_new_data0<WasmCharArray>(startAddress, length)
    val newString = String(null, length, chars)
    stringPool[poolId] = newString
    return newString
}

internal fun stringLiteralLatin1(poolId: Int): String {
    val cached = stringPool[poolId]
    if (cached !== null) {
        return cached
    }

    val addressAndLength = stringAddressesAndLengths.get(poolId)
    val length = (addressAndLength shr 32).toInt()
    val startAddress = (addressAndLength and ((1L shl 32) - 1L)).toInt()

    val bytes = array_new_data0<WasmByteArray>(startAddress, length)
    val chars = WasmCharArray(length)
    for (i in 0..<length) {
        val chr = bytes.get(i).toInt().toChar()
        chars.set(i, chr)
    }

    val newString = String(null, length, chars)
    stringPool[poolId] = newString
    return newString
}

// TODO: remove after bootstrap
internal fun stringLiteral(poolId: Int, start: Int, length: Int): String {
    val cached = stringPool[poolId]
    if (cached !== null) {
        return cached
    }

    val chars = array_new_data0<WasmCharArray>(start, length)
    val newString = String(null, length, chars)
    stringPool[poolId] = newString
    return newString
}