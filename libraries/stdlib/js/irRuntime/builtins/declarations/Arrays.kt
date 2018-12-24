/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!
@file:Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")

package kotlin

/**
 * An array of bytes. When targeting the JVM, instances of this class are represented as `byte[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class ByteArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Byte): this(size)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Byte
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Byte): Unit

    /** Returns the number of elements in the array. */
    @Suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): ByteIterator
}



/**
 * An array of chars. When targeting the JVM, instances of this class are represented as `char[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to null char (`\u0000').
 */
public class CharArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    // public constructor(size: Int, init: (Int) -> Char): this(size)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Char
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Char): Unit

    /** Returns the number of elements in the array. */
    @Suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): CharIterator
}



//public inline class CharArray
//@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
//@PublishedApi
//internal constructor(@PublishedApi internal val storage: IntArray) {
//
//    /** Creates a new array of the specified [size], with all elements initialized to zero. */
//    public constructor(size: Int) : this(IntArray(size))
//
//    /** Returns the array element at the given [index]. This method can be called using the index operator. */
//    public operator fun get(index: Int): Char = storage[index].toChar()
//
//    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
//    public operator fun set(index: Int, value: Char) {
//        storage[index] = value.toInt()
//    }
//
//    /** Returns the number of elements in the array. */
//    val size: Int get() = storage.size
//
//    /** Creates an iterator over the elements of the array. */
//    operator fun iterator(): CharIterator = IteratorImmmmpl(storage)
//
//    private class IteratorImmmmpl(private val array: IntArray) : CharIterator() {
//        private var index = 0
//        override fun hasNext() = index < array.size
//        override fun nextChar() = if (index < array.size) array[index++].toChar() else throw NoSuchElementException(index.toString())
//    }
//}


@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun CharArray(size: Int, init: (Int) -> Char): CharArray {
    val result = CharArray(size)
    var i = 0
    while (i != result.size) {
        result[i] = init(i)
        ++i
    }
    return result
}

/**
 * An array of shorts. When targeting the JVM, instances of this class are represented as `short[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class ShortArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Short): this(size)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Short
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Short): Unit

    /** Returns the number of elements in the array. */
    @Suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): ShortIterator
}

/**
 * An array of ints. When targeting the JVM, instances of this class are represented as `int[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class IntArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Int): this(size)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Int
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Int): Unit

    /** Returns the number of elements in the array. */
    @Suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): IntIterator
}

/**
 * An array of longs. When targeting the JVM, instances of this class are represented as `long[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class LongArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Long): this(size)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Long
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Long): Unit

    /** Returns the number of elements in the array. */
    @Suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): LongIterator
}

/**
 * An array of floats. When targeting the JVM, instances of this class are represented as `float[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class FloatArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Float): this(size)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Float
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Float): Unit

    /** Returns the number of elements in the array. */
    @Suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): FloatIterator
}

/**
 * An array of doubles. When targeting the JVM, instances of this class are represented as `double[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class DoubleArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Double): this(size)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Double
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Double): Unit

    /** Returns the number of elements in the array. */
    @Suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): DoubleIterator
}

/**
 * An array of booleans. When targeting the JVM, instances of this class are represented as `boolean[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to `false`.
 */
public class BooleanArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Boolean): this(size)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Boolean
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Boolean): Unit

    /** Returns the number of elements in the array. */
    @Suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): BooleanIterator
}

