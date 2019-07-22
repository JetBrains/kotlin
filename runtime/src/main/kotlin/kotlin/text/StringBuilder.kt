/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

/**
 * Clears the content of this string builder making it empty.
 *
 * @sample samples.text.Strings.clearStringBuilder
 */
@SinceKotlin("1.3")
public actual fun StringBuilder.clear(): StringBuilder = apply { setLength(0) }

/**
 * Sets the character at the specified [index] to the specified [value].
 */
@kotlin.internal.InlineOnly
public inline operator fun StringBuilder.set(index: Int, value: Char): Unit = this.setCharAt(index, value)

actual class StringBuilder private constructor (
        private var array: CharArray) : CharSequence, Appendable {

    actual constructor() : this(10)

    actual constructor(capacity: Int) : this(CharArray(capacity))

    constructor(string: String) : this(string.toCharArray()) {
        _length = array.size
    }

    actual constructor(content: CharSequence): this(content.length) {
        append(content)
    }

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

    fun setLength(l: Int) {
        _length = l
    }

    actual override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = substring(startIndex, endIndex)

    override fun toString(): String = unsafeStringFromCharArray(array, 0, _length)

    fun substring(startIndex: Int, endIndex: Int): String {
        checkInsertIndex(startIndex)
        checkInsertIndexFrom(endIndex, startIndex)
        return unsafeStringFromCharArray(array, startIndex, endIndex - startIndex)
    }

    fun trimToSize() {
        if (_length < array.size)
            array = array.copyOf(_length)
    }

    fun ensureCapacity(capacity: Int) {
        if (capacity > array.size) {
            var newSize = array.size * 2 + 2
            if (capacity > newSize)
                newSize = capacity
            array = array.copyOf(newSize)
        }
    }

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

    fun insert(index: Int, c: Char): StringBuilder {
        checkInsertIndex(index)
        ensureExtraCapacity(1)
        val newLastIndex = lastIndex + 1
        for (i in newLastIndex downTo index + 1) {
            array[i] = array[i - 1]
        }
        array[index] = c
        _length++
        return this
    }

    fun insert(index: Int, csq: CharSequence?): StringBuilder {
        // Kotlin/JVM inserts the "null" string if the argument is null.
        val toInsert = csq ?: "null"
        return insert(index, toInsert, 0, toInsert.length)
    }

    fun insert(index: Int, csq: CharSequence?, start: Int, end: Int): StringBuilder {
        // Kotlin/JVM processes null as if the argument was "null" char sequence.
        val toInsert = csq ?: "null"
        if (start < 0 || end < start || start > toInsert.length) throw IndexOutOfBoundsException()
        checkInsertIndex(index)
        val extraLength = end - start
        ensureExtraCapacity(extraLength)

        array.copyInto(array, startIndex = index, endIndex = _length, destinationOffset = index + extraLength)
        var from = start
        var to = index
        while (from < end) {
            array[to++] = toInsert[from++]
        }

        _length += extraLength
        return this
    }

    fun insert(index: Int, chars: CharArray): StringBuilder {
        checkInsertIndex(index)
        ensureExtraCapacity(chars.size)

        array.copyInto(array, startIndex = index, endIndex = _length, destinationOffset = index + chars.size)
        chars.copyInto(array, destinationOffset = index)

        _length += chars.size
        return this
    }

    fun insert(index: Int, string: String): StringBuilder {
        checkInsertIndex(index)
        ensureExtraCapacity(string.length)
        array.copyInto(array, startIndex = index, endIndex = _length, destinationOffset = index + string.length)
        _length += insertString(array, index, string)
        return this
    }

    // TODO: optimize those!
    fun insert(index: Int, value: Boolean) = insert(index, value.toString())
    fun insert(index: Int, value: Byte)    = insert(index, value.toString())
    fun insert(index: Int, value: Short)   = insert(index, value.toString())
    fun insert(index: Int, value: Int)     = insert(index, value.toString())
    fun insert(index: Int, value: Long)    = insert(index, value.toString())
    fun insert(index: Int, value: Float)   = insert(index, value.toString())
    fun insert(index: Int, value: Double)  = insert(index, value.toString())
    fun insert(index: Int, value: Any?)    = insert(index, value.toString())


    // Of Appenable.
    actual override fun append(c: Char) : StringBuilder {
        ensureExtraCapacity(1)
        array[_length++] = c
        return this
    }

    actual override fun append(csq: CharSequence?): StringBuilder {
        // Kotlin/JVM processes null as if the argument was "null" char sequence.
        val toAppend = csq ?: "null"
        return append(toAppend, 0, toAppend.length)
    }

    actual override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder {
        // Kotlin/JVM processes null as if the argument was "null" char sequence.
        val toAppend = csq ?: "null"
        if (start < 0 || end < start || start > toAppend.length) throw IndexOutOfBoundsException()
        ensureExtraCapacity(end - start)
        (toAppend as? String)?.let {
            _length += insertString(array, _length, it, start, end - start)
            return this
        }
        var index = start
        while (index < end)
            array[_length++] = toAppend[index++]
        return this
    }

    fun append(it: CharArray): StringBuilder {
        ensureExtraCapacity(it.size)
        it.copyInto(array, _length)
        _length += it.size
        return this
    }

    fun append(it: String): StringBuilder {
        ensureExtraCapacity(it.length)
        _length += insertString(array, _length, it)
        return this
    }

    // TODO: optimize those!
    fun append(it: Boolean) = append(it.toString())
    fun append(it: Byte) = append(it.toString())
    fun append(it: Short) = append(it.toString())
    fun append(it: Int): StringBuilder {
        ensureExtraCapacity(11)
        _length += insertInt(array, _length, it)
        return this
    }
    fun append(it: Long) = append(it.toString())
    fun append(it: Float) = append(it.toString())
    fun append(it: Double) = append(it.toString())
    actual fun append(obj: Any?): StringBuilder = append(obj.toString())

    fun deleteCharAt(index: Int) {
        checkIndex(index)
        array.copyInto(array, startIndex = index + 1, endIndex = _length, destinationOffset = index)
        --_length
    }

    fun setCharAt(index: Int, value: Char) {
        checkIndex(index)
        array[index] = value
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
}
