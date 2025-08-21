/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Represents a readable sequence of [Char] values.
 */
public actual interface CharSequence {
    /**
     * Returns the length of this character sequence.
     *
     * The length is measured in the number of [Char]s constituting the sequence.
     * It implies that the length may not correspond to the number of printed graphemes:
     * some [Char]s could represent control, non-printable, or diaeresis symbols, others could form UTF-16 surrogate pairs,
     * required to encode Unicode code points not representable by a single [Char].
     *
     * @sample samples.text.CharSequences.charSequenceLength
     */
    public actual val length: Int

    /**
     * Returns the character at the specified [index] in this character sequence.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this character sequence.
     */
    public actual operator fun get(index: Int): Char

    /**
     * Returns a new character sequence that is a subsequence of this character sequence,
     * starting at the specified [startIndex] and ending right before the specified [endIndex].
     *
     * @param startIndex the start index (inclusive).
     * @param endIndex the end index (exclusive).
     */
    public actual fun subSequence(startIndex: Int, endIndex: Int): CharSequence
}
