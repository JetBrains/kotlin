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
actual class StringBuilder private constructor (
        private var array: CharArray) : CharSequence, Appendable {

    /** Constructs an empty string builder. */
    actual constructor() : this(10)

    /** Constructs an empty string builder with the specified initial [capacity]. */
    actual constructor(capacity: Int) : this(CharArray(capacity))

    /** Constructs a string builder that contains the same characters as the specified [content] string. */
    actual constructor(content: String) : this(content.toCharArray()) {
        _length = array.size
    }

    /** Constructs a string builder that contains the same characters as the specified [content] char sequence. */
    actual constructor(content: CharSequence): this(content.length) {
        append(content)
    }

    // Of CharSequence.
    private var _length: Int = 0
        set(capacity) {
            ensureCapacity(capacity)
            field = capacity
        }
    actual override val length: Int
        get() = _length

    actual override fun get(index: Int): Char {
        checkIndex(index)
        return array[index]
    }

    actual override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = substring(startIndex, endIndex)

    // Of Appenable.
    actual override fun append(value: Char) : StringBuilder {
        ensureExtraCapacity(1)
        array[_length++] = value
        return this
    }

    actual override fun append(value: CharSequence?): StringBuilder {
        // Kotlin/JVM processes null as if the argument was "null" char sequence.
        val toAppend = value ?: "null"
        return append(toAppend, 0, toAppend.length)
    }

    actual override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): StringBuilder =
            this.appendRange(value ?: "null", startIndex, endIndex)

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
    actual fun reverse(): StringBuilder {
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

            var frontTrailingChar = array[front + 1]
            var endLeadingChar = array[end - 1]
            var surrogateAtFront = allowFrontSurrogate && frontTrailingChar.isLowSurrogate() && frontLeadingChar.isHighSurrogate()
            if (surrogateAtFront && _length < 3) {
                return this
            }
            var surrogateAtEnd = allowEndSurrogate && endTrailingChar.isLowSurrogate() && endLeadingChar.isHighSurrogate()
            allowFrontSurrogate = true
            allowEndSurrogate = true
            when {
                surrogateAtFront && surrogateAtEnd -> {
                    // Both surrogates - just exchange them.
                    array[end] = frontTrailingChar
                    array[end - 1] = frontLeadingChar
                    array[front] = endLeadingChar
                    array[front + 1] = endTrailingChar
                    frontLeadingChar = array[front + 2]
                    endTrailingChar = array[end - 2]
                    front++
                    end--
                }
                !surrogateAtFront && !surrogateAtEnd -> {
                    // Neither surrogates - exchange only front/end.
                    array[end] = frontLeadingChar
                    array[front] = endTrailingChar
                    frontLeadingChar = frontTrailingChar
                    endTrailingChar = endLeadingChar
                }
                surrogateAtFront && !surrogateAtEnd -> {
                    // Surrogate only at the front -
                    // move the low part, the high part will be moved as a usual character on the next iteration.
                    array[end] = frontTrailingChar
                    array[front] = endTrailingChar
                    endTrailingChar = endLeadingChar
                    allowFrontSurrogate = false
                }
                !surrogateAtFront && surrogateAtEnd -> {
                    // Surrogate only at the end -
                    // move the high part, the low part will be moved as a usual character on the next iteration.
                    array[end] = frontLeadingChar
                    array[front] = endLeadingChar
                    frontLeadingChar = frontTrailingChar
                    allowEndSurrogate = false
                }
            }
            front++
            end--
        }
        if (_length % 2 == 1 && (!allowEndSurrogate || !allowFrontSurrogate)) {
            array[end] = if (allowFrontSurrogate) endTrailingChar else frontLeadingChar
        }
        return this
    }

    /**
     * Appends the string representation of the specified object [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    actual fun append(value: Any?): StringBuilder = append(value.toString())

    /**
     * Appends the string representation of the specified boolean [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    // TODO: optimize those!
    actual fun append(value: Boolean): StringBuilder = append(value.toString())
    fun append(value: Byte): StringBuilder = append(value.toString())
    fun append(value: Short): StringBuilder = append(value.toString())
    fun append(value: Int): StringBuilder {
        ensureExtraCapacity(11)
        _length += insertInt(array, _length, value)
        return this
    }
    fun append(value: Long): StringBuilder = append(value.toString())
    fun append(value: Float): StringBuilder = append(value.toString())
    fun append(value: Double): StringBuilder = append(value.toString())

    /**
     * Appends characters in the specified character array [value] to this string builder and returns this instance.
     *
     * Characters are appended in order, starting at the index 0.
     */
    actual fun append(value: CharArray): StringBuilder {
        ensureExtraCapacity(value.size)
        value.copyInto(array, _length)
        _length += value.size
        return this
    }

    @Deprecated("Provided for binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun append(value: String): StringBuilder = append(value)

    /**
     * Appends the specified string [value] to this string builder and returns this instance.
     *
     * If [value] is `null`, then the four characters `"null"` are appended.
     */
    actual fun append(value: String?): StringBuilder {
        val toAppend = value ?: "null"
        ensureExtraCapacity(toAppend.length)
        _length += insertString(array, _length, toAppend)
        return this
    }

    /**
     * Returns the current capacity of this string builder.
     *
     * The capacity is the maximum length this string builder can have before an allocation occurs.
     */
    actual fun capacity(): Int = array.size

    /**
     * Ensures that the capacity of this string builder is at least equal to the specified [minimumCapacity].
     *
     * If the current capacity is less than the [minimumCapacity], a new backing storage is allocated with greater capacity.
     * Otherwise, this method takes no action and simply returns.
     */
    actual fun ensureCapacity(minimumCapacity: Int) {
        if (minimumCapacity > array.size) {
            var newSize = array.size * 2 + 2
            if (minimumCapacity > newSize)
                newSize = minimumCapacity
            array = array.copyOf(newSize)
        }
    }

    /**
     * Returns the index within this string builder of the first occurrence of the specified [string].
     *
     * Returns `-1` if the specified [string] does not occur in this string builder.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    actual fun indexOf(string: String): Int {
        return (this as CharSequence).indexOf(string, startIndex = 0, ignoreCase = false)
    }

    /**
     * Returns the index within this string builder of the first occurrence of the specified [string],
     * starting at the specified [startIndex].
     *
     * Returns `-1` if the specified [string] does not occur in this string builder starting at the specified [startIndex].
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    actual fun indexOf(string: String, startIndex: Int): Int {
        if (string.isEmpty() && startIndex >= _length) return _length
        return (this as CharSequence).indexOf(string, startIndex, ignoreCase = false)
    }

    /**
     * Returns the index within this string builder of the last occurrence of the specified [string].
     * The last occurrence of empty string `""` is considered to be at the index equal to `this.length`.
     *
     * Returns `-1` if the specified [string] does not occur in this string builder.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    actual fun lastIndexOf(string: String): Int {
        if (string.isEmpty()) return _length
        return (this as CharSequence).lastIndexOf(string, startIndex = lastIndex, ignoreCase = false)
    }

    /**
     * Returns the index within this string builder of the last occurrence of the specified [string],
     * starting from the specified [startIndex] toward the beginning.
     *
     * Returns `-1` if the specified [string] does not occur in this string builder starting at the specified [startIndex].
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    actual fun lastIndexOf(string: String, startIndex: Int): Int {
        if (string.isEmpty() && startIndex >= _length) return _length
        return (this as CharSequence).lastIndexOf(string, startIndex, ignoreCase = false)
    }

    /**
     * Inserts the string representation of the specified boolean [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    // TODO: optimize those!
    actual fun insert(index: Int, value: Boolean): StringBuilder = insert(index, value.toString())
    fun insert(index: Int, value: Byte)    = insert(index, value.toString())
    fun insert(index: Int, value: Short)   = insert(index, value.toString())
    fun insert(index: Int, value: Int)     = insert(index, value.toString())
    fun insert(index: Int, value: Long)    = insert(index, value.toString())
    fun insert(index: Int, value: Float)   = insert(index, value.toString())
    fun insert(index: Int, value: Double)  = insert(index, value.toString())

    /**
     * Inserts the specified character [value] into this string builder at the specified [index] and returns this instance.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    actual fun insert(index: Int, value: Char): StringBuilder {
        checkInsertIndex(index)
        ensureExtraCapacity(1)
        val newLastIndex = lastIndex + 1
        for (i in newLastIndex downTo index + 1) {
            array[i] = array[i - 1]
        }
        array[index] = value
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
    actual fun insert(index: Int, value: CharArray): StringBuilder {
        checkInsertIndex(index)
        ensureExtraCapacity(value.size)

        array.copyInto(array, startIndex = index, endIndex = _length, destinationOffset = index + value.size)
        value.copyInto(array, destinationOffset = index)

        _length += value.size
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
    actual fun insert(index: Int, value: CharSequence?): StringBuilder {
        // Kotlin/JVM inserts the "null" string if the argument is null.
        val toInsert = value ?: "null"
        return insertRange(index, toInsert, 0, toInsert.length)
    }

    /**
     * Inserts the string representation of the specified object [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    actual fun insert(index: Int, value: Any?): StringBuilder = insert(index, value.toString())

    @Deprecated("Provided for binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun insert(index: Int, value: String): StringBuilder = insert(index, value)

    /**
     * Inserts the string [value] into this string builder at the specified [index] and returns this instance.
     *
     * If [value] is `null`, then the four characters `"null"` are inserted.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    actual fun insert(index: Int, value: String?): StringBuilder {
        val toInsert = value ?: "null"
        checkInsertIndex(index)
        ensureExtraCapacity(toInsert.length)
        array.copyInto(array, startIndex = index, endIndex = _length, destinationOffset = index + toInsert.length)
        _length += insertString(array, index, toInsert)
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
    actual fun setLength(newLength: Int) {
        if (newLength < 0) {
            throw IllegalArgumentException("Negative new length: $newLength.")
        }

        if (newLength > _length) {
            array.fill('\u0000', _length, newLength.coerceAtMost(array.size))
        }
        _length = newLength
    }

    /**
     * Returns a new [String] that contains characters in this string builder at [startIndex] (inclusive) and up to the [endIndex] (exclusive).
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
     */
    actual fun substring(startIndex: Int, endIndex: Int): String {
        checkBoundsIndexes(startIndex, endIndex, _length)
        return unsafeStringFromCharArray(array, startIndex, endIndex - startIndex)
    }

    /**
     * Returns a new [String] that contains characters in this string builder at [startIndex] (inclusive) and up to the [length] (exclusive).
     *
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    actual fun substring(startIndex: Int): String {
        return substring(startIndex, _length)
    }

    /**
     * Attempts to reduce storage used for this string builder.
     *
     * If the backing storage of this string builder is larger than necessary to hold its current contents,
     * then it may be resized to become more space efficient.
     * Calling this method may, but is not required to, affect the value of the [capacity] property.
     */
    actual fun trimToSize() {
        if (_length < array.size)
            array = array.copyOf(_length)
    }

    override fun toString(): String = unsafeStringFromCharArray(array, 0, _length)

    /**
     * Sets the character at the specified [index] to the specified [value].
     *
     * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
     */
    operator fun set(index: Int, value: Char) {
        checkIndex(index)
        array[index] = value
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
    @WasExperimental(ExperimentalStdlibApi::class)
    fun setRange(startIndex: Int, endIndex: Int, value: String): StringBuilder {
        checkReplaceRange(startIndex, endIndex, _length)

        val coercedEndIndex = endIndex.coerceAtMost(_length)
        val lengthDiff = value.length - (coercedEndIndex - startIndex)
        ensureExtraCapacity(_length + lengthDiff)
        array.copyInto(array, startIndex = coercedEndIndex, endIndex = _length, destinationOffset = startIndex + value.length)
        var replaceIndex = startIndex
        for (index in 0 until value.length) array[replaceIndex++] = value[index] // optimize
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
    @WasExperimental(ExperimentalStdlibApi::class)
    fun deleteAt(index: Int): StringBuilder {
        checkIndex(index)
        array.copyInto(array, startIndex = index + 1, endIndex = _length, destinationOffset = index)
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
    @WasExperimental(ExperimentalStdlibApi::class)
    fun deleteRange(startIndex: Int, endIndex: Int): StringBuilder {
        checkReplaceRange(startIndex, endIndex, _length)

        val coercedEndIndex = endIndex.coerceAtMost(_length)
        array.copyInto(array, startIndex = coercedEndIndex, endIndex = _length, destinationOffset = startIndex)
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
    @WasExperimental(ExperimentalStdlibApi::class)
    fun toCharArray(destination: CharArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = this.length) {
        checkBoundsIndexes(startIndex, endIndex, _length)
        checkBoundsIndexes(destinationOffset, destinationOffset + endIndex - startIndex, destination.size)

        array.copyInto(destination, destinationOffset, startIndex, endIndex)
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
    @WasExperimental(ExperimentalStdlibApi::class)
    fun appendRange(value: CharArray, startIndex: Int, endIndex: Int): StringBuilder {
        checkBoundsIndexes(startIndex, endIndex, value.size)
        val extraLength = endIndex - startIndex
        ensureExtraCapacity(extraLength)
        value.copyInto(array, _length, startIndex, endIndex)
        _length += extraLength
        return this
    }

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
    @WasExperimental(ExperimentalStdlibApi::class)
    fun appendRange(value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder {
        checkBoundsIndexes(startIndex, endIndex, value.length)
        val extraLength = endIndex - startIndex
        ensureExtraCapacity(extraLength)
        (value as? String)?.let {
            _length += insertString(array, _length, it, startIndex, extraLength)
            return this
        }
        var index = startIndex
        while (index < endIndex)
            array[_length++] = value[index++]
        return this
    }

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
    @WasExperimental(ExperimentalStdlibApi::class)
    fun insertRange(index: Int, value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder {
        checkBoundsIndexes(startIndex, endIndex, value.length)
        checkInsertIndex(index)
        val extraLength = endIndex - startIndex
        ensureExtraCapacity(extraLength)

        array.copyInto(array, startIndex = index, endIndex = _length, destinationOffset = index + extraLength)
        var from = startIndex
        var to = index
        while (from < endIndex) {
            array[to++] = value[from++]
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
    @WasExperimental(ExperimentalStdlibApi::class)
    fun insertRange(index: Int, value: CharArray, startIndex: Int, endIndex: Int): StringBuilder {
        checkInsertIndex(index)
        checkBoundsIndexes(startIndex, endIndex, value.size)

        val extraLength = endIndex - startIndex
        array.copyInto(array, startIndex = index, endIndex = _length, destinationOffset = index + extraLength)
        value.copyInto(array, startIndex = startIndex, endIndex = endIndex, destinationOffset = index)

        _length += extraLength
        return this
    }

    // ---------------------------- private ----------------------------

    private fun ensureExtraCapacity(n: Int) {
        ensureCapacity(_length + n)
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= _length) throw IndexOutOfBoundsException()
    }

    private fun checkInsertIndex(index: Int) {
        if (index < 0 || index > _length) throw IndexOutOfBoundsException()
    }

    private fun checkInsertIndexFrom(index: Int, fromIndex: Int) {
        if (index < fromIndex || index > _length) throw IndexOutOfBoundsException()
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
 * Clears the content of this string builder making it empty.
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
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
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
@WasExperimental(ExperimentalStdlibApi::class)
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
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
@WasExperimental(ExperimentalStdlibApi::class)
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.deleteAt(index: Int): StringBuilder = this.deleteAt(index)

/**
 * Removes characters in the specified range from this string builder and returns this instance.
 *
 * @param startIndex the beginning (inclusive) of the range to remove.
 * @param endIndex the end (exclusive) of the range to remove.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
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
@WasExperimental(ExperimentalStdlibApi::class)
@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.toCharArray(destination: CharArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = this.length) =
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
@WasExperimental(ExperimentalStdlibApi::class)
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
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
@WasExperimental(ExperimentalStdlibApi::class)
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
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
@WasExperimental(ExperimentalStdlibApi::class)
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
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
@WasExperimental(ExperimentalStdlibApi::class)
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.insertRange(index: Int, value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder =
        this.insertRange(index, value, startIndex, endIndex)

// Method parameters renamings
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use append(value: Boolean) instead", ReplaceWith("append(value = it)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.append(it: Boolean): StringBuilder = this.append(value = it)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use append(value: Byte) instead", ReplaceWith("append(value = it)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.append(it: Byte): StringBuilder = this.append(value = it)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use append(value: Short) instead", ReplaceWith("append(value = it)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.append(it: Short): StringBuilder = this.append(value = it)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use append(value: Int) instead", ReplaceWith("append(value = it)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.append(it: Int): StringBuilder = this.append(value = it)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use append(value: Long) instead", ReplaceWith("append(value = it)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.append(it: Long): StringBuilder = this.append(value = it)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use append(value: Float) instead", ReplaceWith("append(value = it)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.append(it: Float): StringBuilder = this.append(value = it)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use append(value: Double) instead", ReplaceWith("append(value = it)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.append(it: Double): StringBuilder = this.append(value = it)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use append(value: String) instead", ReplaceWith("append(value = it)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.append(it: String): StringBuilder = this.append(value = it)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use append(value: CharArray) instead", ReplaceWith("append(value = it)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.append(it: CharArray): StringBuilder = this.append(value = it)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use ensureCapacity(minimumCapacity: Int) instead", ReplaceWith("ensureCapacity(minimumCapacity = capacity)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.ensureCapacity(capacity: Int): Unit = this.ensureCapacity(minimumCapacity = capacity)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use insert(index: Int, value: Char) instead", ReplaceWith("insert(index, value = c)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.insert(index: Int, c: Char): StringBuilder = this.insert(index, value = c)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use insert(index: Int, value: CharArray) instead", ReplaceWith("insert(index, value = chars)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.insert(index: Int, chars: CharArray): StringBuilder = this.insert(index, value = chars)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use insert(index: Int, value: CharSequence?) instead", ReplaceWith("insert(index, value = csq)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.insert(index: Int, csq: CharSequence?): StringBuilder = this.insert(index, value = csq)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use insert(index: Int, value: String) instead", ReplaceWith("insert(index, value = string)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.insert(index: Int, string: String): StringBuilder = this.insert(index, value = string)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use setLength(newLength: Int) instead", ReplaceWith("setLength(newLength = l)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.setLength(l: Int) = this.setLength(newLength = l)

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
@Deprecated(
        "Use insertRange(index: Int, csq: CharSequence, start: Int, end: Int) instead",
        ReplaceWith("insertRange(index, csq ?: \"null\", start, end)"),
        DeprecationLevel.WARNING
)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.insert(index: Int, csq: CharSequence?, start: Int, end: Int): StringBuilder =
        this.insertRange(index, csq ?: "null", start, end)

@Deprecated("Use set(index: Int, value: Char) instead", ReplaceWith("set(index, value)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.setCharAt(index: Int, value: Char) = this.set(index, value)

/**
 * Removes the character at the specified [index] from this string builder and returns this instance.
 *
 * If the `Char` at the specified [index] is part of a supplementary code point, this method does not remove the entire supplementary character.
 *
 * @param index the index of `Char` to remove.
 *
 * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
 */
@Deprecated("Use deleteAt(index: Int) instead", ReplaceWith("deleteAt(index)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.deleteCharAt(index: Int) = this.deleteAt(index)
