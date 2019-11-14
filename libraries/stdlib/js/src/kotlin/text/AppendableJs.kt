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
     * Appends the specified character [c] to this Appendable and returns this instance.
     *
     * @param c the character to append.
     */
    public actual fun append(c: Char): Appendable

    /**
     * Appends the specified character sequence [csq] to this Appendable and returns this instance.
     *
     * @param csq the character sequence to append. If [csq] is `null`, then the four characters `"null"` are appended to this Appendable.
     */
    public actual fun append(csq: CharSequence?): Appendable

    /**
     * Appends a subsequence of the specified character sequence [csq] to this Appendable and returns this instance.
     *
     * @param csq the character sequence from which a subsequence is appended. If [csq] is `null`,
     *  then characters are appended as if [csq] contained the four characters `"null"`.
     * @param start the beginning (inclusive) of the subsequence to append.
     * @param end the end (exclusive) of the subsequence to append.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [start] or [end] is out of range of the [csq] character sequence indices or when `start > end`.
     */
    public actual fun append(csq: CharSequence?, start: Int, end: Int): Appendable
}