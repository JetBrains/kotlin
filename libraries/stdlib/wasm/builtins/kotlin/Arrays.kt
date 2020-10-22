/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*

public class ByteArray(size: Int) {
    private var jsArray: WasmExternRef = JsArray_new(size)

    init {
        JsArray_fill_Byte(jsArray, size) { 0 }
    }

    public constructor(size: Int, init: (Int) -> Byte) : this(size) {
        jsArray = JsArray_new(size)
        JsArray_fill_Byte(jsArray, size, init)
    }

    public operator fun get(index: Int): Byte =
        JsArray_get_Byte(jsArray, index)

    public operator fun set(index: Int, value: Byte) {
        JsArray_set_Byte(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): ByteIterator = byteArrayIterator(this)
}

internal fun byteArrayIterator(array: ByteArray) = object : ByteIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextByte() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class CharArray(size: Int) {
    private var jsArray: WasmExternRef = JsArray_new(size)

    init {
        JsArray_fill_Char(jsArray, size) { 0.toChar() }
    }

    public constructor(size: Int, init: (Int) -> Char) : this(size) {
        jsArray = JsArray_new(size)
        JsArray_fill_Char(jsArray, size, init)
    }

    public operator fun get(index: Int): Char =
        JsArray_get_Char(jsArray, index)

    public operator fun set(index: Int, value: Char) {
        JsArray_set_Char(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): CharIterator = charArrayIterator(this)
}

internal fun charArrayIterator(array: CharArray) = object : CharIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextChar() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class ShortArray(size: Int) {
    private var jsArray: WasmExternRef = JsArray_new(size)

    init {
        JsArray_fill_Short(jsArray, size) { 0 }
    }

    public constructor(size: Int, init: (Int) -> Short) : this(size) {
        JsArray_fill_Short(jsArray, size, init)
    }

    public operator fun get(index: Int): Short =
        JsArray_get_Short(jsArray, index)

    public operator fun set(index: Int, value: Short) {
        JsArray_set_Short(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): ShortIterator = shortArrayIterator(this)
}

internal fun shortArrayIterator(array: ShortArray) = object : ShortIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextShort() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class IntArray(size: Int) {
    private var jsArray: WasmExternRef = JsArray_new(size)

    init {
        JsArray_fill_Int(jsArray, size) { 0 }
    }

    public constructor(size: Int, init: (Int) -> Int) : this(size) {
        JsArray_fill_Int(jsArray, size, init)
    }

    public operator fun get(index: Int): Int =
        JsArray_get_Int(jsArray, index)

    public operator fun set(index: Int, value: Int) {
        JsArray_set_Int(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): IntIterator = intArrayIterator(this)
}

internal fun intArrayIterator(array: IntArray) = object : IntIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextInt() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class LongArray(size: Int) {
    private var jsArray: WasmExternRef = JsArray_new(size)

    init {
        JsArray_fill_Long(jsArray, size) { 0L }
    }

    public constructor(size: Int, init: (Int) -> Long) : this(size) {
        JsArray_fill_Long(jsArray, size, init)
    }

    public operator fun get(index: Int): Long =
        JsArray_get_Long(jsArray, index)

    public operator fun set(index: Int, value: Long) {
        JsArray_set_Long(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): LongIterator = longArrayIterator(this)
}

internal fun longArrayIterator(array: LongArray) = object : LongIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextLong() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class FloatArray(size: Int) {
    private var jsArray: WasmExternRef = JsArray_new(size)

    init {
        JsArray_fill_Float(jsArray, size) { 0.0f }
    }

    public constructor(size: Int, init: (Int) -> Float) : this(size) {
        JsArray_fill_Float(jsArray, size, init)
    }

    public operator fun get(index: Int): Float =
        JsArray_get_Float(jsArray, index)

    public operator fun set(index: Int, value: Float) {
        JsArray_set_Float(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): FloatIterator = floatArrayIterator(this)
}

internal fun floatArrayIterator(array: FloatArray) = object : FloatIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextFloat() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class DoubleArray(size: Int) {
    private var jsArray: WasmExternRef = JsArray_new(size)

    init {
        JsArray_fill_Double(jsArray, size) { 0.0 }
    }

    public constructor(size: Int, init: (Int) -> Double) : this(size) {
        JsArray_fill_Double(jsArray, size, init)
    }

    public operator fun get(index: Int): Double =
        JsArray_get_Double(jsArray, index)

    public operator fun set(index: Int, value: Double) {
        JsArray_set_Double(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): DoubleIterator = doubleArrayIterator(this)
}

internal fun doubleArrayIterator(array: DoubleArray) = object : DoubleIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextDouble() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class BooleanArray(size: Int) {
    private var jsArray: WasmExternRef = JsArray_new(size)

    init {
        JsArray_fill_Boolean(jsArray, size) { false }
    }

    public constructor(size: Int, init: (Int) -> Boolean) : this(size) {
        JsArray_fill_Boolean(jsArray, size, init)
    }

    public operator fun get(index: Int): Boolean =
        JsArray_get_Boolean(jsArray, index)

    public operator fun set(index: Int, value: Boolean) {
        JsArray_set_Boolean(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): BooleanIterator = booleanArrayIterator(this)
}

internal fun booleanArrayIterator(array: BooleanArray) = object : BooleanIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextBoolean() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}
