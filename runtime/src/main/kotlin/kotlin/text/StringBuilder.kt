/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.text

// ByteArray -> String (UTF-8 -> UTF-16)

@Deprecated("Use ByteArray.stringFromUtf8()", ReplaceWith("array.stringFromUtf8(start, end)"))
fun fromUtf8Array(array: ByteArray, start: Int, size: Int) = array.stringFromUtf8Impl(start, size)

@Deprecated("Use String.toUtf8()", ReplaceWith("string.toUtf8(start, end)"))
fun toUtf8Array(string: String, start: Int, size: Int) : ByteArray = string.toUtf8(start, size)

/**
 * Converts an UTF-8 array into a [String]. Replaces invalid input sequences with a default character.
 */
fun ByteArray.stringFromUtf8(start: Int = 0, size: Int = this.size) : String =
        stringFromUtf8Impl(start, size)

@SymbolName("Kotlin_ByteArray_stringFromUtf8")
private external fun ByteArray.stringFromUtf8Impl(start: Int, size: Int) : String

/**
 * Converts an UTF-8 array into a [String]. Throws [IllegalCharacterConversionException] if the input is invalid.
 */
fun ByteArray.stringFromUtf8OrThrow(start: Int = 0, size: Int = this.size) : String =
        stringFromUtf8OrThrowImpl(start, size)

@SymbolName("Kotlin_ByteArray_stringFromUtf8OrThrow")
private external fun ByteArray.stringFromUtf8OrThrowImpl(start: Int, size: Int) : String

// String -> ByteArray (UTF-16 -> UTF-8)
/**
 * Converts a [String] into an UTF-8 array. Replaces invalid input sequences with a default character.
 */
fun String.toUtf8(start: Int = 0, size: Int = this.length) : ByteArray =
        toUtf8Impl(start, size)

@SymbolName("Kotlin_String_toUtf8")
private external fun String.toUtf8Impl(start: Int, size: Int) : ByteArray

/**
 * Converts a [String] into an UTF-8 array. Throws [IllegalCharacterConversionException] if the input is invalid.
 */
fun String.toUtf8OrThrow(start: Int = 0, size: Int = this.length) : ByteArray =
        toUtf8OrThrowImpl(start, size)

@SymbolName("Kotlin_String_toUtf8OrThrow")
private external fun String.toUtf8OrThrowImpl(start: Int, size: Int) : ByteArray

// TODO: make it somewhat private?
@SymbolName("Kotlin_String_fromCharArray")
external fun fromCharArray(array: CharArray, start: Int, size: Int) : String

@SymbolName("Kotlin_StringBuilder_insertString")
private external fun insertString(array: CharArray, start: Int, value: String): Int

@SymbolName("Kotlin_StringBuilder_insertInt")
private external fun insertInt(array: CharArray, start: Int, value: Int): Int

/**
 * Sets the character at the specified [index] to the specified [value].
 */
@kotlin.internal.InlineOnly
public inline operator fun StringBuilder.set(index: Int, value: Char): Unit = this.setCharAt(index, value)

actual class StringBuilder private constructor (
        private var array: CharArray
) : CharSequence, Appendable {
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

    override fun toString(): String = fromCharArray(array, 0, _length)

    fun substring(startIndex: Int, endIndex: Int): String {
        checkInsertIndex(startIndex)
        checkInsertIndexFrom(endIndex, startIndex)
        return fromCharArray(array, startIndex, endIndex - startIndex)
    }

    fun trimToSize() {
        if (_length < array.size)
            array = array.copyOf(_length)
    }

    fun ensureCapacity(capacity: Int) {
        if (capacity > array.size) {
            var newSize = array.size * 3 / 2
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

        array.copyRangeTo(array, index, _length, index + extraLength)
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

        array.copyRangeTo(array, index, _length, index + chars.size)
        chars.copyRangeTo(array, 0, chars.size, index)

        _length += chars.size
        return this
    }

    fun insert(index: Int, string: String): StringBuilder {
        checkInsertIndex(index)
        ensureExtraCapacity(string.length)
        array.copyRangeTo(array, index, _length, index + string.length)
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
        var index = start
        while (index < end)
            array[_length++] = toAppend[index++]
        return this
    }

    fun append(it: CharArray): StringBuilder {
        ensureExtraCapacity(it.size)
        it.copyRangeTo(array, 0, it.size, _length)
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
        array.copyRangeTo(array, index + 1, _length, index)
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
