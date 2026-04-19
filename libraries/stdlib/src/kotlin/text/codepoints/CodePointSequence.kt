/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.codepoints

@ExperimentalCodePointApi
public interface CodePointSequence : Sequence<CodePoint> {
    override fun iterator(): CodePointIterator
}

@ExperimentalCodePointApi
public interface CodePointIterator : Iterator<CodePoint> {

}


@ExperimentalCodePointApi
public expect fun String.codePointAt(index: Int): CodePoint

@ExperimentalCodePointApi
public expect fun String.codePointBefore(index: Int): CodePoint

@ExperimentalCodePointApi
public expect fun CharSequence.codePointAt(index: Int): CodePoint

@ExperimentalCodePointApi
public expect fun CharSequence.codePointBefore(index: Int): CodePoint

@ExperimentalCodePointApi
public expect fun CharArray.codePointAt(index: Int): CodePoint

@ExperimentalCodePointApi
public expect fun CharArray.codePointBefore(index: Int): CodePoint


@ExperimentalCodePointApi
public fun String.codePointSequence(): CodePointSequence =
    object : CodePointSequence {
        override fun iterator(): CodePointIterator = object : CodePointIterator {
            var index = 0
            override fun next(): CodePoint {
                if (index >= length) throw NoSuchElementException()
                val c = this@codePointSequence.codePointAt(index)
                index += c.size
                return c
            }

            override fun hasNext(): Boolean {
                return index < this@codePointSequence.length
            }
        }
    }

@ExperimentalCodePointApi
public fun CharSequence.codePointSequence(): CodePointSequence =
    object : CodePointSequence {
        override fun iterator(): CodePointIterator = object : CodePointIterator {
            var index = 0
            override fun next(): CodePoint {
                if (index >= length) throw NoSuchElementException()
                val c = this@codePointSequence.codePointAt(index)
                index += c.size
                return c
            }

            override fun hasNext(): Boolean {
                return index < this@codePointSequence.length
            }
        }
    }

@ExperimentalCodePointApi
public fun CharArray.codePointSequence(): CodePointSequence =
    object : CodePointSequence {
        override fun iterator(): CodePointIterator = object : CodePointIterator {
            var index = 0
            override fun next(): CodePoint {
                if (index >= size) throw NoSuchElementException()
                val c = this@codePointSequence.codePointAt(index)
                index += c.size
                return c
            }

            override fun hasNext(): Boolean {
                return index < this@codePointSequence.size
            }
        }
    }

