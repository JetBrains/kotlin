/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

@file:Suppress(
    "NON_ABSTRACT_FUNCTION_WITH_NO_BODY",
    "MUST_BE_INITIALIZED_OR_BE_ABSTRACT",
    "EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE",
    "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED",
    "WRONG_MODIFIER_TARGET"
)

package kotlin

import kotlin.wasm.internal.*

public class ByteArray(size: Int) {
    private var jsArray: WasmAnyRef = JsArray_new(size)

    init {
        JsArray_fill_Byte(jsArray, size) { 0 }
    }

    public inline constructor(size: Int, init: (Int) -> Byte) {
        jsArray = JsArray_new(size)
        JsArray_fill_Byte(jsArray, size, init)
    }

    public operator fun get(index: Int): Byte =
        JsArray_get_Byte(jsArray, index)

    public operator fun set(index: Int, value: Byte): Unit {
        JsArray_set_Byte(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): ByteIterator = _ByteArrayIterator(this)
}

internal fun _ByteArrayIterator(array: ByteArray) = object : ByteIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextByte() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class CharArray(size: Int) {
    private var jsArray: WasmAnyRef = JsArray_new(size)

    init {
        JsArray_fill_Char(jsArray, size) { 0.toChar() }
    }

    public inline constructor(size: Int, init: (Int) -> Char) {
        jsArray = JsArray_new(size)
        JsArray_fill_Char(jsArray, size, init)
    }

    public operator fun get(index: Int): Char =
        JsArray_get_Char(jsArray, index)

    public operator fun set(index: Int, value: Char): Unit {
        JsArray_set_Char(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): CharIterator = _CharArrayIterator(this)
}

internal fun _CharArrayIterator(array: CharArray) = object : CharIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextChar() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class ShortArray(size: Int) {
    private var jsArray: WasmAnyRef = JsArray_new(size)

    init {
        JsArray_fill_Short(jsArray, size) { 0 }
    }

    public inline constructor(size: Int, init: (Int) -> Short) {
        jsArray = JsArray_new(size)
        JsArray_fill_Short(jsArray, size, init)
    }

    public operator fun get(index: Int): Short =
        JsArray_get_Short(jsArray, index)

    public operator fun set(index: Int, value: Short): Unit {
        JsArray_set_Short(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): ShortIterator = _ShortArrayIterator(this)
}

internal fun _ShortArrayIterator(array: ShortArray) = object : ShortIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextShort() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class IntArray(size: Int) {
    private var jsArray: WasmAnyRef = JsArray_new(size)

    init {
        JsArray_fill_Int(jsArray, size) { 0 }
    }

    public inline constructor(size: Int, init: (Int) -> Int) {
        jsArray = JsArray_new(size)
        JsArray_fill_Int(jsArray, size, init)
    }

    public operator fun get(index: Int): Int =
        JsArray_get_Int(jsArray, index)

    public operator fun set(index: Int, value: Int): Unit {
        JsArray_set_Int(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): IntIterator = _IntArrayIterator(this)
}

internal fun _IntArrayIterator(array: IntArray) = object : IntIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextInt() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class LongArray(size: Int) {
    private var jsArray: WasmAnyRef = JsArray_new(size)

    init {
        JsArray_fill_Long(jsArray, size) { 0L }
    }

    public inline constructor(size: Int, init: (Int) -> Long) {
        jsArray = JsArray_new(size)
        JsArray_fill_Long(jsArray, size, init)
    }

    public operator fun get(index: Int): Long =
        JsArray_get_Long(jsArray, index)

    public operator fun set(index: Int, value: Long): Unit {
        JsArray_set_Long(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): LongIterator = _LongArrayIterator(this)
}

internal fun _LongArrayIterator(array: LongArray) = object : LongIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextLong() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class FloatArray(size: Int) {
    private var jsArray: WasmAnyRef = JsArray_new(size)

    init {
        JsArray_fill_Float(jsArray, size) { 0.0f }
    }

    public inline constructor(size: Int, init: (Int) -> Float) {
        jsArray = JsArray_new(size)
        JsArray_fill_Float(jsArray, size, init)
    }

    public operator fun get(index: Int): Float =
        JsArray_get_Float(jsArray, index)

    public operator fun set(index: Int, value: Float): Unit {
        JsArray_set_Float(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): FloatIterator = _FloatArrayIterator(this)
}

internal fun _FloatArrayIterator(array: FloatArray) = object : FloatIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextFloat() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class DoubleArray(size: Int) {
    private var jsArray: WasmAnyRef = JsArray_new(size)

    init {
        JsArray_fill_Double(jsArray, size) { 0.0 }
    }

    public inline constructor(size: Int, init: (Int) -> Double) {
        jsArray = JsArray_new(size)
        JsArray_fill_Double(jsArray, size, init)
    }

    public operator fun get(index: Int): Double =
        JsArray_get_Double(jsArray, index)

    public operator fun set(index: Int, value: Double): Unit {
        JsArray_set_Double(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): DoubleIterator = _DoubleArrayIterator(this)
}

internal fun _DoubleArrayIterator(array: DoubleArray) = object : DoubleIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextDouble() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class BooleanArray(size: Int) {
    private var jsArray: WasmAnyRef = JsArray_new(size)

    init {
        JsArray_fill_Boolean(jsArray, size) { false }
    }

    public inline constructor(size: Int, init: (Int) -> Boolean) {
        jsArray = JsArray_new(size)
        JsArray_fill_Boolean(jsArray, size, init)
    }

    public operator fun get(index: Int): Boolean =
        JsArray_get_Boolean(jsArray, index)

    public operator fun set(index: Int, value: Boolean): Unit {
        JsArray_set_Boolean(jsArray, index, value)
    }

    public val size: Int
        get() = JsArray_getSize(jsArray)


    public operator fun iterator(): BooleanIterator = _BooleanArrayIterator(this)
}

internal fun _BooleanArrayIterator(array: BooleanArray) = object : BooleanIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextBoolean() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}
