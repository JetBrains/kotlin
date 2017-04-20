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

@SymbolName("Kotlin_String_fromUtf8Array")
external fun fromUtf8Array(array: ByteArray, start: Int, size: Int) : String

@SymbolName("Kotlin_String_toUtf8Array")
external fun toUtf8Array(string: String, start: Int, size: Int) : ByteArray

// TODO: make it somewhat private?
@SymbolName("Kotlin_String_fromCharArray")
external fun fromCharArray(array: CharArray, start: Int, size: Int) : String

@SymbolName("Kotlin_String_toCharArray")
external fun toCharArray(string: String) : CharArray

/**
 * Builds new string by populating newly created [StringBuilder] using provided [builderAction]
 * and then converting it to [String].
 */
@kotlin.internal.InlineOnly
public inline fun buildString(builderAction: StringBuilder.() -> Unit): String =
        StringBuilder().apply(builderAction).toString()

/**
 * Builds new string by populating newly created [StringBuilder] initialized with the given [capacity]
 * using provided [builderAction] and then converting it to [String].
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun buildString(capacity: Int, builderAction: StringBuilder.() -> Unit): String =
        StringBuilder(capacity).apply(builderAction).toString()

/**
 * Sets the character at the specified [index] to the specified [value].
 */
@kotlin.internal.InlineOnly
public inline operator fun StringBuilder.set(index: Int, value: Char): Unit = this.setCharAt(index, value)

class StringBuilder private constructor (
        private var array: CharArray
) : CharSequence, Appendable {
    constructor() : this(10)

    constructor(capacity: Int) : this(CharArray(capacity))

    constructor(string: String) : this(toCharArray(string)) {
        length = array.size
    }

    constructor(sequence: CharSequence): this(sequence.length) {
        append(sequence)
    }

    override var length: Int = 0
        set(capacity) {
            ensureCapacity(capacity)
            field = capacity
        }

    override fun get(index: Int): Char {
        checkIndex(index)
        return array[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = substring(startIndex, endIndex)

    override fun toString(): String = fromCharArray(array, 0, length)

    fun substring(startIndex: Int, endIndex: Int): String {
        checkInsertIndex(startIndex)
        checkInsertIndexFrom(endIndex, startIndex)
        return fromCharArray(array, startIndex, endIndex - startIndex)
    }

    fun trimToSize() {
        if (length < array.size)
            array = array.copyOf(length)
    }

    fun ensureCapacity(capacity: Int) {
        if (capacity > array.size) {
            var newSize = array.size * 3 / 2
            if (capacity > newSize)
                newSize = capacity
            array = array.copyOf(newSize)
        }
    }

    fun reverse(): StringBuilder {
        var front = 0
        var end = array.lastIndex
        var hasSurrogates = false
        while (front < end) {
            if (array[front].isSurrogate() || array[end].isSurrogate()) {
                hasSurrogates = true
            }
            val tmp = array[front]
            array[front] = array[end]
            array[end] = tmp
            front++
            end--
        }

        // Reverse surrogate pairs back.
        if (hasSurrogates) {
            var i = 0
            while (i < length - 1) {
                if (array[i].isLowSurrogate() && array[i + 1].isHighSurrogate()) {
                    val tmp = array[i]
                    array[i] = array[i + 1]
                    array[i + 1] = tmp
                    i++
                }
                i++
            }
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
        length++
        return this
    }

    fun insert(index: Int, csq: CharSequence?): StringBuilder {
        // Kotlin JVM inserts the "null" string if the argument is null.
        val toInsert = csq ?: "null"
        return insert(index, toInsert, 0, toInsert.length)
    }

    fun insert(index: Int, csq: CharSequence?, start: Int, end: Int): StringBuilder {
        // Kotlin JVM processes null as if the argument was "null" char sequence.
        val toInsert = csq ?: "null"
        if (start < 0 || end < start || start > toInsert.length) throw IndexOutOfBoundsException()
        checkInsertIndex(index)

        val extraLength = end - start
        ensureExtraCapacity(extraLength)
        var from = lastIndex
        var to = length + extraLength - 1
        while (from >= index) {
            array[to--] = array[from--]
        }

        from = start
        to = index
        while (from < end) {
            array[to++] = toInsert[from++]
        }

        length += extraLength
        return this
    }

    fun insert(index: Int, chars: CharArray): StringBuilder {
        checkInsertIndex(index)
        ensureExtraCapacity(chars.size)

        var from = lastIndex
        var to = length + chars.size - 1
        while (from >= index) {
            array[to--] = array[from--]
        }

        var i = index
        for (c in chars) {
            array[i++] = c
        }

        length += chars.size
        return this
    }

    fun insert(index: Int, string: String): StringBuilder = insert(index, toCharArray(string))

    fun insert(index: Int, value: Boolean) = insert(index, value.toString())
    fun insert(index: Int, value: Byte)    = insert(index, value.toString())
    fun insert(index: Int, value: Short)   = insert(index, value.toString())
    fun insert(index: Int, value: Int)     = insert(index, value.toString())
    fun insert(index: Int, value: Long)    = insert(index, value.toString())
    fun insert(index: Int, value: Float)   = insert(index, value.toString())
    fun insert(index: Int, value: Double)  = insert(index, value.toString())
    fun insert(index: Int, value: Any?)    = insert(index, value.toString())


    // Of Appenable.
    override fun append(c: Char) : StringBuilder {
        ensureExtraCapacity(1)
        array[length++] = c
        return this
    }

    override fun append(csq: CharSequence?): StringBuilder {
        // Kotlin JVM processes null as if the argument was "null" char sequence.
        val toAppend = csq ?: "null"
        return append(toAppend, 0, toAppend.length)
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder {
        // Kotlin JVM processes null as if the argument was "null" char sequence.
        val toAppend = csq ?: "null"
        if (start < 0 || end < start || start > toAppend.length) throw IndexOutOfBoundsException()
        ensureExtraCapacity(end - start)
        var index = start
        while (index < end)
            array[length++] = toAppend[index++]
        return this
    }

    fun append(it: CharArray): StringBuilder {
        ensureExtraCapacity(it.size)
        for (c in it)
            array[length++] = c
        return this
    }

    fun append(it: String): StringBuilder {
        ensureExtraCapacity(it.length)
        for (c in toCharArray(it))
            array[length++] = c
        return this
    }

    fun append(it: Boolean) = append(it.toString())
    fun append(it: Byte) = append(it.toString())
    fun append(it: Short) = append(it.toString())
    fun append(it: Int) = append(it.toString())
    fun append(it: Long) = append(it.toString())
    fun append(it: Float) = append(it.toString())
    fun append(it: Double) = append(it.toString())
    fun append(it: Any?) = append(it.toString())

    fun deleteCharAt(index: Int) {
        checkIndex(index)
        array.copyRangeTo(array, index + 1, length, index)
        --length
    }

    fun setCharAt(index: Int, value: Char) {
        checkIndex(index)
        array[index] = value
    }

    // ---------------------------- private ----------------------------

    private fun ensureExtraCapacity(n: Int) {
        ensureCapacity(length + n)
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= length) throw IndexOutOfBoundsException()
    }

    private fun checkInsertIndex(index: Int) {
        if (index < 0 || index > length) throw IndexOutOfBoundsException()
    }

    private fun checkInsertIndexFrom(index: Int, fromIndex: Int) {
        if (index < fromIndex || index > length) throw IndexOutOfBoundsException()
    }
}
