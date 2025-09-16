/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("OPT_IN_USAGE")

package kotlin

import kotlin.wasm.internal.*

/**
 * The `String` class represents character strings. All string literals in Kotlin programs, such as `"abc"`, are
 * implemented as instances of this class.
 */

public actual class String internal @WasmPrimitiveConstructor constructor(
    public val internalStr: JsString,
    @kotlin.internal.IntrinsicConstEvaluation
    private var _chars: WasmCharArray?,
    @kotlin.internal.IntrinsicConstEvaluation
    public actual override val length: Int,
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
        return String(jsConcat(this.internalStr, right.internalStr).unsafeCast(), null, this.length + right.length)
    }

    @kotlin.internal.IntrinsicConstEvaluation
    public actual override fun get(index: Int): Char {
        rangeCheck(index, this.length)
        return jsCharCodeAt(this.internalStr, index).reinterpretAsChar()
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @kotlin.internal.IntrinsicConstEvaluation
    internal val chars: WasmCharArray
        get() {
            if (_chars == null) {
                val copy = WasmCharArray(length)
                jsIntoCharCodeArray(internalStr, copy, 0)
                _chars = copy
            }
            return _chars!!
        }

    public actual override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        checkStringBounds(startIndex, endIndex, length)
        return String(jsSubstring(this.internalStr, startIndex, endIndex).unsafeCast(), null, endIndex - startIndex)
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
        return jsCompare(this.internalStr, other.internalStr)
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

        return jsEquals(this.internalStr, other.internalStr) == 1
    }

    @kotlin.internal.IntrinsicConstEvaluation
    public actual override fun toString(): String = this

    public override fun hashCode(): Int {
        if (_hashCode != 0) return _hashCode
        val thisLength = this.length
        if (thisLength == 0) return 0

        var hash = 0
        repeat(thisLength) {
            hash = (hash shl 5) - hash + get(it).code
        }
        _hashCode = hash
        return _hashCode
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual fun WasmCharArray.createString(): String =
    String(jsFromCharCodeArray(this, 0, this.len()).unsafeCast(), this, this.len())

internal actual fun String.getChars() = this.chars
