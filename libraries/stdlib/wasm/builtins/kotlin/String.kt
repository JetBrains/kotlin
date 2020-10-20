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
@WasmPrimitive
public class String constructor(public val string: String) : Comparable<String>, CharSequence {
    public companion object;

    /**
     * Returns a string obtained by concatenating this string with the string representation of the given [other] object.
     */
    public operator fun plus(other: Any?): String =
        stringPlusImpl(this, other.toString())

    public override val length: Int
        get() = stringLengthImpl(this)

    /**
     * Returns the character of this string at the specified [index].
     *
     * If the [index] is out of bounds of this string, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    public override fun get(index: Int): Char =
        stringGetCharImpl(this, index)

    public override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        stringSubSequenceImpl(this, startIndex, endIndex)

    public override fun compareTo(other: String): Int =
        stringCompareToImpl(this, other)

    public override fun equals(other: Any?): Boolean {
        if (other is String)
            return this.compareTo(other) == 0
        return false
    }

    public override fun toString(): String = this

    // TODO: Implement
    public override fun hashCode(): Int = 10
}

@JsFun("(it, other) => it + String(other)")
private fun stringPlusImpl(it: String, other: String): String =
    implementedAsIntrinsic

@JsFun("(it) => it.length")
private fun stringLengthImpl(it: String): Int =
    implementedAsIntrinsic

@WasmImport("runtime", "String_getChar")
private fun stringGetCharImpl(it: String, index: Int): Char =
    implementedAsIntrinsic

@WasmImport("runtime", "String_compareTo")
private fun stringCompareToImpl(it: String, other: String): Int =
    implementedAsIntrinsic

@WasmImport("runtime", "String_subsequence")
private fun stringSubSequenceImpl(string: String, startIndex: Int, endIndex: Int): String =
    implementedAsIntrinsic
