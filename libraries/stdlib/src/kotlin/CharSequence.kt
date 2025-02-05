/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.ActualizeByJvmBuiltinProvider

/**
 * Represents a readable sequence of [Char] values.
 */
@ActualizeByJvmBuiltinProvider
public expect interface CharSequence {
    /**
     * Returns the length of this character sequence.
     *
     * The length is measured in the number of characters constituting the sequence.
     * As [Char]s are UTF-16 encoded, some Unicode characters could be represented as a surrogate pair,
     * meaning that the length of the char sequence may not correspond to the number of printed characters.
     *
     * @sample samples.text.CharSequences.charSequenceLength
     */
    public val length: Int

    /**
     * Returns the character at the specified [index] in this character sequence.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this character sequence.
     *
     * Note that the [String] implementation of this interface in Kotlin/JS has unspecified behavior
     * if the [index] is out of its bounds.
     */
    public operator fun get(index: Int): Char

    /**
     * Returns a new character sequence that is a subsequence of this character sequence,
     * starting at the specified [startIndex] and ending right before the specified [endIndex].
     *
     * @param startIndex the start index (inclusive).
     * @param endIndex the end index (exclusive).
     */
    public fun subSequence(startIndex: Int, endIndex: Int): CharSequence
}
