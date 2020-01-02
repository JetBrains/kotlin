/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 * An object to which char sequences and values can be appended.
 */
public actual interface Appendable {
    /**
     * Appends the specified character [value] to this Appendable and returns this instance.
     *
     * @param value the character to append.
     */
    public actual fun append(value: Char): Appendable

    /**
     * Appends the specified character sequence [value] to this Appendable and returns this instance.
     *
     * @param value the character sequence to append. If [value] is `null`, then the four characters `"null"` are appended to this Appendable.
     */
    public actual fun append(value: CharSequence?): Appendable

    /**
     * Appends a subsequence of the specified character sequence [value] to this Appendable and returns this instance.
     *
     * @param value the character sequence from which a subsequence is appended. If [value] is `null`,
     *  then characters are appended as if [value] contained the four characters `"null"`.
     * @param startIndex the beginning (inclusive) of the subsequence to append.
     * @param endIndex the end (exclusive) of the subsequence to append.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] character sequence indices or when `startIndex > endIndex`.
     */
    public actual fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Appendable
}