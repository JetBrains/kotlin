/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress(
    "TYPE_PARAMETER_AS_REIFIED",
    "WRONG_MODIFIER_TARGET",
    "NON_ABSTRACT_FUNCTION_WITH_NO_BODY",
    "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED",
    "UNUSED_PARAMETER"
)

package kotlin

import kotlin.wasm.internal.*

public class ByteArray(size: Int) {
    internal val storage = WasmByteArray(size)

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmByteArray)

    public inline constructor(size: Int, init: (Int) -> Byte)

    public operator fun get(index: Int): Byte {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Byte) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()

    public operator fun iterator(): ByteIterator = byteArrayIterator(this)
}

internal fun byteArrayIterator(array: ByteArray) = object : ByteIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextByte() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createByteArray(size: Int, init: (Int) -> Byte): ByteArray {
    val result = WasmByteArray(size)
    result.fill(size, init)
    return ByteArray(result)
}

public class CharArray(size: Int) {
    internal val storage = WasmCharArray(size)

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmCharArray)

    public inline constructor(size: Int, init: (Int) -> Char)

    public operator fun get(index: Int): Char {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Char) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()


    public operator fun iterator(): CharIterator = charArrayIterator(this)
}

internal fun charArrayIterator(array: CharArray) = object : CharIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextChar() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createCharArray(size: Int, init: (Int) -> Char): CharArray {
    val result = WasmCharArray(size)
    result.fill(size, init)
    return CharArray(result)
}

public class ShortArray(size: Int) {
    internal val storage = WasmShortArray(size)

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmShortArray)

    public inline constructor(size: Int, init: (Int) -> Short)

    public operator fun get(index: Int): Short {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Short) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()


    public operator fun iterator(): ShortIterator = shortArrayIterator(this)
}

internal fun shortArrayIterator(array: ShortArray) = object : ShortIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextShort() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createShortArray(size: Int, init: (Int) -> Short): ShortArray {
    val result = WasmShortArray(size)
    result.fill(size, init)
    return ShortArray(result)
}

public class IntArray(size: Int) {
    internal val storage = WasmIntArray(size)

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmIntArray)

    public inline constructor(size: Int, init: (Int) -> Int)

    public operator fun get(index: Int): Int {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Int) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()


    public operator fun iterator(): IntIterator = intArrayIterator(this)
}

internal fun intArrayIterator(array: IntArray) = object : IntIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextInt() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createIntArray(size: Int, init: (Int) -> Int): IntArray {
    val result = WasmIntArray(size)
    result.fill(size, init)
    return IntArray(result)
}

public class LongArray(size: Int) {
    internal val storage = WasmLongArray (size)

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmLongArray)

    public inline constructor(size: Int, init: (Int) -> Long)

    public operator fun get(index: Int): Long {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Long) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()

    public operator fun iterator(): LongIterator = longArrayIterator(this)
}

internal fun longArrayIterator(array: LongArray) = object : LongIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextLong() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createLongArray(size: Int, init: (Int) -> Long): LongArray {
    val result = WasmLongArray(size)
    result.fill(size, init)
    return LongArray(result)
}

public class FloatArray(size: Int) {
    internal val storage = WasmFloatArray(size)

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmFloatArray)

    public inline constructor(size: Int, init: (Int) -> Float)

    public operator fun get(index: Int): Float {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Float) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()

    public operator fun iterator(): FloatIterator = floatArrayIterator(this)
}

internal fun floatArrayIterator(array: FloatArray) = object : FloatIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextFloat() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createFloatArray(size: Int, init: (Int) -> Float): FloatArray {
    val result = WasmFloatArray(size)
    result.fill(size, init)
    return FloatArray(result)
}

public class DoubleArray(size: Int) {
    internal val storage = WasmDoubleArray(size)

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmDoubleArray)

    public inline constructor(size: Int, init: (Int) -> Double)

    public operator fun get(index: Int): Double {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Double) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()

    public operator fun iterator(): DoubleIterator = doubleArrayIterator(this)
}

internal fun doubleArrayIterator(array: DoubleArray) = object : DoubleIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextDouble() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createDoubleArray(size: Int, init: (Int) -> Double): DoubleArray {
    val result = WasmDoubleArray(size)
    result.fill(size, init)
    return DoubleArray(result)
}

public class BooleanArray(size: Int) {
    internal val storage = WasmByteArray(size)

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmByteArray)

    public inline constructor(size: Int, init: (Int) -> Boolean)

    public operator fun get(index: Int): Boolean {
        rangeCheck(index, storage.len())
        return storage.get(index).reinterpretAsInt().reinterpretAsBoolean()
    }

    public operator fun set(index: Int, value: Boolean) {
        rangeCheck(index, storage.len())
        storage.set(index, value.toInt().reinterpretAsByte())
    }

    public val size: Int
        get() = storage.len()

    public operator fun iterator(): BooleanIterator = booleanArrayIterator(this)
}

internal fun booleanArrayIterator(array: BooleanArray) = object : BooleanIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextBoolean() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

@WasmNoOpCast
private fun Boolean.reinterpretAsByte(): Byte =
    implementedAsIntrinsic

internal inline fun createBooleanArray(size: Int, init: (Int) -> Boolean): BooleanArray {
    val result = WasmByteArray(size)
    result.fill(size) {
        init(it).reinterpretAsByte()
    }
    return BooleanArray(result)
}