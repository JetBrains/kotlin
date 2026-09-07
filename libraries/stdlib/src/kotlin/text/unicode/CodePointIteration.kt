/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.unicode

/**
 * Returns the Unicode code point at the specified [index] of this string.
 *
 * The [index] refers to the char-based position in the string, and the returned value is the code point
 * represented by the character or surrogate pair (a high surrogate char followed by a low surrogate char)
 * at that position.
 *
 * If the [index] points to an unpaired surrogate character, for example, in the middle of a surrogate pair,
 * the code point with the code of that only one character is returned.
 *
 * @param index the char-based position in the string for which the Unicode code point is to be returned, must be in range `0..<length`.
 * @return the Unicode code point as a [CodePoint] corresponding to the character or surrogate pair at the given [index].
 * @throws IndexOutOfBoundsException if the [index] is out of range.
 */
@ExperimentalCodePointApi
public expect fun String.codePointAt(index: Int): CodePoint

/**
 * Returns the Unicode code point at the specified [index] of this char sequence.
 *
 * The [index] refers to the char-based position in the char sequence, and the returned value is the code point
 * represented by the character or surrogate pair (a high surrogate char followed by a low surrogate char)
 * at that position.
 *
 * If the [index] points to an unpaired surrogate character, for example, in the middle of a surrogate pair,
 * the code point with the code of that only one character is returned.
 *
 * @param index the char-based position in the char sequence for which the Unicode code point is to be returned, must be in range `0..<length`.
 * @return the Unicode code point as a [CodePoint] corresponding to the character or surrogate pair at the given [index].
 * @throws IndexOutOfBoundsException if the [index] is out of range.
 */
@ExperimentalCodePointApi
public expect fun CharSequence.codePointAt(index: Int): CodePoint

/**
 * Returns the Unicode code point at the specified [index] of this char array.
 *
 * The [index] refers to the char-based position in the char array, and the returned value is the code point
 * represented by the character or surrogate pair (a high surrogate char followed by a low surrogate char)
 * at that index.
 *
 * If the [index] points to an unpaired surrogate character, for example, in the middle of a surrogate pair,
 * the code point with the code of that only one character is returned.
 *
 * @param index the char-based position in the char array for which the Unicode code point is to be returned, must be in range `0..<size`.
 * @return the Unicode code point as a [CodePoint] corresponding to the character or surrogate pair at the given [index].
 * @throws IndexOutOfBoundsException if the [index] is out of range.
 */
@ExperimentalCodePointApi
public expect fun CharArray.codePointAt(index: Int): CodePoint

/**
 * Returns the Unicode code point at the specified [index] of this char array.
 *
 * The [index] refers to the char-based position in the char array, and the returned value is the code point
 * represented by the character or surrogate pair (a high surrogate char followed by a low surrogate char)
 * at that index.
 *
 * If the [index] points to an unpaired surrogate character, for example, in the middle of a surrogate pair,
 * the code point with the code of that only one character is returned.
 *
 * The [endIndex] limits the end of a subrange in the char array, so that only characters at indices less than [endIndex] are considered.
 *
 * @param index the char-based position in the char array for which the Unicode code point is to be returned, must be in range `0..<endIndex`.
 * @param endIndex the char-based position in the char array that limits the end of a subrange in the char array, must be in range `0..size`.
 * @return the Unicode code point as a [CodePoint] corresponding to the character or surrogate pair at the given [index].
 * @throws IndexOutOfBoundsException if the [index] or [endIndex] are out of range.
 */
@ExperimentalCodePointApi
public expect fun CharArray.codePointAt(index: Int, endIndex: Int): CodePoint

/**
 * Returns the Unicode code point preceding the specified [index] in this string.
 *
 * The [index] refers to the char-based position in the string, and the returned value is the code point
 * represented by the character or surrogate pair (a high surrogate char followed by a low surrogate char)
 * immediately preceding that position.
 *
 * If the `index-1` position points to an unpaired surrogate character, for example,
 * a low surrogate char not preceded by a high surrogate char,
 * the code point with the code of that only one character is returned.
 *
 * @param index the char-based position in the string following the Unicode code point that is to be returned, must be in range `1..length`.
 * @return the Unicode code point value preceding the specified [index].
 * @throws IndexOutOfBoundsException if [index] is out of range.
 */
@ExperimentalCodePointApi
public expect fun String.codePointBefore(index: Int): CodePoint

/**
 * Returns the Unicode code point preceding the specified [index] in this char sequence.
 *
 * The [index] refers to the char-based position in the char sequence, and the returned value is the code point
 * represented by the character or surrogate pair (a high surrogate char followed by a low surrogate char)
 * immediately preceding that position.
 *
 * If the `index-1` position points to an unpaired surrogate character, for example,
 * a low surrogate char not preceded by a high surrogate char,
 * the code point with the code of that only one character is returned.
 *
 * @param index the char-based position in the char sequence following the Unicode code point that is to be returned, must be in range `1..length`.
 * @return the Unicode code point value preceding the specified [index].
 * @throws IndexOutOfBoundsException if [index] is out of range.
 */
@ExperimentalCodePointApi
public expect fun CharSequence.codePointBefore(index: Int): CodePoint

/**
 * Returns the Unicode code point preceding the specified [index] in this char array.
 *
 * The [index] refers to the char-based position in the char array, and the returned value is the code point
 * represented by the character or surrogate pair (a high surrogate char followed by a low surrogate char)
 * immediately preceding that position.
 *
 * If the `index-1` position points to an unpaired surrogate character, for example,
 * a low surrogate char not preceded by a high surrogate char,
 * the code point with the code of that only one character is returned.
 *
 * @param index the char-based position in the char array following the Unicode code point that is to be returned, must be in range `1..size`.
 * @return the Unicode code point value preceding the specified [index].
 * @throws IndexOutOfBoundsException if [index] is out of range.
 */
@ExperimentalCodePointApi
public expect fun CharArray.codePointBefore(index: Int): CodePoint

/**
 * Returns the Unicode code point preceding the specified [index] in this char array.
 *
 * The [index] refers to the char-based position in the char array, and the returned value is the code point
 * represented by the character or surrogate pair (a high surrogate char followed by a low surrogate char)
 * immediately preceding that position.
 *
 * If the `index-1` position points to an unpaired surrogate character, for example,
 * a low surrogate char not preceded by a high surrogate char,
 * the code point with the code of that only one character is returned.
 *
 * The [startIndex] limits the beginning of a subrange in the char array, so that only characters at indices greater than or equal to [startIndex] are considered.
 *
 * @param index the char-based position in the char array following the Unicode code point that is to be returned, must be in range `startIndex+1..size`.
 * @param startIndex the char-based position in the char array that limits the beginning of a subrange in the char array, must be in range `0..size`.
 * @return the Unicode code point value preceding the specified [index].
 * @throws IndexOutOfBoundsException if [index] or [startIndex] are out of range.
 */
@ExperimentalCodePointApi
public expect fun CharArray.codePointBefore(index: Int, startIndex: Int): CodePoint

/**
 * Counts the number of Unicode code points in the specified range of the string.
 *
 * The range is defined by the [startIndex] (inclusive) and [endIndex] (exclusive) parameters.
 * By default, the entire string is considered.
 *
 * Any unpaired surrogate chars, for example, appearing when `startIndex` or `endIndex` fall into
 * the middle of a surrogate pair, are counted as one code point each.
 *
 * @param startIndex The beginning index, inclusive. Defaults to 0.
 * @param endIndex The ending index, exclusive. Defaults to the length of the string.
 * @return The number of Unicode code points in the specified range.
 * @throws IndexOutOfBoundsException If [startIndex] or [endIndex] is out of the valid range `0..length` or `endIndex < startIndex`.
 */
@ExperimentalCodePointApi
public expect fun String.codePointCount(startIndex: Int = 0, endIndex: Int = this.length): Int

/**
 * Counts the number of Unicode code points in the specified range of the char sequence.
 *
 * The range is defined by the [startIndex] (inclusive) and [endIndex] (exclusive) parameters.
 * By default, the entire char sequence is considered.
 *
 * Any unpaired surrogate chars, for example, appearing when `startIndex` or `endIndex` fall into
 * the middle of a surrogate pair, are counted as one code point each.
 *
 * @param startIndex The beginning index, inclusive. Defaults to 0.
 * @param endIndex The ending index, exclusive. Defaults to the length of the char sequence.
 * @return The number of Unicode code points in the specified range.
 * @throws IndexOutOfBoundsException If [startIndex] or [endIndex] is out of the valid range `0..length` or `endIndex < startIndex`.
 */
@ExperimentalCodePointApi
public expect fun CharSequence.codePointCount(startIndex: Int = 0, endIndex: Int = this.length): Int

/**
 * Counts the number of Unicode code points in the specified range of the char array.
 *
 * The range is defined by the [startIndex] (inclusive) and [endIndex] (exclusive) parameters.
 * By default, the entire char array is considered.
 *
 * Any unpaired surrogate chars, for example, appearing when `startIndex` or `endIndex` fall into
 * the middle of a surrogate pair, are counted as one code point each.
 *
 * @param startIndex The beginning index, inclusive. Defaults to 0.
 * @param endIndex The ending index, exclusive. Defaults to the size of the char array.
 * @return The number of Unicode code points in the specified range.
 * @throws IndexOutOfBoundsException If [startIndex] or [endIndex] is out of the valid range `0..size` or `endIndex < startIndex`.
 */
@ExperimentalCodePointApi
public expect fun CharArray.codePointCount(startIndex: Int = 0, endIndex: Int = this.size): Int

/**
 * Calculates the index of the character that is a specified number of Unicode code points away from
 * the character at the given index in this string.
 *
 * Any unpaired surrogate chars, for example, appearing when the [index] is in the middle of a surrogate pair,
 * are counted as one code point each.
 *
 * @param index the index of the character in this string from which the offset is calculated.
 * @param codePointOffset the number of Unicode code points to offset from the character at the specified [index].
 *                        A positive offset moves forward in the string, while a negative offset moves backward.
 * @return the index of the character reached by applying the specified code point offset from the given [index].
 * @throws IndexOutOfBoundsException if the [index] is out of the valid range `0..length` or
 *         there are less than `codePointOffset` code points in the string starting at the specified [index] for positive [codePointOffset] or
 *         there are less than `-codePointOffset` code points in the string before the specified [index] for negative [codePointOffset].
 */
@ExperimentalCodePointApi
public expect fun String.offsetByCodePoints(index: Int, codePointOffset: Int): Int

/**
 * Calculates the index of the character that is a specified number of Unicode code points away from
 * the character at the given index in this char sequence.
 *
 * Any unpaired surrogate chars, for example, appearing when the [index] is in the middle of a surrogate pair,
 * are counted as one code point each.
 *
 * @param index the index of the character in this char sequence from which the offset is calculated.
 * @param codePointOffset the number of Unicode code points to offset from the character at the specified [index].
 *                        A positive offset moves forward in the char sequence, while a negative offset moves backward.
 * @return the index of the character reached by applying the specified code point offset from the given [index].
 * @throws IndexOutOfBoundsException if the [index] is out of the valid range `0..length` or
 *         there are less than `codePointOffset` code points in the string starting at the specified [index] for positive [codePointOffset] or
 *         there are less than `-codePointOffset` code points in the string before the specified [index] for negative [codePointOffset].
 */
@ExperimentalCodePointApi
public expect fun CharSequence.offsetByCodePoints(index: Int, codePointOffset: Int): Int

/**
 * Calculates the index of the character that is a specified number of Unicode code points away from
 * the character at the given index in the specified range of this array.
 *
 * The range is defined by the [startIndex] (inclusive) and [endIndex] (exclusive) parameters.
 * By default, the entire char array is considered.
 *
 * Any unpaired surrogate chars are counted, for example, appearing when the [index] is in the middle of a surrogate pair,
 * are counted as one code point each.
 *
 * @param index the index of the character in this string from which the offset is calculated.
 * @param codePointOffset the number of Unicode code points to offset from the character at the specified [index].
 *                        A positive offset moves forward in the string, while a negative offset moves backward.
 * @param startIndex The beginning index, inclusive. Defaults to 0.
 * @param endIndex The ending index, exclusive. Defaults to the size of the char array.
 * @return the index of the character reached by applying the specified code point offset from the given [index].
 * @throws IndexOutOfBoundsException If [startIndex] or [endIndex] is out of the valid range `0..size` or `endIndex < startIndex`.
 * @throws IndexOutOfBoundsException if the [index] is out of the valid range `startIndex..endIndex` or
 *         there are less than `codePointOffset` code points in the string starting at the specified [index] for positive [codePointOffset] or
 *         there are less than `-codePointOffset` code points in the string before the specified [index] for negative [codePointOffset].
 */
@ExperimentalCodePointApi
public expect fun CharArray.offsetByCodePoints(index: Int, codePointOffset: Int, startIndex: Int = 0, endIndex: Int = this.size): Int


/**
 * Executes the given [action] on each Unicode code point of this string.
 *
 * This function iterates through the string, extracting code points using [codePointAt] function
 * and passing them to the specified [action]. The iteration position is then advanced to the next code point.
 *
 * Any unpaired surrogate chars are passed to the [action] as individual code points.
 *
 * @see codePointSequence
 * @see codePointIterator
 */
@ExperimentalCodePointApi
public inline fun String.forEachCodePoint(action: (CodePoint) -> Unit) {
    forEachCodePointImpl(0, length, this::codePointAt, action)
}

/**
 * Executes the given [action] on each Unicode code point of this char sequence.
 *
 * This function iterates through the char sequence, extracting code points using [codePointAt] function
 * and passing them to the specified [action]. The iteration position is then advanced to the next code point.
 *
 * Any unpaired surrogate chars are passed to the [action] as individual code points.
 *
 * @see codePointSequence
 * @see codePointIterator
 */
@ExperimentalCodePointApi
public inline fun CharSequence.forEachCodePoint(action: (CodePoint) -> Unit) {
    forEachCodePointImpl(0, length, this::codePointAt, action)
}

/**
 * Executes the given [action] on each Unicode code point in the specified range of this char array.
 *
 * This function iterates through the char array, extracting code points using [codePointAt] function
 * and passing them to the specified [action]. The iteration position is then advanced to the next code point.
 *
 * The range is defined by the [startIndex] (inclusive) and [endIndex] (exclusive) parameters.
 * By default, the entire char array is iterated.
 *
 * Any unpaired surrogate chars are passed to the [action] as individual code points.
 *
 * @param startIndex The beginning index, inclusive. Defaults to 0.
 * @param endIndex The ending index, exclusive. Defaults to the size of the char array.
 * @throws IndexOutOfBoundsException If [startIndex] or [endIndex] is out of the valid range `0..charArray.size` or `endIndex < startIndex`.
 *
 * @see codePointSequence
 * @see codePointIterator
 */
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

/**
 * A sequence of Unicode code points.
 *
 * The sequence can be obtained from a [String], [CharSequence], or a [CharArray] with the [codePointSequence] function.
 * The sequence allows iterating through the code points in the container in the direction from the beginning to the end.
 */
@ExperimentalCodePointApi
public interface CodePointSequence : Sequence<CodePoint> {
    override fun iterator(): CodePointIterator
}

/**
 * A specialized iterator of Unicode code points.
 *
 * This iterator type is returned by [CodePointSequence.iterator] operator.
 * It iterates through the code points in the container in the direction from the beginning to the end.
 */
@ExperimentalCodePointApi
public interface CodePointIterator : Iterator<CodePoint> {
}

/**
 * An iterator for traversing Unicode code points bidirectionally within a text.
 *
 * This interface extends the capabilities of [CodePointIterator] by adding support for reverse iteration
 * and positional control.
 *
 * Functionalities provided by this iterator include:
 * - Determining if there are more code points to iterate in either forward or reverse direction: [hasNext], [hasPrevious].
 * - Determining the char position of the next or previous code point: [nextIndex], [previousIndex].
 * - Retrieving the next or previous code point using [next] and [previous] functions.
 * - Advancing the iteration by a specified number of code points in either direction: [advanceByCodePoints].
 *
 * The iterator can be obtained from a [String], [CharSequence], or [CharArray] using the [codePointIterator] function.
 */
// TODO: Name TBD
@ExperimentalCodePointApi
public interface CodePointIndexedIterator : CodePointIterator {
    /**
     * Returns `true` if the iterator can return more code points when iterating in forward direction.
     *
     * The function [next] can be used to retrieve the next code point, unless [hasNext] returns `false`.
     */
    public override fun hasNext(): Boolean

    /**
     * Returns `true` if the iterator can return more code points when iterating in reverse direction.
     *
     * The function [previous] can be used to retrieve the previous code point, unless [hasPrevious] returns `false`.
     */
    public fun hasPrevious(): Boolean

    /**
     * Returns the next code point in the iteration and advances the iteration position forward by one code point.
     */
    public override fun next(): CodePoint

    /**
     * Returns the previous code point in the iteration and moves the iteration position backward by one code point.
     */
    public fun previous(): CodePoint

    // nextIndex and previousIndex return the char offset of
    // the next/previous code point

    /**
     * Returns the char-based position of the next Unicode code point in the iteration that would be returned by a subsequent call to [next],
     * or the end index if the iteration has reached the end.
     */
    public fun nextIndex(): Int

    /**
     * Returns the char-based position of the previous Unicode code point in the iteration that would be returned by a subsequent call to [previous],
     * or `-1` if the iteration has reached the beginning.
     */
    public fun previousIndex(): Int

    /**
     * Advances the iterator by the specified number of Unicode code points, either forward or backward.
     *
     * If the [codePointOffset] is positive, the iterator moves forward by the specified number of code points.
     * If the [codePointOffset] is negative, the iterator moves backward by the specified number of code points.
     * The zero [codePointOffset] does not move the position of the iterator.
     *
     * @throws IndexOutOfBoundsException if the offset moves the iterator beyond the allowable range of iteration.
     */
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

/**
 * Returns a [CodePointSequence] iterating through Unicode code points in this string.
 *
 * The sequence allows iteration over the code points from the beginning to the end of the string.
 *
 * @see forEachCodePoint
 */
@ExperimentalCodePointApi
public fun String.codePointSequence(): CodePointSequence =
    object : CodePointSequence {
        override fun iterator(): CodePointIterator = object : AbstractCodePointIterator(0, length) {
            override fun codePointAt(index: Int): CodePoint = this@codePointSequence.codePointAt(index)
        }
    }

/**
 * Returns a [CodePointSequence] iterating through Unicode code points in this char sequence.
 *
 * The sequence allows iteration over the code points from the beginning to the end of the specified range of this char array.
 *
 * @see forEachCodePoint
 */
@ExperimentalCodePointApi
public fun CharSequence.codePointSequence(): CodePointSequence =
    object : CodePointSequence {
        override fun iterator(): CodePointIterator = object : AbstractCodePointIterator(0, length) {
            override fun codePointAt(index: Int): CodePoint = this@codePointSequence.codePointAt(index)
        }
    }

/**
 * Returns a [CodePointSequence] iterating through Unicode code points in the specified range of this char array.
 *
 * The sequence allows iteration over the code points from the beginning to the end of the specified range.
 *
 * The range is defined by the [startIndex] (inclusive) and [endIndex] (exclusive) parameters.
 * By default, the entire char array is iterated.
 *
 * @param startIndex The beginning index, inclusive. Defaults to 0.
 * @param endIndex The ending index, exclusive. Defaults to the size of the char array.
 * @throws IndexOutOfBoundsException If [startIndex] or [endIndex] is out of the valid range `0..size` or `endIndex < startIndex`.
 *
 * @see forEachCodePoint
 */
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

/**
 * Creates an iterator over the Unicode code points of the string starting at the specified char-based position [index].
 * The returned iterator allows traversal of code points in both forward and reverse directions.
 *
 * @param index The starting index in the string to begin iteration. Must be in the range `0..length`.
 * @return A [CodePointIndexedIterator] for traversing the string's Unicode code points from the specified index.
 * @throws IndexOutOfBoundsException if [index] is not within the range `0..length`.
 */
@ExperimentalCodePointApi
public fun String.codePointIterator(index: Int = 0): CodePointIndexedIterator {
    AbstractList.checkPositionIndex(index, length)
    return object : AbstractCodePointIndexedIterator(index, 0, length) {
        override fun codePointAt(index: Int): CodePoint = this@codePointIterator.codePointAt(index)
        override fun codePointBefore(index: Int): CodePoint = this@codePointIterator.codePointBefore(index)
        override fun offsetByCodePoints(index: Int, codePointOffset: Int): Int = this@codePointIterator.offsetByCodePoints(index, codePointOffset)
    }
}

/**
 * Creates an iterator over the Unicode code points of the char sequence starting at the specified char-based position [index].
 * The returned iterator allows traversal of code points in both forward and reverse directions.
 *
 * @param index The starting index in the string to begin iteration. Must be in the range `0..length`.
 * @return A [CodePointIndexedIterator] for traversing the string's Unicode code points from the specified index.
 * @throws IndexOutOfBoundsException if [index] is not within the range `0..length`.
 */
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

/**
 * Creates an iterator over the Unicode code points of the specified range of this char array at the specified char-based position [index].
 * The returned iterator allows traversal of code points in both forward and reverse directions.
 *
 * The range is defined by the [startIndex] (inclusive) and [endIndex] (exclusive) parameters.
 * By default, the entire char array is iterated.
 *
 * @param index The starting index in the string to begin iteration. Must be in the range `startIndex..endIndex`.
 * @param startIndex The beginning index, inclusive. Defaults to 0.
 * @param endIndex The ending index, exclusive. Defaults to the size of the char array.
 * @return A [CodePointIndexedIterator] for traversing the string's Unicode code points from the specified index.
 * @throws IndexOutOfBoundsException If [startIndex] or [endIndex] is out of the valid range `0..size` or `endIndex < startIndex`.
 * @throws IndexOutOfBoundsException if [index] is not within the range `startIndex..endIndex`.
 */
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
