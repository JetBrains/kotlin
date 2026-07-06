/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.codepoints

@ExperimentalCodePointApi
public expect fun String.codePointAt(index: Int): CodePoint

@ExperimentalCodePointApi
public expect fun CharSequence.codePointAt(index: Int): CodePoint

@ExperimentalCodePointApi
public expect fun CharArray.codePointAt(index: Int): CodePoint

@ExperimentalCodePointApi
public expect fun CharArray.codePointAt(index: Int, endIndex: Int): CodePoint

@ExperimentalCodePointApi
public expect fun String.codePointBefore(index: Int): CodePoint

@ExperimentalCodePointApi
public expect fun CharSequence.codePointBefore(index: Int): CodePoint

@ExperimentalCodePointApi
public expect fun CharArray.codePointBefore(index: Int): CodePoint

@ExperimentalCodePointApi
public expect fun CharArray.codePointBefore(index: Int, startIndex: Int): CodePoint

@ExperimentalCodePointApi
public expect fun String.codePointCount(startIndex: Int = 0, endIndex: Int = this.length): Int

@ExperimentalCodePointApi
public expect fun CharSequence.codePointCount(startIndex: Int = 0, endIndex: Int = this.length): Int

@ExperimentalCodePointApi
public expect fun CharArray.codePointCount(startIndex: Int = 0, endIndex: Int = this.size): Int

@ExperimentalCodePointApi
public expect fun String.offsetByCodePoints(index: Int, codePointOffset: Int): Int

@ExperimentalCodePointApi
public expect fun CharSequence.offsetByCodePoints(index: Int, codePointOffset: Int): Int

@ExperimentalCodePointApi
public expect fun CharArray.offsetByCodePoints(index: Int, codePointOffset: Int, startIndex: Int = 0, endIndex: Int = this.size): Int

@ExperimentalCodePointApi
public inline fun String.forEachCodePoint(action: (CodePoint) -> Unit) {
    forEachCodePointImpl(0, length, this::codePointAt, action)
}

@ExperimentalCodePointApi
public inline fun CharSequence.forEachCodePoint(action: (CodePoint) -> Unit) {
    forEachCodePointImpl(0, length, this::codePointAt, action)
}

@ExperimentalCodePointApi
public inline fun CharArray.forEachCodePoint(startIndex: Int = 0, endIndex: Int = size, action: (CodePoint) -> Unit) {
    if (startIndex < 0 || endIndex > size || startIndex > endIndex) throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, size: $size")
    forEachCodePointImpl(startIndex, endIndex, { index -> codePointAt(index, endIndex) }, action)
}

@ExperimentalCodePointApi
@PublishedApi
internal inline fun forEachCodePointImpl(startIndex: Int, endIndex: Int, codePointAt: (Int) -> CodePoint, action: (CodePoint) -> Unit) {
    var index = startIndex
    while (index < endIndex) {
        val c = codePointAt(index)
        action(c)
        index += c.size
    }
}


@ExperimentalCodePointApi
public interface CodePointSequence : Sequence<CodePoint> {
    override fun iterator(): CodePointIterator
}

@ExperimentalCodePointApi
public interface CodePointIterator : Iterator<CodePoint> {
}

// TODO: Name TBD
@ExperimentalCodePointApi
public interface CodePointIndexedIterator : CodePointIterator {
    public override fun hasNext(): Boolean
    public fun hasPrevious(): Boolean
    public override fun next(): CodePoint
    public fun previous(): CodePoint

    // nextIndex and previousIndex return the char offset of
    // the next/previous code point
    public fun nextIndex(): Int
    public fun previousIndex(): Int

    public fun advanceByCodePoints(codePointOffset: Int)
}

@ExperimentalCodePointApi
private abstract class AbstractCodePointIterator(startIndex: Int, val endIndex: Int) : CodePointIterator {
    var index = startIndex

    abstract fun codePointAt(index: Int): CodePoint

    override fun hasNext(): Boolean = index < endIndex

    override fun next(): CodePoint {
        if (!hasNext()) throw NoSuchElementException()
        val c = codePointAt(index)
        index += c.size
        return c
    }
}

@ExperimentalCodePointApi
public fun String.codePointSequence(): CodePointSequence =
    object : CodePointSequence {
        override fun iterator(): CodePointIterator = object : AbstractCodePointIterator(0, length) {
            override fun codePointAt(index: Int): CodePoint = this@codePointSequence.codePointAt(index)
        }
    }

@ExperimentalCodePointApi
public fun CharSequence.codePointSequence(): CodePointSequence =
    object : CodePointSequence {
        override fun iterator(): CodePointIterator = object : AbstractCodePointIterator(0, length) {
            override fun codePointAt(index: Int): CodePoint = this@codePointSequence.codePointAt(index)
        }
    }

@ExperimentalCodePointApi
public fun CharArray.codePointSequence(startIndex: Int = 0, endIndex: Int = size): CodePointSequence {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, size)
    return object : CodePointSequence {
        override fun iterator(): CodePointIterator = object : AbstractCodePointIterator(startIndex, endIndex) {
            override fun codePointAt(index: Int): CodePoint = this@codePointSequence.codePointAt(index, this.endIndex)
        }
    }
}


@ExperimentalCodePointApi
private abstract class AbstractCodePointIndexedIterator(initialIndex: Int, val startIndex: Int, val endIndex: Int) : CodePointIndexedIterator {
    companion object {
        private const val PREVIOUS_NOT_COMPUTED: Int = -2
    }
    var _index = initialIndex
    var _previousIndex = PREVIOUS_NOT_COMPUTED

    abstract fun codePointAt(index: Int): CodePoint
    abstract fun codePointBefore(index: Int): CodePoint
    abstract fun offsetByCodePoints(index: Int, codePointOffset: Int): Int

    override fun hasNext(): Boolean = _index < endIndex
    override fun hasPrevious(): Boolean = _index > startIndex

    override fun next(): CodePoint {
        if (!hasNext()) throw NoSuchElementException()
        val c = codePointAt(_index)
        _previousIndex = _index
        _index += c.size
        return c
    }

    override fun previous(): CodePoint {
        if (!hasPrevious()) throw NoSuchElementException()
        val c = codePointBefore(_index)
        _previousIndex = PREVIOUS_NOT_COMPUTED
        _index -= c.size
        return c
    }

    override fun nextIndex(): Int {
        return _index
    }

    override fun previousIndex(): Int {
        if (_previousIndex == PREVIOUS_NOT_COMPUTED) {
            _previousIndex = if (_index > startIndex) _index - codePointBefore(_index).size else -1
        }
        return _previousIndex
    }

    override fun advanceByCodePoints(codePointOffset: Int) {
        _index = offsetByCodePoints(_index, codePointOffset)
        _previousIndex = PREVIOUS_NOT_COMPUTED
    }

}

@ExperimentalCodePointApi
public fun String.codePointIterator(index: Int = 0): CodePointIndexedIterator {
    AbstractList.checkPositionIndex(index, length)
    return object : AbstractCodePointIndexedIterator(index, 0, length) {
        override fun codePointAt(index: Int): CodePoint = this@codePointIterator.codePointAt(index)
        override fun codePointBefore(index: Int): CodePoint = this@codePointIterator.codePointBefore(index)
        override fun offsetByCodePoints(index: Int, codePointOffset: Int): Int = this@codePointIterator.offsetByCodePoints(index, codePointOffset)
    }
}

@ExperimentalCodePointApi
public fun CharSequence.codePointIterator(index: Int = 0): CodePointIndexedIterator {
    AbstractList.checkPositionIndex(index, length)
    return object : AbstractCodePointIndexedIterator(index, 0, length) {
        override fun codePointAt(index: Int): CodePoint = this@codePointIterator.codePointAt(index)
        override fun codePointBefore(index: Int): CodePoint = this@codePointIterator.codePointBefore(index)
        override fun offsetByCodePoints(index: Int, codePointOffset: Int): Int = this@codePointIterator.offsetByCodePoints(index, codePointOffset)
    }
}

//@ExperimentalCodePointApi
//public fun CharArray.codePointIterator(startIndex: Int = 0, endIndex: Int = size): CodePointIndexedIterator =
//    codePointIterator(startIndex, startIndex, endIndex)

@ExperimentalCodePointApi
public fun CharArray.codePointIterator(index: Int = 0, startIndex: Int = 0, endIndex: Int = size): CodePointIndexedIterator {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, size)
    if (index !in startIndex..endIndex) throw IndexOutOfBoundsException("index: $index, startIndex: $startIndex, endIndex: $endIndex")

    return object : AbstractCodePointIndexedIterator(index, startIndex, endIndex) {
        override fun codePointAt(index: Int): CodePoint = this@codePointIterator.codePointAt(index, this.endIndex)
        override fun codePointBefore(index: Int): CodePoint = this@codePointIterator.codePointBefore(index, this.startIndex)
        override fun offsetByCodePoints(index: Int, codePointOffset: Int): Int =
            this@codePointIterator.offsetByCodePoints(index, codePointOffset, this.startIndex, this.endIndex)
    }
}
