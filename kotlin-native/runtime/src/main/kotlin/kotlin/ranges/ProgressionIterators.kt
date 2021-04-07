/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.ranges

/**
 * An iterator over a progression of values of type `Char`.
 * @property step the number by which the value is incremented on each step.
 */
internal class CharProgressionIterator(first: Char, last: Char, val step: Int) : CharIterator() {
    private val finalElement: Int = last.code
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
    private var next: Int = if (hasNext) first.code else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun nextChar(): Char {
        val value = next
        if (value == finalElement) {
            if (!hasNext) throw kotlin.NoSuchElementException()
            hasNext = false
        }
        else {
            next += step
        }
        return value.toChar()
    }
}

/**
 * An iterator over a progression of values of type `Int`.
 * @property step the number by which the value is incremented on each step.
 */
internal class IntProgressionIterator(first: Int, last: Int, val step: Int) : IntIterator() {
    private val finalElement: Int = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
    private var next: Int = if (hasNext) first else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun nextInt(): Int {
        val value = next
        if (value == finalElement) {
            if (!hasNext) throw kotlin.NoSuchElementException()
            hasNext = false
        }
        else {
            next += step
        }
        return value
    }
}

/**
 * An iterator over a progression of values of type `Long`.
 * @property step the number by which the value is incremented on each step.
 */
internal class LongProgressionIterator(first: Long, last: Long, val step: Long) : LongIterator() {
    private val finalElement: Long = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
    private var next: Long = if (hasNext) first else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun nextLong(): Long {
        val value = next
        if (value == finalElement) {
            if (!hasNext) throw kotlin.NoSuchElementException()
            hasNext = false
        }
        else {
            next += step
        }
        return value
    }
}

