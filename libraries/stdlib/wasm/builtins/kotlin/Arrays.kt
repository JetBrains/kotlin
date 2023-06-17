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

/**
 * An array of bytes.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 * @throws RuntimeException if the specified [size] is negative.
 */
public class ByteArray(size: Int) {
    internal val storage: WasmByteArray

    init {
        if (size < 0) throw IllegalArgumentException("Negative array size")
        storage = WasmByteArray(size)
    }

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmByteArray)

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public inline constructor(size: Int, init: (Int) -> Byte)

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun get(index: Int): Byte {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun set(index: Int, value: Byte) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = storage.len()

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): ByteIterator = byteArrayIterator(this)
}

internal fun byteArrayIterator(array: ByteArray) = object : ByteIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextByte() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createByteArray(size: Int, init: (Int) -> Byte): ByteArray {
    if (size < 0) throw IllegalArgumentException("Negative array size")
    val result = WasmByteArray(size)
    result.fill(size, init)
    return ByteArray(result)
}

/**
 * An array of chars.
 * @constructor Creates a new array of the specified [size], with all elements initialized to null char (`\u0000').
 * @throws RuntimeException if the specified [size] is negative.
 */
public class CharArray(size: Int) {
    internal val storage: WasmCharArray

    init {
        if (size < 0) throw IllegalArgumentException("Negative array size")
        storage = WasmCharArray(size)
    }

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmCharArray)

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public inline constructor(size: Int, init: (Int) -> Char)

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun get(index: Int): Char {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun set(index: Int, value: Char) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = storage.len()

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): CharIterator = charArrayIterator(this)
}

internal fun charArrayIterator(array: CharArray) = object : CharIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextChar() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createCharArray(size: Int, init: (Int) -> Char): CharArray {
    if (size < 0) throw IllegalArgumentException("Negative array size")
    val result = WasmCharArray(size)
    result.fill(size, init)
    return CharArray(result)
}

/**
 * An array of shorts.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 * @throws RuntimeException if the specified [size] is negative.
 */
public class ShortArray(size: Int) {
    internal val storage: WasmShortArray

    init {
        if (size < 0) throw IllegalArgumentException("Negative array size")
        storage = WasmShortArray(size)
    }

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmShortArray)

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public inline constructor(size: Int, init: (Int) -> Short)

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun get(index: Int): Short {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun set(index: Int, value: Short) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = storage.len()

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): ShortIterator = shortArrayIterator(this)
}

internal fun shortArrayIterator(array: ShortArray) = object : ShortIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextShort() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createShortArray(size: Int, init: (Int) -> Short): ShortArray {
    if (size < 0) throw IllegalArgumentException("Negative array size")
    val result = WasmShortArray(size)
    result.fill(size, init)
    return ShortArray(result)
}

/**
 * An array of ints.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 * @throws RuntimeException if the specified [size] is negative.
 */
public class IntArray(size: Int) {
    internal val storage: WasmIntArray

    init {
        if (size < 0) throw IllegalArgumentException("Negative array size")
        storage = WasmIntArray(size)
    }

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmIntArray)

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public inline constructor(size: Int, init: (Int) -> Int)

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun get(index: Int): Int {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun set(index: Int, value: Int) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = storage.len()

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): IntIterator = intArrayIterator(this)
}

internal fun intArrayIterator(array: IntArray) = object : IntIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextInt() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createIntArray(size: Int, init: (Int) -> Int): IntArray {
    if (size < 0) throw IllegalArgumentException("Negative array size")
    val result = WasmIntArray(size)
    result.fill(size, init)
    return IntArray(result)
}

/**
 * An array of longs.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 * @throws RuntimeException if the specified [size] is negative.
 */
public class LongArray(size: Int) {
    internal val storage: WasmLongArray

    init {
        if (size < 0) throw IllegalArgumentException("Negative array size")
        storage = WasmLongArray(size)
    }

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmLongArray)

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public inline constructor(size: Int, init: (Int) -> Long)

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun get(index: Int): Long {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun set(index: Int, value: Long) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = storage.len()

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): LongIterator = longArrayIterator(this)
}

internal fun longArrayIterator(array: LongArray) = object : LongIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextLong() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createLongArray(size: Int, init: (Int) -> Long): LongArray {
    if (size < 0) throw IllegalArgumentException("Negative array size")
    val result = WasmLongArray(size)
    result.fill(size, init)
    return LongArray(result)
}

/**
 * An array of floats.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 * @throws RuntimeException if the specified [size] is negative.
 */
public class FloatArray(size: Int) {
    internal val storage: WasmFloatArray

    init {
        if (size < 0) throw IllegalArgumentException("Negative array size")
        storage = WasmFloatArray(size)
    }

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmFloatArray)

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public inline constructor(size: Int, init: (Int) -> Float)

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun get(index: Int): Float {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun set(index: Int, value: Float) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = storage.len()

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): FloatIterator = floatArrayIterator(this)
}

internal fun floatArrayIterator(array: FloatArray) = object : FloatIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextFloat() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createFloatArray(size: Int, init: (Int) -> Float): FloatArray {
    if (size < 0) throw IllegalArgumentException("Negative array size")
    val result = WasmFloatArray(size)
    result.fill(size, init)
    return FloatArray(result)
}

/**
 * An array of doubles.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 * @throws RuntimeException if the specified [size] is negative.
 */
public class DoubleArray(size: Int) {
    internal val storage: WasmDoubleArray

    init {
        if (size < 0) throw IllegalArgumentException("Negative array size")
        storage = WasmDoubleArray(size)
    }

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmDoubleArray)

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public inline constructor(size: Int, init: (Int) -> Double)

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun get(index: Int): Double {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun set(index: Int, value: Double) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = storage.len()

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): DoubleIterator = doubleArrayIterator(this)
}

internal fun doubleArrayIterator(array: DoubleArray) = object : DoubleIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextDouble() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun createDoubleArray(size: Int, init: (Int) -> Double): DoubleArray {
    if (size < 0) throw IllegalArgumentException("Negative array size")
    val result = WasmDoubleArray(size)
    result.fill(size, init)
    return DoubleArray(result)
}

/**
 * An array of booleans.
 * @constructor Creates a new array of the specified [size], with all elements initialized to `false`.
 * @throws RuntimeException if the specified [size] is negative.
 */
public class BooleanArray(size: Int) {
    internal val storage: WasmByteArray

    init {
        if (size < 0) throw IllegalArgumentException("Negative array size")
        storage = WasmByteArray(size)
    }

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmByteArray)

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public inline constructor(size: Int, init: (Int) -> Boolean)

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun get(index: Int): Boolean {
        rangeCheck(index, storage.len())
        return storage.get(index).reinterpretAsInt().reinterpretAsBoolean()
    }

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    public operator fun set(index: Int, value: Boolean) {
        rangeCheck(index, storage.len())
        storage.set(index, value.toInt().reinterpretAsByte())
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = storage.len()

    /** Creates an iterator over the elements of the array. */
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
    if (size < 0) throw IllegalArgumentException("Negative array size")
    val result = WasmByteArray(size)
    result.fill(size) {
        init(it).reinterpretAsByte()
    }
    return BooleanArray(result)
}