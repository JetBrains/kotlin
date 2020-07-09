/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// TODO: Add SortedSet class and implement toSortedSet extensions
// TODO: Add fill and binary search methods for primitive arrays (with tests)

package kotlin

import kotlin.internal.PureReifiable
import kotlin.native.internal.ExportTypeInfo
import kotlin.native.internal.IntrinsicType
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.PointsTo

/**
 * An array of bytes.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theByteArrayTypeInfo")
public final class ByteArray {
    // Constructors are handled with compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Byte): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    public val size: Int
        get() = getArrayLength()

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_ByteArray_get")
    external public operator fun get(index: Int): Byte

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_ByteArray_set")
    external public operator fun set(index: Int, value: Byte): Unit

    @SymbolName("Kotlin_ByteArray_getArrayLength")
    external private fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): ByteIterator {
        return ByteIteratorImpl(this)
    }
}

private class ByteIteratorImpl(val collection: ByteArray) : ByteIterator() {
    var index : Int = 0

    public override fun nextByte(): Byte {
        if (!hasNext()) throw NoSuchElementException("$index")
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}

/**
 * An array of chars.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theCharArrayTypeInfo")
public final class CharArray {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Char): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = getArrayLength()

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_CharArray_get")
    external public operator fun get(index: Int): Char

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_CharArray_set")
    external public operator fun set(index: Int, value: Char): Unit

    @SymbolName("Kotlin_CharArray_getArrayLength")
    external private fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): kotlin.collections.CharIterator {
        return CharIteratorImpl(this)
    }
}

private class CharIteratorImpl(val collection: CharArray) : CharIterator() {
    var index : Int = 0

    public override fun nextChar(): Char {
        if (!hasNext()) throw NoSuchElementException("$index")
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}

/**
 * An array of shorts.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theShortArrayTypeInfo")
public final class ShortArray {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Short): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = getArrayLength()

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_ShortArray_get")
    external public operator fun get(index: Int): Short

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_ShortArray_set")
    external public operator fun set(index: Int, value: Short): Unit

    @SymbolName("Kotlin_ShortArray_getArrayLength")
    external private fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): kotlin.collections.ShortIterator {
        return ShortIteratorImpl(this)
    }
}

private class ShortIteratorImpl(val collection: ShortArray) : ShortIterator() {
    var index : Int = 0

    public override fun nextShort(): Short {
        if (!hasNext()) throw NoSuchElementException("$index")
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}

/**
 * An array of ints. When targeting the JVM, instances of this class are represented as `int[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theIntArrayTypeInfo")
public final class IntArray {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Int): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = getArrayLength()

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_IntArray_get")
    external public operator fun get(index: Int): Int

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_IntArray_set")
    external public operator fun set(index: Int, value: Int): Unit

    @SymbolName("Kotlin_IntArray_getArrayLength")
    external private fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): kotlin.collections.IntIterator {
        return IntIteratorImpl(this)
    }
}

private class IntIteratorImpl(val collection: IntArray) : IntIterator() {
    var index : Int = 0

    public override fun nextInt(): Int {
        if (!hasNext()) throw NoSuchElementException("$index")
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}

/**
 * An array of longs.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theLongArrayTypeInfo")
public final class LongArray {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Long): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = getArrayLength()

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_LongArray_get")
    external public operator fun get(index: Int): Long

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_LongArray_set")
    external public operator fun set(index: Int, value: Long): Unit

    @SymbolName("Kotlin_LongArray_getArrayLength")
    external private fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): kotlin.collections.LongIterator {
        return LongIteratorImpl(this)
    }
}

private class LongIteratorImpl(val collection: LongArray) : LongIterator() {
    var index : Int = 0

    public override fun nextLong(): Long {
        if (!hasNext()) throw NoSuchElementException("$index")
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}

/**
 * An array of floats.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theFloatArrayTypeInfo")
public final class FloatArray {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Float): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = getArrayLength()

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_FloatArray_get")
    external public operator fun get(index: Int): Float

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_FloatArray_set")
    external public operator fun set(index: Int, value: Float): Unit

    @SymbolName("Kotlin_FloatArray_getArrayLength")
    external private fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): kotlin.collections.FloatIterator {
        return FloatIteratorImpl(this)
    }
}

private class FloatIteratorImpl(val collection: FloatArray) : FloatIterator() {
    var index : Int = 0

    public override fun nextFloat(): Float {
        if (!hasNext()) throw NoSuchElementException("$index")
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}

@ExportTypeInfo("theDoubleArrayTypeInfo")
public final class DoubleArray {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Double): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = getArrayLength()

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_DoubleArray_get")
    external public operator fun get(index: Int): Double

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_DoubleArray_set")
    external public operator fun set(index: Int, value: Double): Unit

    @SymbolName("Kotlin_DoubleArray_getArrayLength")
    external private fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): kotlin.collections.DoubleIterator {
        return DoubleIteratorImpl(this)
    }
}

private class DoubleIteratorImpl(val collection: DoubleArray) : DoubleIterator() {
    var index : Int = 0

    public override fun nextDouble(): Double {
        if (!hasNext()) throw NoSuchElementException("$index")
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}

@ExportTypeInfo("theBooleanArrayTypeInfo")
public final class BooleanArray {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Boolean): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    /** Returns the number of elements in the array. */
    public val size: Int
        get() = getArrayLength()

    /**
     * Returns the array element at the given [index].  This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_BooleanArray_get")
    external public operator fun get(index: Int): Boolean

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_BooleanArray_set")
    external public operator fun set(index: Int, value: Boolean): Unit

    @SymbolName("Kotlin_BooleanArray_getArrayLength")
    external private fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): kotlin.collections.BooleanIterator {
        return BooleanIteratorImpl(this)
    }
}

private class BooleanIteratorImpl(val collection: BooleanArray) : BooleanIterator() {
    var index : Int = 0

    public override fun nextBoolean(): Boolean {
        if (!hasNext()) throw NoSuchElementException("$index")
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}

/**
 * Returns an array of objects of the given type with the given [size], initialized with null values.
 */
public inline fun <reified @PureReifiable T> arrayOfNulls(size: Int): Array<T?> =
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        arrayOfUninitializedElements<T?>(size)

/**
 * Returns an array containing the specified elements.
 */
@TypedIntrinsic(IntrinsicType.IDENTITY)
@PointsTo(0x00, 0x01) // ret -> elements
public external inline fun <reified @PureReifiable T> arrayOf(vararg elements: T): Array<T>

@SymbolName("Kotlin_emptyArray")
external public fun <T> emptyArray(): Array<T>

/**
 * Returns an array containing the specified [Double] numbers.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun doubleArrayOf(vararg elements: Double) = elements

/**
 * Returns an array containing the specified [Float] numbers.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun floatArrayOf(vararg elements: Float) = elements

/**
 * Returns an array containing the specified [Long] numbers.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun longArrayOf(vararg elements: Long) = elements

/**
 * Returns an array containing the specified [Int] numbers.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun intArrayOf(vararg elements: Int) = elements

/**
 * Returns an array containing the specified characters.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun charArrayOf(vararg elements: Char) = elements

/**
 * Returns an array containing the specified [Short] numbers.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun shortArrayOf(vararg elements: Short) = elements

/**
 * Returns an array containing the specified [Byte] numbers.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun byteArrayOf(vararg elements: Byte) = elements

/**
 * Returns an array containing the specified boolean values.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun booleanArrayOf(vararg elements: Boolean) = elements
