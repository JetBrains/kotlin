/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

/**
 * A mutable sequence of characters.
 *
 * String builder can be used to efficiently perform multiple string manipulation operations.
 */
public actual class StringBuilder : CharSequence, Appendable {
    private var array: String
    private var _length: Int

    /** Constructs an empty string builder. */
    public actual constructor() : this(10)

    /** Constructs an empty string builder with the specified initial [capacity]. */
    public actual constructor(capacity: Int) {
        _length = 0
        array = unsafeStringCopy("", capacity)
    }

    /** Constructs a string builder that contains the same characters as the specified [content] string. */
    public actual constructor(content: String) {
        _length = content.length
        array = unsafeStringCopy(content, _length)
    }

    /** Constructs a string builder that contains the same characters as the specified [content] char sequence. */
    public actual constructor(content: CharSequence) : this(content.length) {
        append(content)
    }

    // Of CharSequence.
    actual override val length: Int
        get() = _length

    actual override fun get(index: Int): Char {
        AbstractList.checkElementIndex(index, _length)
        return array[index]
    }

    actual override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        substring(startIndex, endIndex)

    // Of Appendable.
    actual override fun append(value: Char) : StringBuilder {
        ensureExtraCapacity(1)
        array = unsafeStringSetChar(array, _length++, value)
        return this
    }

    actual override fun append(value: CharSequence?): StringBuilder =
        if (value == null) append(null as String?) else appendRange(value, 0, value.length)

    actual override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): StringBuilder =
        if (value == null) append(null as String?) else appendRange(value, startIndex, endIndex)

    /**
     * Reverses the contents of this string builder and returns this instance.
     *
     * Surrogate pairs included in this string builder are treated as single characters.
     * Therefore, the order of the high-low surrogates is never reversed.
     *
     * Note that the reverse operation may produce new surrogate pairs that were unpaired low-surrogates and high-surrogates before the operation.
     * For example, reversing `"\uDC00\uD800"` produces `"\uD800\uDC00"` which is a valid surrogate pair.
     */
    // Based on Apache Harmony implementation.
    public actual fun reverse(): StringBuilder {
        if (this.length < 2) {
            return this
        }
        var end = _length - 1
        var front = 0
        var frontLeadingChar = array[0]
        var endTrailingChar = array[end]
        var allowFrontSurrogate = true
        var allowEndSurrogate = true
        while (front < _length / 2) {
            val frontTrailingChar = array[front + 1]
            val endLeadingChar = array[end - 1]
            val surrogateAtFront = allowFrontSurrogate && frontTrailingChar.isLowSurrogate() && frontLeadingChar.isHighSurrogate()
            if (surrogateAtFront && _length < 3) {
                return this
            }
            val surrogateAtEnd = allowEndSurrogate && endTrailingChar.isLowSurrogate() && endLeadingChar.isHighSurrogate()
            allowFrontSurrogate = true
            allowEndSurrogate = true
            when {
                surrogateAtFront && surrogateAtEnd -> {
                    // Both surrogates - just exchange them.
                    unsafeStringSetChar(array, end, frontTrailingChar)
                    unsafeStringSetChar(array, end - 1, frontLeadingChar)
                    unsafeStringSetChar(array, front, endLeadingChar)
                    unsafeStringSetChar(array, front + 1, endTrailingChar)
                    frontLeadingChar = array[front + 2]
                    endTrailingChar = array[end - 2]
                    front++
                    end--
                }
                !surrogateAtFront && !surrogateAtEnd -> {
                    // Neither surrogates - exchange only front/end.
                    unsafeStringSetChar(array, end, frontLeadingChar)
                    unsafeStringSetChar(array, front, endTrailingChar)
                    frontLeadingChar = frontTrailingChar
                    endTrailingChar = endLeadingChar
                }
                surrogateAtFront && !surrogateAtEnd -> {
                    // Surrogate only at the front -
                    // move the low part, the high part will be moved as a usual character on the next iteration.
                    unsafeStringSetChar(array, end, frontTrailingChar)
                    unsafeStringSetChar(array, front, endTrailingChar)
                    endTrailingChar = endLeadingChar
                    allowFrontSurrogate = false
                }
                !surrogateAtFront && surrogateAtEnd -> {
                    // Surrogate only at the end -
                    // move the high part, the low part will be moved as a usual character on the next iteration.
                    unsafeStringSetChar(array, end, frontLeadingChar)
                    unsafeStringSetChar(array, front, endLeadingChar)
                    frontLeadingChar = frontTrailingChar
                    allowEndSurrogate = false
                }
            }
            front++
            end--
        }
        if (_length % 2 == 1 && (!allowEndSurrogate || !allowFrontSurrogate)) {
            unsafeStringSetChar(array, end, if (allowFrontSurrogate) endTrailingChar else frontLeadingChar)
        }
        return this
    }

    /**
     * Appends the string representation of the specified object [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    public actual fun append(value: Any?): StringBuilder = append(value.toString())

    /**
     * Appends the string representation of the specified boolean [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    public actual fun append(value: Boolean): StringBuilder = append(if (value) "true" else "false")

    /**
     * Appends the string representation of the specified byte [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    public fun append(value: Byte): StringBuilder = append(value.toInt())

    /**
     * Appends the string representation of the specified short [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    public fun append(value: Short): StringBuilder = append(value.toInt())

    /**
     * Appends the string representation of the specified int [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    public actual fun append(value: Int): StringBuilder {
        ensureExtraCapacity(11)
        _length += unsafeStringSetInt(array, _length, value)
        return this
    }

    // TODO: optimize the append overloads with primitive value!

    /**
     * Appends the string representation of the specified long [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    public actual fun append(value: Long): StringBuilder = append(value.toString())

    /**
     * Appends the string representation of the specified float [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    public actual fun append(value: Float): StringBuilder = append(value.toString())

    /**
     * Appends the string representation of the specified double [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    public actual fun append(value: Double): StringBuilder = append(value.toString())

    /**
     * Appends characters in the specified character array [value] to this string builder and returns this instance.
     *
     * Characters are appended in order, starting at the index 0.
     */
    public actual fun append(value: CharArray): StringBuilder {
        val valueSize = value.size
        ensureExtraCapacity(valueSize)
        array = unsafeStringSetArray(array, _length, value, 0, valueSize)
        _length += valueSize
        return this
    }

    /**
     * Appends the specified string [value] to this string builder and returns this instance.
     *
     * If [value] is `null`, then the four characters `"null"` are appended.
     */
    public actual fun append(value: String?): StringBuilder {
        val toAppend = value ?: "null"
        val toAppendLength = toAppend.length
        ensureExtraCapacity(toAppendLength)
        array = unsafeStringSetString(array, _length, toAppend, 0, toAppendLength)
        _length += toAppendLength
        return this
    }

    /**
     * Returns the current capacity of this string builder.
     *
     * The capacity is the maximum length this string builder can have before an allocation occurs.
     */
    public actual fun capacity(): Int = array.length

    /**
     * Ensures that the capacity of this string builder is at least equal to the specified [minimumCapacity].
     *
     * If the current capacity is less than the [minimumCapacity], a new backing storage is allocated with greater capacity.
     * Otherwise, this method takes no action and simply returns.
     */
    public actual fun ensureCapacity(minimumCapacity: Int) {
        if (minimumCapacity <= array.length) return
        array = unsafeStringCopy(array, AbstractList.newCapacity(array.length, minimumCapacity))
    }

    /**
     * Returns the index within this string builder of the first occurrence of the specified [string].
     *
     * Returns `-1` if the specified [string] does not occur in this string builder.
     */
    @SinceKotlin("1.4")
    public actual fun indexOf(string: String): Int = indexOf(string, startIndex = 0)

    /**
     * Returns the index within this string builder of the first occurrence of the specified [string],
     * starting at the specified [startIndex].
     *
     * Returns `-1` if the specified [string] does not occur in this string builder starting at the specified [startIndex].
     */
    @SinceKotlin("1.4")
    public actual fun indexOf(string: String, startIndex: Int): Int {
        val result = array.indexOf(string, startIndex, ignoreCase = false)
        return if (result < 0 || result + string.length <= _length) result else -1
    }

    /**
     * Returns the index within this string builder of the last occurrence of the specified [string].
     * The last occurrence of empty string `""` is considered to be at the index equal to `this.length`.
     *
     * Returns `-1` if the specified [string] does not occur in this string builder.
     */
    @SinceKotlin("1.4")
    public actual fun lastIndexOf(string: String): Int =
        array.lastIndexOf(string, lastIndex, ignoreCase = false)

    /**
     * Returns the index within this string builder of the last occurrence of the specified [string],
     * starting from the specified [startIndex] toward the beginning.
     *
     * Returns `-1` if the specified [string] does not occur in this string builder starting at the specified [startIndex].
     */
    @SinceKotlin("1.4")
    public actual fun lastIndexOf(string: String, startIndex: Int): Int =
        array.lastIndexOf(string, startIndex.coerceAtMost(_length), ignoreCase = false)

    /**
     * Inserts the string representation of the specified boolean [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public actual fun insert(index: Int, value: Boolean): StringBuilder = insert(index, if (value) "true" else "false")

    /**
     * Inserts the string representation of the specified byte [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public fun insert(index: Int, value: Byte): StringBuilder = insert(index, value.toInt())

    /**
     * Inserts the string representation of the specified short [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public fun insert(index: Int, value: Short): StringBuilder = insert(index, value.toInt())

    /**
     * Inserts the string representation of the specified int [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public actual fun insert(index: Int, value: Int): StringBuilder {
        AbstractList.checkPositionIndex(index, _length)
        ensureExtraCapacity(11)
        _length += unsafeStringSetInt(array, index, value)
        return this
    }

    // TODO: optimize the insert overloads with primitive value!

    /**
     * Inserts the string representation of the specified long [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public actual fun insert(index: Int, value: Long): StringBuilder = insert(index, value.toString())

    /**
     * Inserts the string representation of the specified float [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public actual fun insert(index: Int, value: Float): StringBuilder = insert(index, value.toString())

    /**
     * Inserts the string representation of the specified double [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public actual fun insert(index: Int, value: Double): StringBuilder = insert(index, value.toString())

    /**
     * Inserts the specified character [value] into this string builder at the specified [index] and returns this instance.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public actual fun insert(index: Int, value: Char): StringBuilder {
        AbstractList.checkPositionIndex(index, _length)
        ensureExtraCapacity(1)
        unsafeStringSetString(array, index + 1, array, index, _length)
        array = unsafeStringSetChar(array, index, value)
        _length++
        return this
    }

    /**
     * Inserts characters in the specified character array [value] into this string builder at the specified [index] and returns this instance.
     *
     * The inserted characters go in same order as in the [value] character array, starting at [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public actual fun insert(index: Int, value: CharArray): StringBuilder {
        AbstractList.checkPositionIndex(index, _length)
        val valueSize = value.size
        ensureExtraCapacity(valueSize)
        unsafeStringSetString(array, index + valueSize, array, index, _length)
        array = unsafeStringSetArray(array, index, value, 0, valueSize)
        _length += valueSize
        return this
    }

    /**
     * Inserts characters in the specified character sequence [value] into this string builder at the specified [index] and returns this instance.
     *
     * The inserted characters go in the same order as in the [value] character sequence, starting at [index].
     *
     * @param index the position in this string builder to insert at.
     * @param value the character sequence from which characters are inserted. If [value] is `null`, then the four characters `"null"` are inserted.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public actual fun insert(index: Int, value: CharSequence?): StringBuilder =
        if (value == null) insert(index, null as String?) else insertRange(index, value, 0, value.length)

    /**
     * Inserts the string representation of the specified object [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public actual fun insert(index: Int, value: Any?): StringBuilder = insert(index, value.toString())

    /**
     * Inserts the string [value] into this string builder at the specified [index] and returns this instance.
     *
     * If [value] is `null`, then the four characters `"null"` are inserted.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public actual fun insert(index: Int, value: String?): StringBuilder {
        AbstractList.checkPositionIndex(index, _length)
        val toInsert = value ?: "null"
        val toInsertLength = toInsert.length
        ensureExtraCapacity(toInsertLength)
        unsafeStringSetString(array, index + toInsertLength, array, index, _length)
        array = unsafeStringSetString(array, index, toInsert, 0, toInsertLength)
        _length += toInsertLength
        return this
    }

    /**
     *  Sets the length of this string builder to the specified [newLength].
     *
     *  If the [newLength] is less than the current length, it is changed to the specified [newLength].
     *  Otherwise, null characters '\u0000' are appended to this string builder until its length is less than the [newLength].
     *
     *  Note that in Kotlin/JS [set] operator function has non-constant execution time complexity.
     *  Therefore, increasing length of this string builder and then updating each character by index may slow down your program.
     *
     *  @throws IndexOutOfBoundsException or [IllegalArgumentException] if [newLength] is less than zero.
     */
    public actual fun setLength(newLength: Int) {
        if (newLength < 0) {
            throw IllegalArgumentException("Negative new length: $newLength.")
        }
        for (i in _length until newLength.coerceAtMost(array.length)) {
            unsafeStringSetChar(array, i, '\u0000')
        }
        ensureCapacity(newLength)
        _length = newLength
    }

    /**
     * Returns a new [String] that contains characters in this string builder at [startIndex] (inclusive) and up to the [endIndex] (exclusive).
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
     */
    public actual fun substring(startIndex: Int, endIndex: Int): String {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, _length)
        return array.substring(startIndex, endIndex)
    }

    /**
     * Returns a new [String] that contains characters in this string builder at [startIndex] (inclusive) and up to the [length] (exclusive).
     *
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    public actual fun substring(startIndex: Int): String = substring(startIndex, _length)

    /**
     * Attempts to reduce storage used for this string builder.
     *
     * If the backing storage of this string builder is larger than necessary to hold its current contents,
     * then it may be resized to become more space efficient.
     * Calling this method may, but is not required to, affect the value of the [capacity] property.
     */
    public actual fun trimToSize() {
        if (_length < array.length)
            array = array.substring(0, _length)
    }

    override fun toString(): String = array.substring(0, _length)

    /**
     * Sets the character at the specified [index] to the specified [value].
     *
     * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
     */
    public operator fun set(index: Int, value: Char) {
        AbstractList.checkElementIndex(index, _length)
        array = unsafeStringSetChar(array, index, value)
    }

    /**
     * Replaces characters in the specified range of this string builder with characters in the specified string [value] and returns this instance.
     *
     * @param startIndex the beginning (inclusive) of the range to replace.
     * @param endIndex the end (exclusive) of the range to replace.
     * @param value the string to replace with.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] if [startIndex] is less than zero, greater than the length of this string builder, or `startIndex > endIndex`.
     */
    @SinceKotlin("1.4")
    public fun setRange(startIndex: Int, endIndex: Int, value: String): StringBuilder {
        checkReplaceRange(startIndex, endIndex, _length)
        val coercedEndIndex = endIndex.coerceAtMost(_length)
        val valueLength = value.length
        val lengthDiff = valueLength - (coercedEndIndex - startIndex)
        ensureExtraCapacity(lengthDiff)
        unsafeStringSetString(array, startIndex + valueLength, array, coercedEndIndex, _length)
        array = unsafeStringSetString(array, startIndex, value, 0, valueLength)
        _length += lengthDiff
        return this
    }

    /**
     * Removes the character at the specified [index] from this string builder and returns this instance.
     *
     * If the `Char` at the specified [index] is part of a supplementary code point, this method does not remove the entire supplementary character.
     *
     * @param index the index of `Char` to remove.
     *
     * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
     */
    @SinceKotlin("1.4")
    public fun deleteAt(index: Int): StringBuilder {
        AbstractList.checkElementIndex(index, _length)
        unsafeStringSetString(array, index, array, index + 1, _length)
        --_length
        return this
    }

    /**
     * Removes characters in the specified range from this string builder and returns this instance.
     *
     * @param startIndex the beginning (inclusive) of the range to remove.
     * @param endIndex the end (exclusive) of the range to remove.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
     */
    @SinceKotlin("1.4")
    public fun deleteRange(startIndex: Int, endIndex: Int): StringBuilder {
        checkReplaceRange(startIndex, endIndex, _length)
        val coercedEndIndex = endIndex.coerceAtMost(_length)
        unsafeStringSetString(array, startIndex, array, coercedEndIndex, _length)
        _length -= coercedEndIndex - startIndex
        return this
    }

    /**
     * Copies characters from this string builder into the [destination] character array.
     *
     * @param destination the array to copy to.
     * @param destinationOffset the position in the array to copy to, 0 by default.
     * @param startIndex the beginning (inclusive) of the range to copy, 0 by default.
     * @param endIndex the end (exclusive) of the range to copy, length of this string builder by default.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException when the subrange doesn't fit into the [destination] array starting at the specified [destinationOffset],
     *  or when that index is out of the [destination] array indices range.
     */
    @SinceKotlin("1.4")
    public fun toCharArray(destination: CharArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = this.length) {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, _length)
        AbstractList.checkBoundsIndexes(destinationOffset, destinationOffset + endIndex - startIndex, destination.size)
        array.toCharArray(destination, destinationOffset, startIndex, endIndex)
    }

    /**
     * Appends characters in a subarray of the specified character array [value] to this string builder and returns this instance.
     *
     * Characters are appended in order, starting at specified [startIndex].
     *
     * @param value the array from which characters are appended.
     * @param startIndex the beginning (inclusive) of the subarray to append.
     * @param endIndex the end (exclusive) of the subarray to append.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] array indices or when `startIndex > endIndex`.
     */
    @SinceKotlin("1.4")
    public fun appendRange(value: CharArray, startIndex: Int, endIndex: Int): StringBuilder =
        insertRange(_length, value, startIndex, endIndex)

    /**
     * Appends a subsequence of the specified character sequence [value] to this string builder and returns this instance.
     *
     * @param value the character sequence from which a subsequence is appended.
     * @param startIndex the beginning (inclusive) of the subsequence to append.
     * @param endIndex the end (exclusive) of the subsequence to append.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] character sequence indices or when `startIndex > endIndex`.
     */
    @SinceKotlin("1.4")
    public fun appendRange(value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder =
        insertRange(_length, value, startIndex, endIndex)

    /**
     * Inserts characters in a subsequence of the specified character sequence [value] into this string builder at the specified [index] and returns this instance.
     *
     * The inserted characters go in the same order as in the [value] character sequence, starting at [index].
     *
     * @param index the position in this string builder to insert at.
     * @param value the character sequence from which a subsequence is inserted.
     * @param startIndex the beginning (inclusive) of the subsequence to insert.
     * @param endIndex the end (exclusive) of the subsequence to insert.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] character sequence indices or when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    public fun insertRange(index: Int, value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, value.length)
        AbstractList.checkPositionIndex(index, _length)
        val extraLength = endIndex - startIndex
        ensureExtraCapacity(extraLength)
        unsafeStringSetString(array, index + extraLength, array, index, _length)
        when (value) {
            is String ->
                array = unsafeStringSetString(array, index, value, startIndex, endIndex)
            is StringBuilder ->
                array = unsafeStringSetString(array, index, value.array, startIndex, endIndex)
            else -> {
                var from = startIndex
                var to = index
                while (from < endIndex) {
                    array = unsafeStringSetChar(array, to++, value[from++])
                }
            }
        }
        _length += extraLength
        return this
    }

    /**
     * Inserts characters in a subarray of the specified character array [value] into this string builder at the specified [index] and returns this instance.
     *
     * The inserted characters go in same order as in the [value] array, starting at [index].
     *
     * @param index the position in this string builder to insert at.
     * @param value the array from which characters are inserted.
     * @param startIndex the beginning (inclusive) of the subarray to insert.
     * @param endIndex the end (exclusive) of the subarray to insert.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] array indices or when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    public fun insertRange(index: Int, value: CharArray, startIndex: Int, endIndex: Int): StringBuilder {
        AbstractList.checkPositionIndex(index, _length)
        AbstractList.checkBoundsIndexes(startIndex, endIndex, value.size)
        val extraLength = endIndex - startIndex
        ensureExtraCapacity(extraLength)
        unsafeStringSetString(array, index + extraLength, array, index, _length)
        array = unsafeStringSetArray(array, index, value, startIndex, endIndex)
        _length += extraLength
        return this
    }

    // ---------------------------- private ----------------------------

    private fun ensureExtraCapacity(extraLength: Int) {
        val minimumCapacity = _length + extraLength
        if (minimumCapacity < 0) throw OutOfMemoryError() // overflow
        ensureCapacity(minimumCapacity)
    }

    private fun checkReplaceRange(startIndex: Int, endIndex: Int, length: Int) {
        if (startIndex < 0 || startIndex > length) {
            throw IndexOutOfBoundsException("startIndex: $startIndex, length: $length")
        }
        if (startIndex > endIndex) {
            throw IllegalArgumentException("startIndex($startIndex) > endIndex($endIndex)")
        }
    }
}


/**
 * Appends the string representation of the specified byte [value] to this string builder and returns this instance.
 *
 * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
 * and then that string was appended to this string builder.
 */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.append(value: Byte): StringBuilder = this.append(value)

/**
 * Appends the string representation of the specified short [value] to this string builder and returns this instance.
 *
 * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
 * and then that string was appended to this string builder.
 */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.append(value: Short): StringBuilder = this.append(value)

/**
 * Inserts the string representation of the specified byte [value] into this string builder at the specified [index] and returns this instance.
 *
 * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
 * and then that string was inserted into this string builder at the specified [index].
 *
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.insert(index: Int, value: Byte): StringBuilder = this.insert(index, value)

/**
 * Inserts the string representation of the specified short [value] into this string builder at the specified [index] and returns this instance.
 *
 * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
 * and then that string was inserted into this string builder at the specified [index].
 *
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.insert(index: Int, value: Short): StringBuilder = this.insert(index, value)

/**
 * Clears the content of this string builder making it empty and returns this instance.
 *
 * @sample samples.text.Strings.clearStringBuilder
 */
@SinceKotlin("1.3")
public actual fun StringBuilder.clear(): StringBuilder = apply { setLength(0) }

/**
 * Sets the character at the specified [index] to the specified [value].
 *
 * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
 */
@kotlin.internal.InlineOnly
public actual inline operator fun StringBuilder.set(index: Int, value: Char): Unit = this.set(index, value)

/**
 * Replaces characters in the specified range of this string builder with characters in the specified string [value] and returns this instance.
 *
 * @param startIndex the beginning (inclusive) of the range to replace.
 * @param endIndex the end (exclusive) of the range to replace.
 * @param value the string to replace with.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] if [startIndex] is less than zero, greater than the length of this string builder, or `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.setRange(startIndex: Int, endIndex: Int, value: String): StringBuilder =
    this.setRange(startIndex, endIndex, value)

/**
 * Removes the character at the specified [index] from this string builder and returns this instance.
 *
 * If the `Char` at the specified [index] is part of a supplementary code point, this method does not remove the entire supplementary character.
 *
 * @param index the index of `Char` to remove.
 *
 * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.deleteAt(index: Int): StringBuilder = this.deleteAt(index)

/**
 * Removes characters in the specified range from this string builder and returns this instance.
 *
 * @param startIndex the beginning (inclusive) of the range to remove.
 * @param endIndex the end (exclusive) of the range to remove.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.deleteRange(startIndex: Int, endIndex: Int): StringBuilder = this.deleteRange(startIndex, endIndex)

/**
 * Copies characters from this string builder into the [destination] character array.
 *
 * @param destination the array to copy to.
 * @param destinationOffset the position in the array to copy to, 0 by default.
 * @param startIndex the beginning (inclusive) of the range to copy, 0 by default.
 * @param endIndex the end (exclusive) of the range to copy, length of this string builder by default.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException when the subrange doesn't fit into the [destination] array starting at the specified [destinationOffset],
 *  or when that index is out of the [destination] array indices range.
 */
@SinceKotlin("1.4")
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.toCharArray(destination: CharArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = this.length): Unit =
    this.toCharArray(destination, destinationOffset, startIndex, endIndex)

/**
 * Appends characters in a subarray of the specified character array [value] to this string builder and returns this instance.
 *
 * Characters are appended in order, starting at specified [startIndex].
 *
 * @param value the array from which characters are appended.
 * @param startIndex the beginning (inclusive) of the subarray to append.
 * @param endIndex the end (exclusive) of the subarray to append.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] array indices or when `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendRange(value: CharArray, startIndex: Int, endIndex: Int): StringBuilder =
    this.appendRange(value, startIndex, endIndex)

/**
 * Appends a subsequence of the specified character sequence [value] to this string builder and returns this instance.
 *
 * @param value the character sequence from which a subsequence is appended.
 * @param startIndex the beginning (inclusive) of the subsequence to append.
 * @param endIndex the end (exclusive) of the subsequence to append.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] character sequence indices or when `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendRange(value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder =
    this.appendRange(value, startIndex, endIndex)

/**
 * Inserts characters in a subarray of the specified character array [value] into this string builder at the specified [index] and returns this instance.
 *
 * The inserted characters go in same order as in the [value] array, starting at [index].
 *
 * @param index the position in this string builder to insert at.
 * @param value the array from which characters are inserted.
 * @param startIndex the beginning (inclusive) of the subarray to insert.
 * @param endIndex the end (exclusive) of the subarray to insert.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] array indices or when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.insertRange(index: Int, value: CharArray, startIndex: Int, endIndex: Int): StringBuilder =
    this.insertRange(index, value, startIndex, endIndex)

/**
 * Inserts characters in a subsequence of the specified character sequence [value] into this string builder at the specified [index] and returns this instance.
 *
 * The inserted characters go in the same order as in the [value] character sequence, starting at [index].
 *
 * @param index the position in this string builder to insert at.
 * @param value the character sequence from which a subsequence is inserted.
 * @param startIndex the beginning (inclusive) of the subsequence to insert.
 * @param endIndex the end (exclusive) of the subsequence to insert.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] character sequence indices or when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.insertRange(index: Int, value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder =
    this.insertRange(index, value, startIndex, endIndex)

// Method renamings
/**
 * Inserts characters in a subsequence of the specified character sequence [csq] into this string builder at the specified [index] and returns this instance.
 *
 * The inserted characters go in the same order as in the [csq] character sequence, starting at [index].
 *
 * @param index the position in this string builder to insert at.
 * @param csq the character sequence from which a subsequence is inserted. If [csq] is `null`,
 *  then characters will be inserted as if [csq] contained the four characters `"null"`.
 * @param start the beginning (inclusive) of the subsequence to insert.
 * @param end the end (exclusive) of the subsequence to insert.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [start] or [end] is out of range of the [csq] character sequence indices or when `start > end`.
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated(
    "Use insertRange(index: Int, csq: CharSequence, start: Int, end: Int) instead",
    ReplaceWith("insertRange(index, csq ?: \"null\", start, end)")
)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.insert(index: Int, csq: CharSequence?, start: Int, end: Int): StringBuilder =
    this.insertRange(index, csq ?: "null", start, end)

@DeprecatedSinceKotlin(warningSince = "1.3", errorSince = "1.6")
@Deprecated("Use set(index: Int, value: Char) instead", ReplaceWith("set(index, value)"))
@kotlin.internal.InlineOnly
public inline fun StringBuilder.setCharAt(index: Int, value: Char): Unit = this.set(index, value)

/**
 * Removes the character at the specified [index] from this string builder and returns this instance.
 *
 * If the `Char` at the specified [index] is part of a supplementary code point, this method does not remove the entire supplementary character.
 *
 * @param index the index of `Char` to remove.
 *
 * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
 */
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated("Use deleteAt(index: Int) instead", ReplaceWith("deleteAt(index)"))
@kotlin.internal.InlineOnly
public inline fun StringBuilder.deleteCharAt(index: Int): StringBuilder = this.deleteAt(index)

internal expect fun unsafeStringFromCharArray(array: CharArray, start: Int, size: Int): String

// Returns a *mutable* copy of the string that can be modified by the functions below.
// `length` has to be at least `string.length`; if it's greater, the copy has extra space at the end.
internal expect fun unsafeStringCopy(string: String, length: Int): String

// These intrinsics do not do any bounds checks. They modify the existing string if possible,
// but can return a new string if the current representation cannot encode the new value. This can
// be ignored if it's known that the value is in range for the current encoding, e.g. if it was
// obtained from this string in the first place or if it's always ASCII.
internal expect fun unsafeStringSetChar(string: String, index: Int, c: Char): String
internal expect fun unsafeStringSetArray(string: String, index: Int, value: CharArray, start: Int, end: Int): String
internal expect fun unsafeStringSetString(string: String, index: Int, value: String, start: Int, end: Int): String
internal expect fun unsafeStringSetInt(string: String, index: Int, value: Int): Int
