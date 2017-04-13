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

// TODO: Add SortedSed class and implement toSortedSet extensions
// TODO: Add fill and binary search methods for primitive arrays (with tests)

package kotlin

import kotlin.collections.*
import kotlin.comparisons.*
import kotlin.internal.PureReifiable
import kotlin.util.sortArrayComparable
import kotlin.util.sortArrayWith
import kotlin.util.sortArray
// TODO: make all iterator() methods inline.

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
     * [init] function. The [init] function returns an array element given its index.
     */
    // TODO: What about inline constructors?
    public constructor(size: Int, init: (Int) -> Byte): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_ByteArray_get")
    external public operator fun get(index: Int): Byte

    @SymbolName("Kotlin_ByteArray_set")
    external public operator fun set(index: Int, value: Byte): Unit

    @SymbolName("Kotlin_ByteArray_getArrayLength")
    external private fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): ByteIterator {
        return ByteIteratorImpl(this)
    }
}

// TODO: replace with generics, once implemented.
private class ByteIteratorImpl(val collection: ByteArray) : ByteIterator() {
    var index : Int = 0

    public override fun nextByte(): Byte {
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
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Char): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_CharArray_get")
    external public operator fun get(index: Int): Char

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
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Short): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_ShortArray_get")
    external public operator fun get(index: Int): Short

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
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Int): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_IntArray_get")
    external public operator fun get(index: Int): Int

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
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Long): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_LongArray_get")
    external public operator fun get(index: Int): Long

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
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Float): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_FloatArray_get")
    external public operator fun get(index: Int): Float

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
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Double): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_DoubleArray_get")
    external public operator fun get(index: Int): Double

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
     * [init] function. The [init] function returns an array element given its index.
     */
    public constructor(size: Int, init: (Int) -> Boolean): this(size) {
        for (i in 0..size - 1) {
            this[i] = init(i)
        }
    }

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_BooleanArray_get")
    external public operator fun get(index: Int): Boolean

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
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}

// From ArraysJVM.kt
/** Returns the array if it's not `null`, or an empty array otherwise. */
public inline fun <reified T> Array<out T>?.orEmpty(): Array<out T> = this ?: emptyArray<T>()

// This part is from generated _Arrays.kt.


/**
 * Returns 1st *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> Array<out T>.component1(): T {
    return get(0)
}

/**
 * Returns 1st *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun ByteArray.component1(): Byte {
    return get(0)
}

/**
 * Returns 1st *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun ShortArray.component1(): Short {
    return get(0)
}

/**
 * Returns 1st *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun IntArray.component1(): Int {
    return get(0)
}

/**
 * Returns 1st *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun LongArray.component1(): Long {
    return get(0)
}

/**
 * Returns 1st *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun FloatArray.component1(): Float {
    return get(0)
}

/**
 * Returns 1st *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun DoubleArray.component1(): Double {
    return get(0)
}

/**
 * Returns 1st *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun BooleanArray.component1(): Boolean {
    return get(0)
}

/**
 * Returns 1st *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun CharArray.component1(): Char {
    return get(0)
}

/**
 * Returns 2nd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> Array<out T>.component2(): T {
    return get(1)
}

/**
 * Returns 2nd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun ByteArray.component2(): Byte {
    return get(1)
}

/**
 * Returns 2nd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun ShortArray.component2(): Short {
    return get(1)
}

/**
 * Returns 2nd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun IntArray.component2(): Int {
    return get(1)
}

/**
 * Returns 2nd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun LongArray.component2(): Long {
    return get(1)
}

/**
 * Returns 2nd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun FloatArray.component2(): Float {
    return get(1)
}

/**
 * Returns 2nd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun DoubleArray.component2(): Double {
    return get(1)
}

/**
 * Returns 2nd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun BooleanArray.component2(): Boolean {
    return get(1)
}

/**
 * Returns 2nd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun CharArray.component2(): Char {
    return get(1)
}

/**
 * Returns 3rd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> Array<out T>.component3(): T {
    return get(2)
}

/**
 * Returns 3rd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun ByteArray.component3(): Byte {
    return get(2)
}

/**
 * Returns 3rd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun ShortArray.component3(): Short {
    return get(2)
}

/**
 * Returns 3rd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun IntArray.component3(): Int {
    return get(2)
}

/**
 * Returns 3rd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun LongArray.component3(): Long {
    return get(2)
}

/**
 * Returns 3rd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun FloatArray.component3(): Float {
    return get(2)
}

/**
 * Returns 3rd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun DoubleArray.component3(): Double {
    return get(2)
}

/**
 * Returns 3rd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun BooleanArray.component3(): Boolean {
    return get(2)
}

/**
 * Returns 3rd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun CharArray.component3(): Char {
    return get(2)
}

/**
 * Returns 4th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> Array<out T>.component4(): T {
    return get(3)
}

/**
 * Returns 4th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun ByteArray.component4(): Byte {
    return get(3)
}

/**
 * Returns 4th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun ShortArray.component4(): Short {
    return get(3)
}

/**
 * Returns 4th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun IntArray.component4(): Int {
    return get(3)
}

/**
 * Returns 4th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun LongArray.component4(): Long {
    return get(3)
}

/**
 * Returns 4th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun FloatArray.component4(): Float {
    return get(3)
}

/**
 * Returns 4th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun DoubleArray.component4(): Double {
    return get(3)
}

/**
 * Returns 4th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun BooleanArray.component4(): Boolean {
    return get(3)
}

/**
 * Returns 4th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun CharArray.component4(): Char {
    return get(3)
}

/**
 * Returns 5th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> Array<out T>.component5(): T {
    return get(4)
}

/**
 * Returns 5th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun ByteArray.component5(): Byte {
    return get(4)
}

/**
 * Returns 5th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun ShortArray.component5(): Short {
    return get(4)
}

/**
 * Returns 5th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun IntArray.component5(): Int {
    return get(4)
}

/**
 * Returns 5th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun LongArray.component5(): Long {
    return get(4)
}

/**
 * Returns 5th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun FloatArray.component5(): Float {
    return get(4)
}

/**
 * Returns 5th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun DoubleArray.component5(): Double {
    return get(4)
}

/**
 * Returns 5th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun BooleanArray.component5(): Boolean {
    return get(4)
}

/**
 * Returns 5th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun CharArray.component5(): Char {
    return get(4)
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun <T> Array<out T>.all(predicate: (T) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun ByteArray.all(predicate: (Byte) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun ShortArray.all(predicate: (Short) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun IntArray.all(predicate: (Int) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun LongArray.all(predicate: (Long) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun FloatArray.all(predicate: (Float) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun DoubleArray.all(predicate: (Double) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun BooleanArray.all(predicate: (Boolean) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun CharArray.all(predicate: (Char) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if array has at least one element.
 */
public fun <T> Array<out T>.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if array has at least one element.
 */
public fun ByteArray.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if array has at least one element.
 */
public fun ShortArray.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if array has at least one element.
 */
public fun IntArray.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if array has at least one element.
 */
public fun LongArray.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if array has at least one element.
 */
public fun FloatArray.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if array has at least one element.
 */
public fun DoubleArray.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if array has at least one element.
 */
public fun BooleanArray.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if array has at least one element.
 */
public fun CharArray.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun <T> Array<out T>.any(predicate: (T) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun ByteArray.any(predicate: (Byte) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun ShortArray.any(predicate: (Short) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun IntArray.any(predicate: (Int) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun LongArray.any(predicate: (Long) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun FloatArray.any(predicate: (Float) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun DoubleArray.any(predicate: (Double) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun BooleanArray.any(predicate: (Boolean) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun CharArray.any(predicate: (Char) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns `true` if [element] is found in the array.
 */
public operator fun <@kotlin.internal.OnlyInputTypes T> Array<out T>.contains(element: T): Boolean {
    return indexOf(element) >= 0
}

/**
 * Returns `true` if [element] is found in the array.
 */
public operator fun ByteArray.contains(element: Byte): Boolean {
    return indexOf(element) >= 0
}

/**
 * Returns `true` if [element] is found in the array.
 */
public operator fun ShortArray.contains(element: Short): Boolean {
    return indexOf(element) >= 0
}

/**
 * Returns `true` if [element] is found in the array.
 */
public operator fun IntArray.contains(element: Int): Boolean {
    return indexOf(element) >= 0
}

/**
 * Returns `true` if [element] is found in the array.
 */
public operator fun LongArray.contains(element: Long): Boolean {
    return indexOf(element) >= 0
}

/**
 * Returns `true` if [element] is found in the array.
 */
public operator fun FloatArray.contains(element: Float): Boolean {
    return indexOf(element) >= 0
}

/**
 * Returns `true` if [element] is found in the array.
 */
public operator fun DoubleArray.contains(element: Double): Boolean {
    return indexOf(element) >= 0
}

/**
 * Returns `true` if [element] is found in the array.
 */
public operator fun BooleanArray.contains(element: Boolean): Boolean {
    return indexOf(element) >= 0
}

/**
 * Returns `true` if [element] is found in the array.
 */
public operator fun CharArray.contains(element: Char): Boolean {
    return indexOf(element) >= 0
}

/**
 * Returns the number of elements in this array.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Array<out T>.count(): Int = this.size

/**
 * Returns the number of elements in this array.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.count(): Int {
    return size
}

/**
 * Returns the number of elements in this array.
 */
@kotlin.internal.InlineOnly
public inline fun ShortArray.count(): Int {
    return size
}

/**
 * Returns the number of elements in this array.
 */
@kotlin.internal.InlineOnly
public inline fun IntArray.count(): Int {
    return size
}

/**
 * Returns the number of elements in this array.
 */
@kotlin.internal.InlineOnly
public inline fun LongArray.count(): Int {
    return size
}

/**
 * Returns the number of elements in this array.
 */
@kotlin.internal.InlineOnly
public inline fun FloatArray.count(): Int {
    return size
}

/**
 * Returns the number of elements in this array.
 */
@kotlin.internal.InlineOnly
public inline fun DoubleArray.count(): Int {
    return size
}

/**
 * Returns the number of elements in this array.
 */
@kotlin.internal.InlineOnly
public inline fun BooleanArray.count(): Int {
    return size
}

/**
 * Returns the number of elements in this array.
 */
@kotlin.internal.InlineOnly
public inline fun CharArray.count(): Int {
    return size
}

/**
 * Returns the number of elements matching the given [predicate].
 */
public inline fun <T> Array<out T>.count(predicate: (T) -> Boolean): Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the number of elements matching the given [predicate].
 */
public inline fun ByteArray.count(predicate: (Byte) -> Boolean): Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the number of elements matching the given [predicate].
 */
public inline fun ShortArray.count(predicate: (Short) -> Boolean): Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the number of elements matching the given [predicate].
 */
public inline fun IntArray.count(predicate: (Int) -> Boolean): Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the number of elements matching the given [predicate].
 */
public inline fun LongArray.count(predicate: (Long) -> Boolean): Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the number of elements matching the given [predicate].
 */
public inline fun FloatArray.count(predicate: (Float) -> Boolean): Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the number of elements matching the given [predicate].
 */
public inline fun DoubleArray.count(predicate: (Double) -> Boolean): Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the number of elements matching the given [predicate].
 */
public inline fun BooleanArray.count(predicate: (Boolean) -> Boolean): Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the number of elements matching the given [predicate].
 */
public inline fun CharArray.count(predicate: (Char) -> Boolean): Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Array<out T>.elementAt(index: Int): T {
    return get(index)
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.elementAt(index: Int): Byte {
    return get(index)
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun ShortArray.elementAt(index: Int): Short {
    return get(index)
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun IntArray.elementAt(index: Int): Int {
    return get(index)
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun LongArray.elementAt(index: Int): Long {
    return get(index)
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun FloatArray.elementAt(index: Int): Float {
    return get(index)
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun DoubleArray.elementAt(index: Int): Double {
    return get(index)
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun BooleanArray.elementAt(index: Int): Boolean {
    return get(index)
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun CharArray.elementAt(index: Int): Char {
    return get(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Array<out T>.elementAtOrElse(index: Int, defaultValue: (Int) -> T): T {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.elementAtOrElse(index: Int, defaultValue: (Int) -> Byte): Byte {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun ShortArray.elementAtOrElse(index: Int, defaultValue: (Int) -> Short): Short {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun IntArray.elementAtOrElse(index: Int, defaultValue: (Int) -> Int): Int {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun LongArray.elementAtOrElse(index: Int, defaultValue: (Int) -> Long): Long {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun FloatArray.elementAtOrElse(index: Int, defaultValue: (Int) -> Float): Float {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun DoubleArray.elementAtOrElse(index: Int, defaultValue: (Int) -> Double): Double {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun BooleanArray.elementAtOrElse(index: Int, defaultValue: (Int) -> Boolean): Boolean {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun CharArray.elementAtOrElse(index: Int, defaultValue: (Int) -> Char): Char {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Array<out T>.elementAtOrNull(index: Int): T? {
    return this.getOrNull(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.elementAtOrNull(index: Int): Byte? {
    return this.getOrNull(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun ShortArray.elementAtOrNull(index: Int): Short? {
    return this.getOrNull(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun IntArray.elementAtOrNull(index: Int): Int? {
    return this.getOrNull(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun LongArray.elementAtOrNull(index: Int): Long? {
    return this.getOrNull(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun FloatArray.elementAtOrNull(index: Int): Float? {
    return this.getOrNull(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun DoubleArray.elementAtOrNull(index: Int): Double? {
    return this.getOrNull(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun BooleanArray.elementAtOrNull(index: Int): Boolean? {
    return this.getOrNull(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun CharArray.elementAtOrNull(index: Int): Char? {
    return this.getOrNull(index)
}

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun <T> Array<out T>.first(): T {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[0]
}

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun ByteArray.first(): Byte {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[0]
}

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun ShortArray.first(): Short {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[0]
}

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun IntArray.first(): Int {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[0]
}

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun LongArray.first(): Long {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[0]
}

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun FloatArray.first(): Float {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[0]
}

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun DoubleArray.first(): Double {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[0]
}

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun BooleanArray.first(): Boolean {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[0]
}

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun CharArray.first(): Char {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[0]
}

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun <T> Array<out T>.first(predicate: (T) -> Boolean): T {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun ByteArray.first(predicate: (Byte) -> Boolean): Byte {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun ShortArray.first(predicate: (Short) -> Boolean): Short {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun IntArray.first(predicate: (Int) -> Boolean): Int {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun LongArray.first(predicate: (Long) -> Boolean): Long {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun FloatArray.first(predicate: (Float) -> Boolean): Float {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun DoubleArray.first(predicate: (Double) -> Boolean): Double {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun BooleanArray.first(predicate: (Boolean) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun CharArray.first(predicate: (Char) -> Boolean): Char {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Array<out T>.getOrElse(index: Int, defaultValue: (Int) -> T): T {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.getOrElse(index: Int, defaultValue: (Int) -> Byte): Byte {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun ShortArray.getOrElse(index: Int, defaultValue: (Int) -> Short): Short {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun IntArray.getOrElse(index: Int, defaultValue: (Int) -> Int): Int {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun LongArray.getOrElse(index: Int, defaultValue: (Int) -> Long): Long {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun FloatArray.getOrElse(index: Int, defaultValue: (Int) -> Float): Float {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun DoubleArray.getOrElse(index: Int, defaultValue: (Int) -> Double): Double {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun BooleanArray.getOrElse(index: Int, defaultValue: (Int) -> Boolean): Boolean {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this array.
 */
@kotlin.internal.InlineOnly
public inline fun CharArray.getOrElse(index: Int, defaultValue: (Int) -> Char): Char {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
public fun <T> Array<out T>.getOrNull(index: Int): T? {
    return if (index >= 0 && index <= lastIndex) get(index) else null
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
public fun ByteArray.getOrNull(index: Int): Byte? {
    return if (index >= 0 && index <= lastIndex) get(index) else null
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
public fun ShortArray.getOrNull(index: Int): Short? {
    return if (index >= 0 && index <= lastIndex) get(index) else null
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
public fun IntArray.getOrNull(index: Int): Int? {
    return if (index >= 0 && index <= lastIndex) get(index) else null
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
public fun LongArray.getOrNull(index: Int): Long? {
    return if (index >= 0 && index <= lastIndex) get(index) else null
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
public fun FloatArray.getOrNull(index: Int): Float? {
    return if (index >= 0 && index <= lastIndex) get(index) else null
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
public fun DoubleArray.getOrNull(index: Int): Double? {
    return if (index >= 0 && index <= lastIndex) get(index) else null
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
public fun BooleanArray.getOrNull(index: Int): Boolean? {
    return if (index >= 0 && index <= lastIndex) get(index) else null
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this array.
 */
public fun CharArray.getOrNull(index: Int): Char? {
    return if (index >= 0 && index <= lastIndex) get(index) else null
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun <R> IntArray.fold(initial: R, operation: (acc: R, Int) -> R): R {
    var accumulator = initial
    for (element in this) accumulator = operation(accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun <R> LongArray.fold(initial: R, operation: (acc: R, Long) -> R): R {
    var accumulator = initial
    for (element in this) accumulator = operation(accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun <R> FloatArray.fold(initial: R, operation: (acc: R, Float) -> R): R {
    var accumulator = initial
    for (element in this) accumulator = operation(accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun <R> DoubleArray.fold(initial: R, operation: (acc: R, Double) -> R): R {
    var accumulator = initial
    for (element in this) accumulator = operation(accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun <R> BooleanArray.fold(initial: R, operation: (acc: R, Boolean) -> R): R {
    var accumulator = initial
    for (element in this) accumulator = operation(accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun <R> CharArray.fold(initial: R, operation: (acc: R, Char) -> R): R {
    var accumulator = initial
    for (element in this) accumulator = operation(accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 */
public inline fun <T, R> Array<out T>.foldIndexed(initial: R, operation: (index: Int, acc: R, T) -> R): R {
    var index = 0
    var accumulator = initial
    for (element in this) accumulator = operation(index++, accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 */
public inline fun <R> ByteArray.foldIndexed(initial: R, operation: (index: Int, acc: R, Byte) -> R): R {
    var index = 0
    var accumulator = initial
    for (element in this) accumulator = operation(index++, accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 */
public inline fun <R> ShortArray.foldIndexed(initial: R, operation: (index: Int, acc: R, Short) -> R): R {
    var index = 0
    var accumulator = initial
    for (element in this) accumulator = operation(index++, accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 */
public inline fun <R> IntArray.foldIndexed(initial: R, operation: (index: Int, acc: R, Int) -> R): R {
    var index = 0
    var accumulator = initial
    for (element in this) accumulator = operation(index++, accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 */
public inline fun <R> LongArray.foldIndexed(initial: R, operation: (index: Int, acc: R, Long) -> R): R {
    var index = 0
    var accumulator = initial
    for (element in this) accumulator = operation(index++, accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 */
public inline fun <R> FloatArray.foldIndexed(initial: R, operation: (index: Int, acc: R, Float) -> R): R {
    var index = 0
    var accumulator = initial
    for (element in this) accumulator = operation(index++, accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 */
public inline fun <R> DoubleArray.foldIndexed(initial: R, operation: (index: Int, acc: R, Double) -> R): R {
    var index = 0
    var accumulator = initial
    for (element in this) accumulator = operation(index++, accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 */
public inline fun <R> BooleanArray.foldIndexed(initial: R, operation: (index: Int, acc: R, Boolean) -> R): R {
    var index = 0
    var accumulator = initial
    for (element in this) accumulator = operation(index++, accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 */
public inline fun <R> CharArray.foldIndexed(initial: R, operation: (index: Int, acc: R, Char) -> R): R {
    var index = 0
    var accumulator = initial
    for (element in this) accumulator = operation(index++, accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun <T, R> Array<out T>.foldRight(initial: R, operation: (T, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun <R> ByteArray.foldRight(initial: R, operation: (Byte, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun <R> ShortArray.foldRight(initial: R, operation: (Short, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun <R> IntArray.foldRight(initial: R, operation: (Int, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun <R> LongArray.foldRight(initial: R, operation: (Long, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun <R> FloatArray.foldRight(initial: R, operation: (Float, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun <R> DoubleArray.foldRight(initial: R, operation: (Double, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun <R> BooleanArray.foldRight(initial: R, operation: (Boolean, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun <R> CharArray.foldRight(initial: R, operation: (Char, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun <T, R> Array<out T>.foldRightIndexed(initial: R, operation: (index: Int, T, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun <R> ByteArray.foldRightIndexed(initial: R, operation: (index: Int, Byte, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun <R> ShortArray.foldRightIndexed(initial: R, operation: (index: Int, Short, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun <R> IntArray.foldRightIndexed(initial: R, operation: (index: Int, Int, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun <R> LongArray.foldRightIndexed(initial: R, operation: (index: Int, Long, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun <R> FloatArray.foldRightIndexed(initial: R, operation: (index: Int, Float, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun <R> DoubleArray.foldRightIndexed(initial: R, operation: (index: Int, Double, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun <R> BooleanArray.foldRightIndexed(initial: R, operation: (index: Int, Boolean, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun <R> CharArray.foldRightIndexed(initial: R, operation: (index: Int, Char, acc: R) -> R): R {
    var index = lastIndex
    var accumulator = initial
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Performs the given [action] on each element.
 */
public inline fun <T> Array<out T>.forEach(action: (T) -> Unit): Unit {
    for (element in this) action(element)
}

/**
 * Performs the given [action] on each element.
 */
public inline fun ByteArray.forEach(action: (Byte) -> Unit): Unit {
    for (element in this) action(element)
}

/**
 * Performs the given [action] on each element.
 */
public inline fun ShortArray.forEach(action: (Short) -> Unit): Unit {
    for (element in this) action(element)
}

/**
 * Performs the given [action] on each element.
 */
public inline fun IntArray.forEach(action: (Int) -> Unit): Unit {
    for (element in this) action(element)
}

/**
 * Performs the given [action] on each element.
 */
public inline fun LongArray.forEach(action: (Long) -> Unit): Unit {
    for (element in this) action(element)
}

/**
 * Performs the given [action] on each element.
 */
public inline fun FloatArray.forEach(action: (Float) -> Unit): Unit {
    for (element in this) action(element)
}

/**
 * Performs the given [action] on each element.
 */
public inline fun DoubleArray.forEach(action: (Double) -> Unit): Unit {
    for (element in this) action(element)
}

/**
 * Performs the given [action] on each element.
 */
public inline fun BooleanArray.forEach(action: (Boolean) -> Unit): Unit {
    for (element in this) action(element)
}

/**
 * Performs the given [action] on each element.
 */
public inline fun CharArray.forEach(action: (Char) -> Unit): Unit {
    for (element in this) action(element)
}

/**
 * Performs the given [action] on each element, providing sequential index with the element.
 * @param [action] function that takes the index of an element and the element itself
 * and performs the desired action on the element.
 */
public inline fun <T> Array<out T>.forEachIndexed(action: (index: Int, T) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}

/**
 * Performs the given [action] on each element, providing sequential index with the element.
 * @param [action] function that takes the index of an element and the element itself
 * and performs the desired action on the element.
 */
public inline fun ByteArray.forEachIndexed(action: (index: Int, Byte) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}

/**
 * Performs the given [action] on each element, providing sequential index with the element.
 * @param [action] function that takes the index of an element and the element itself
 * and performs the desired action on the element.
 */
public inline fun ShortArray.forEachIndexed(action: (index: Int, Short) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}

/**
 * Performs the given [action] on each element, providing sequential index with the element.
 * @param [action] function that takes the index of an element and the element itself
 * and performs the desired action on the element.
 */
public inline fun IntArray.forEachIndexed(action: (index: Int, Int) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}

/**
 * Performs the given [action] on each element, providing sequential index with the element.
 * @param [action] function that takes the index of an element and the element itself
 * and performs the desired action on the element.
 */
public inline fun LongArray.forEachIndexed(action: (index: Int, Long) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}

/**
 * Performs the given [action] on each element, providing sequential index with the element.
 * @param [action] function that takes the index of an element and the element itself
 * and performs the desired action on the element.
 */
public inline fun FloatArray.forEachIndexed(action: (index: Int, Float) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}

/**
 * Performs the given [action] on each element, providing sequential index with the element.
 * @param [action] function that takes the index of an element and the element itself
 * and performs the desired action on the element.
 */
public inline fun DoubleArray.forEachIndexed(action: (index: Int, Double) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}

/**
 * Performs the given [action] on each element, providing sequential index with the element.
 * @param [action] function that takes the index of an element and the element itself
 * and performs the desired action on the element.
 */
public inline fun BooleanArray.forEachIndexed(action: (index: Int, Boolean) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}

/**
 * Performs the given [action] on each element, providing sequential index with the element.
 * @param [action] function that takes the index of an element and the element itself
 * and performs the desired action on the element.
 */
public inline fun CharArray.forEachIndexed(action: (index: Int, Char) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}

/**
 * Returns the largest element or `null` if there are no elements.
 *
 * If any of elements is `NaN` returns `NaN`.
 */
@SinceKotlin("1.1")
public fun Array<out Double>.max(): Double? {
    if (isEmpty()) return null
    var max = this[0]
    if (max.isNaN()) return max
    for (i in 1..lastIndex) {
        val e = this[i]
        if (e.isNaN()) return e
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the largest element or `null` if there are no elements.
 *
 * If any of elements is `NaN` returns `NaN`.
 */
@SinceKotlin("1.1")
public fun Array<out Float>.max(): Float? {
    if (isEmpty()) return null
    var max = this[0]
    if (max.isNaN()) return max
    for (i in 1..lastIndex) {
        val e = this[i]
        if (e.isNaN()) return e
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the largest element or `null` if there are no elements.
 */
public fun <T : Comparable<T>> Array<out T>.max(): T? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the largest element or `null` if there are no elements.
 */
public fun ByteArray.max(): Byte? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the largest element or `null` if there are no elements.
 */
public fun ShortArray.max(): Short? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the largest element or `null` if there are no elements.
 */
public fun IntArray.max(): Int? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the largest element or `null` if there are no elements.
 */
public fun LongArray.max(): Long? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the largest element or `null` if there are no elements.
 *
 * If any of elements is `NaN` returns `NaN`.
 */
public fun FloatArray.max(): Float? {
    if (isEmpty()) return null
    var max = this[0]
    if (max.isNaN()) return max
    for (i in 1..lastIndex) {
        val e = this[i]
        if (e.isNaN()) return e
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the largest element or `null` if there are no elements.
 *
 * If any of elements is `NaN` returns `NaN`.
 */
public fun DoubleArray.max(): Double? {
    if (isEmpty()) return null
    var max = this[0]
    if (max.isNaN()) return max
    for (i in 1..lastIndex) {
        val e = this[i]
        if (e.isNaN()) return e
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the largest element or `null` if there are no elements.
 */
public fun CharArray.max(): Char? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there are no elements.
 */
public inline fun <T, R : Comparable<R>> Array<out T>.maxBy(selector: (T) -> R): T? {
    if (isEmpty()) return null
    var maxElem = this[0]
    var maxValue = selector(maxElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> ByteArray.maxBy(selector: (Byte) -> R): Byte? {
    if (isEmpty()) return null
    var maxElem = this[0]
    var maxValue = selector(maxElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> ShortArray.maxBy(selector: (Short) -> R): Short? {
    if (isEmpty()) return null
    var maxElem = this[0]
    var maxValue = selector(maxElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> IntArray.maxBy(selector: (Int) -> R): Int? {
    if (isEmpty()) return null
    var maxElem = this[0]
    var maxValue = selector(maxElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> LongArray.maxBy(selector: (Long) -> R): Long? {
    if (isEmpty()) return null
    var maxElem = this[0]
    var maxValue = selector(maxElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> FloatArray.maxBy(selector: (Float) -> R): Float? {
    if (isEmpty()) return null
    var maxElem = this[0]
    var maxValue = selector(maxElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> DoubleArray.maxBy(selector: (Double) -> R): Double? {
    if (isEmpty()) return null
    var maxElem = this[0]
    var maxValue = selector(maxElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> BooleanArray.maxBy(selector: (Boolean) -> R): Boolean? {
    if (isEmpty()) return null
    var maxElem = this[0]
    var maxValue = selector(maxElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> CharArray.maxBy(selector: (Char) -> R): Char? {
    if (isEmpty()) return null
    var maxElem = this[0]
    var maxValue = selector(maxElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Returns the first element having the largest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun <T> Array<out T>.maxWith(comparator: Comparator<in T>): T? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(max, e) < 0) max = e
    }
    return max
}

/**
 * Returns the first element having the largest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun ByteArray.maxWith(comparator: Comparator<in Byte>): Byte? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(max, e) < 0) max = e
    }
    return max
}

/**
 * Returns the first element having the largest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun ShortArray.maxWith(comparator: Comparator<in Short>): Short? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(max, e) < 0) max = e
    }
    return max
}

/**
 * Returns the first element having the largest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun IntArray.maxWith(comparator: Comparator<in Int>): Int? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(max, e) < 0) max = e
    }
    return max
}

/**
 * Returns the first element having the largest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun LongArray.maxWith(comparator: Comparator<in Long>): Long? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(max, e) < 0) max = e
    }
    return max
}

/**
 * Returns the first element having the largest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun FloatArray.maxWith(comparator: Comparator<in Float>): Float? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(max, e) < 0) max = e
    }
    return max
}

/**
 * Returns the first element having the largest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun DoubleArray.maxWith(comparator: Comparator<in Double>): Double? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(max, e) < 0) max = e
    }
    return max
}

/**
 * Returns the first element having the largest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun BooleanArray.maxWith(comparator: Comparator<in Boolean>): Boolean? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(max, e) < 0) max = e
    }
    return max
}

/**
 * Returns the first element having the largest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun CharArray.maxWith(comparator: Comparator<in Char>): Char? {
    if (isEmpty()) return null
    var max = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(max, e) < 0) max = e
    }
    return max
}

/**
 * Returns the smallest element or `null` if there are no elements.
 *
 * If any of elements is `NaN` returns `NaN`.
 */
@SinceKotlin("1.1")
public fun Array<out Double>.min(): Double? {
    if (isEmpty()) return null
    var min = this[0]
    if (min.isNaN()) return min
    for (i in 1..lastIndex) {
        val e = this[i]
        if (e.isNaN()) return e
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the smallest element or `null` if there are no elements.
 *
 * If any of elements is `NaN` returns `NaN`.
 */
@SinceKotlin("1.1")
public fun Array<out Float>.min(): Float? {
    if (isEmpty()) return null
    var min = this[0]
    if (min.isNaN()) return min
    for (i in 1..lastIndex) {
        val e = this[i]
        if (e.isNaN()) return e
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the smallest element or `null` if there are no elements.
 */
public fun <T : Comparable<T>> Array<out T>.min(): T? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the smallest element or `null` if there are no elements.
 */
public fun ByteArray.min(): Byte? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the smallest element or `null` if there are no elements.
 */
public fun ShortArray.min(): Short? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the smallest element or `null` if there are no elements.
 */
public fun IntArray.min(): Int? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the smallest element or `null` if there are no elements.
 */
public fun LongArray.min(): Long? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the smallest element or `null` if there are no elements.
 *
 * If any of elements is `NaN` returns `NaN`.
 */
public fun FloatArray.min(): Float? {
    if (isEmpty()) return null
    var min = this[0]
    if (min.isNaN()) return min
    for (i in 1..lastIndex) {
        val e = this[i]
        if (e.isNaN()) return e
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the smallest element or `null` if there are no elements.
 *
 * If any of elements is `NaN` returns `NaN`.
 */
public fun DoubleArray.min(): Double? {
    if (isEmpty()) return null
    var min = this[0]
    if (min.isNaN()) return min
    for (i in 1..lastIndex) {
        val e = this[i]
        if (e.isNaN()) return e
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the smallest element or `null` if there are no elements.
 */
public fun CharArray.min(): Char? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 */
public inline fun <T, R : Comparable<R>> Array<out T>.minBy(selector: (T) -> R): T? {
    if (isEmpty()) return null
    var minElem = this[0]
    var minValue = selector(minElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> ByteArray.minBy(selector: (Byte) -> R): Byte? {
    if (isEmpty()) return null
    var minElem = this[0]
    var minValue = selector(minElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> ShortArray.minBy(selector: (Short) -> R): Short? {
    if (isEmpty()) return null
    var minElem = this[0]
    var minValue = selector(minElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> IntArray.minBy(selector: (Int) -> R): Int? {
    if (isEmpty()) return null
    var minElem = this[0]
    var minValue = selector(minElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> LongArray.minBy(selector: (Long) -> R): Long? {
    if (isEmpty()) return null
    var minElem = this[0]
    var minValue = selector(minElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> FloatArray.minBy(selector: (Float) -> R): Float? {
    if (isEmpty()) return null
    var minElem = this[0]
    var minValue = selector(minElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> DoubleArray.minBy(selector: (Double) -> R): Double? {
    if (isEmpty()) return null
    var minElem = this[0]
    var minValue = selector(minElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> BooleanArray.minBy(selector: (Boolean) -> R): Boolean? {
    if (isEmpty()) return null
    var minElem = this[0]
    var minValue = selector(minElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>> CharArray.minBy(selector: (Char) -> R): Char? {
    if (isEmpty()) return null
    var minElem = this[0]
    var minValue = selector(minElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Returns the first element having the smallest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun <T> Array<out T>.minWith(comparator: Comparator<in T>): T? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(min, e) > 0) min = e
    }
    return min
}

/**
 * Returns the first element having the smallest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun ByteArray.minWith(comparator: Comparator<in Byte>): Byte? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(min, e) > 0) min = e
    }
    return min
}

/**
 * Returns the first element having the smallest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun ShortArray.minWith(comparator: Comparator<in Short>): Short? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(min, e) > 0) min = e
    }
    return min
}

/**
 * Returns the first element having the smallest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun IntArray.minWith(comparator: Comparator<in Int>): Int? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(min, e) > 0) min = e
    }
    return min
}

/**
 * Returns the first element having the smallest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun LongArray.minWith(comparator: Comparator<in Long>): Long? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(min, e) > 0) min = e
    }
    return min
}

/**
 * Returns the first element having the smallest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun FloatArray.minWith(comparator: Comparator<in Float>): Float? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(min, e) > 0) min = e
    }
    return min
}

/**
 * Returns the first element having the smallest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun DoubleArray.minWith(comparator: Comparator<in Double>): Double? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(min, e) > 0) min = e
    }
    return min
}

/**
 * Returns the first element having the smallest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun BooleanArray.minWith(comparator: Comparator<in Boolean>): Boolean? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(min, e) > 0) min = e
    }
    return min
}

/**
 * Returns the first element having the smallest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun CharArray.minWith(comparator: Comparator<in Char>): Char? {
    if (isEmpty()) return null
    var min = this[0]
    for (i in 1..lastIndex) {
        val e = this[i]
        if (comparator.compare(min, e) > 0) min = e
    }
    return min
}

/**
 * Returns `true` if the array has no elements.
 */
public fun <T> Array<out T>.none(): Boolean {
    for (element in this) return false
    return true
}

/**
 * Returns `true` if the array has no elements.
 */
public fun ByteArray.none(): Boolean {
    for (element in this) return false
    return true
}

/**
 * Returns `true` if the array has no elements.
 */
public fun ShortArray.none(): Boolean {
    for (element in this) return false
    return true
}

/**
 * Returns `true` if the array has no elements.
 */
public fun IntArray.none(): Boolean {
    for (element in this) return false
    return true
}

/**
 * Returns `true` if the array has no elements.
 */
public fun LongArray.none(): Boolean {
    for (element in this) return false
    return true
}

/**
 * Returns `true` if the array has no elements.
 */
public fun FloatArray.none(): Boolean {
    for (element in this) return false
    return true
}

/**
 * Returns `true` if the array has no elements.
 */
public fun DoubleArray.none(): Boolean {
    for (element in this) return false
    return true
}

/**
 * Returns `true` if the array has no elements.
 */
public fun BooleanArray.none(): Boolean {
    for (element in this) return false
    return true
}

/**
 * Returns `true` if the array has no elements.
 */
public fun CharArray.none(): Boolean {
    for (element in this) return false
    return true
}

/**
 * Returns `true` if no elements match the given [predicate].
 */
public inline fun <T> Array<out T>.none(predicate: (T) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return false
    return true
}

/**
 * Returns `true` if no elements match the given [predicate].
 */
public inline fun ByteArray.none(predicate: (Byte) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return false
    return true
}

/**
 * Returns `true` if no elements match the given [predicate].
 */
public inline fun ShortArray.none(predicate: (Short) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return false
    return true
}

/**
 * Returns `true` if no elements match the given [predicate].
 */
public inline fun IntArray.none(predicate: (Int) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return false
    return true
}

/**
 * Returns `true` if no elements match the given [predicate].
 */
public inline fun LongArray.none(predicate: (Long) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return false
    return true
}

/**
 * Returns `true` if no elements match the given [predicate].
 */
public inline fun FloatArray.none(predicate: (Float) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return false
    return true
}

/**
 * Returns `true` if no elements match the given [predicate].
 */
public inline fun DoubleArray.none(predicate: (Double) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return false
    return true
}

/**
 * Returns `true` if no elements match the given [predicate].
 */
public inline fun BooleanArray.none(predicate: (Boolean) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return false
    return true
}

/**
 * Returns `true` if no elements match the given [predicate].
 */
public inline fun CharArray.none(predicate: (Char) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return false
    return true
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun <S, T: S> Array<out T>.reduce(operation: (acc: S, T) -> S): S {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator: S = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun ByteArray.reduce(operation: (acc: Byte, Byte) -> Byte): Byte {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun ShortArray.reduce(operation: (acc: Short, Short) -> Short): Short {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun IntArray.reduce(operation: (acc: Int, Int) -> Int): Int {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun LongArray.reduce(operation: (acc: Long, Long) -> Long): Long {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun FloatArray.reduce(operation: (acc: Float, Float) -> Float): Float {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun DoubleArray.reduce(operation: (acc: Double, Double) -> Double): Double {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun BooleanArray.reduce(operation: (acc: Boolean, Boolean) -> Boolean): Boolean {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun CharArray.reduce(operation: (acc: Char, Char) -> Char): Char {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself and calculates the next accumulator value.
 */
public inline fun <S, T: S> Array<out T>.reduceIndexed(operation: (index: Int, acc: S, T) -> S): S {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator: S = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(index, accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself and calculates the next accumulator value.
 */
public inline fun ByteArray.reduceIndexed(operation: (index: Int, acc: Byte, Byte) -> Byte): Byte {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(index, accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself and calculates the next accumulator value.
 */
public inline fun ShortArray.reduceIndexed(operation: (index: Int, acc: Short, Short) -> Short): Short {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(index, accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself and calculates the next accumulator value.
 */
public inline fun IntArray.reduceIndexed(operation: (index: Int, acc: Int, Int) -> Int): Int {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(index, accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself and calculates the next accumulator value.
 */
public inline fun LongArray.reduceIndexed(operation: (index: Int, acc: Long, Long) -> Long): Long {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(index, accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself and calculates the next accumulator value.
 */
public inline fun FloatArray.reduceIndexed(operation: (index: Int, acc: Float, Float) -> Float): Float {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(index, accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself and calculates the next accumulator value.
 */
public inline fun DoubleArray.reduceIndexed(operation: (index: Int, acc: Double, Double) -> Double): Double {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(index, accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself and calculates the next accumulator value.
 */
public inline fun BooleanArray.reduceIndexed(operation: (index: Int, acc: Boolean, Boolean) -> Boolean): Boolean {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(index, accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original array.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself and calculates the next accumulator value.
 */
public inline fun CharArray.reduceIndexed(operation: (index: Int, acc: Char, Char) -> Char): Char {
    if (isEmpty())
        throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = this[0]
    for (index in 1..lastIndex) {
        accumulator = operation(index, accumulator, this[index])
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun <S, T: S> Array<out T>.reduceRight(operation: (T, acc: S) -> S): S {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator: S = get(index--)
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun ByteArray.reduceRight(operation: (Byte, acc: Byte) -> Byte): Byte {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun ShortArray.reduceRight(operation: (Short, acc: Short) -> Short): Short {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun IntArray.reduceRight(operation: (Int, acc: Int) -> Int): Int {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun LongArray.reduceRight(operation: (Long, acc: Long) -> Long): Long {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun FloatArray.reduceRight(operation: (Float, acc: Float) -> Float): Float {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun DoubleArray.reduceRight(operation: (Double, acc: Double) -> Double): Double {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun BooleanArray.reduceRight(operation: (Boolean, acc: Boolean) -> Boolean): Boolean {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun CharArray.reduceRight(operation: (Char, acc: Char) -> Char): Char {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(get(index--), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun <S, T: S> Array<out T>.reduceRightIndexed(operation: (index: Int, T, acc: S) -> S): S {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator: S = get(index--)
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun ByteArray.reduceRightIndexed(operation: (index: Int, Byte, acc: Byte) -> Byte): Byte {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun ShortArray.reduceRightIndexed(operation: (index: Int, Short, acc: Short) -> Short): Short {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun IntArray.reduceRightIndexed(operation: (index: Int, Int, acc: Int) -> Int): Int {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun LongArray.reduceRightIndexed(operation: (index: Int, Long, acc: Long) -> Long): Long {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun FloatArray.reduceRightIndexed(operation: (index: Int, Float, acc: Float) -> Float): Float {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun DoubleArray.reduceRightIndexed(operation: (index: Int, Double, acc: Double) -> Double): Double {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun BooleanArray.reduceRightIndexed(operation: (index: Int, Boolean, acc: Boolean) -> Boolean): Boolean {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left
 * to each element with its index in the original array and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun CharArray.reduceRightIndexed(operation: (index: Int, Char, acc: Char) -> Char): Char {
    var index = lastIndex
    if (index < 0) throw UnsupportedOperationException("Empty array can't be reduced.")
    var accumulator = get(index--)
    while (index >= 0) {
        accumulator = operation(index, get(index), accumulator)
        --index
    }
    return accumulator
}

/**
 * Returns first index of [element], or -1 if the array does not contain element.
 */
public fun <@kotlin.internal.OnlyInputTypes T> Array<out T>.indexOf(element: T): Int {
    if (element == null) {
        for (index in indices) {
            if (this[index] == null) {
                return index
            }
        }
    } else {
        for (index in indices) {
            if (element == this[index]) {
                return index
            }
        }
    }
    return -1
}

/**
 * Returns first index of [element], or -1 if the array does not contain element.
 */
public fun ByteArray.indexOf(element: Byte): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns first index of [element], or -1 if the array does not contain element.
 */
public fun ShortArray.indexOf(element: Short): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns first index of [element], or -1 if the array does not contain element.
 */
public fun IntArray.indexOf(element: Int): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns first index of [element], or -1 if the array does not contain element.
 */
public fun LongArray.indexOf(element: Long): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns first index of [element], or -1 if the array does not contain element.
 */
public fun FloatArray.indexOf(element: Float): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns first index of [element], or -1 if the array does not contain element.
 */
public fun DoubleArray.indexOf(element: Double): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns first index of [element], or -1 if the array does not contain element.
 */
public fun BooleanArray.indexOf(element: Boolean): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns first index of [element], or -1 if the array does not contain element.
 */
public fun CharArray.indexOf(element: Char): Int {
    for (index in indices) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun <T> Array<out T>.indexOfFirst(predicate: (T) -> Boolean): Int {
    for (index in indices) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun ByteArray.indexOfFirst(predicate: (Byte) -> Boolean): Int {
    for (index in indices) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun ShortArray.indexOfFirst(predicate: (Short) -> Boolean): Int {
    for (index in indices) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun IntArray.indexOfFirst(predicate: (Int) -> Boolean): Int {
    for (index in indices) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun LongArray.indexOfFirst(predicate: (Long) -> Boolean): Int {
    for (index in indices) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun FloatArray.indexOfFirst(predicate: (Float) -> Boolean): Int {
    for (index in indices) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun DoubleArray.indexOfFirst(predicate: (Double) -> Boolean): Int {
    for (index in indices) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun BooleanArray.indexOfFirst(predicate: (Boolean) -> Boolean): Int {
    for (index in indices) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun CharArray.indexOfFirst(predicate: (Char) -> Boolean): Int {
    for (index in indices) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun <T> Array<out T>.indexOfLast(predicate: (T) -> Boolean): Int {
    for (index in indices.reversed()) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun ByteArray.indexOfLast(predicate: (Byte) -> Boolean): Int {
    for (index in indices.reversed()) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun ShortArray.indexOfLast(predicate: (Short) -> Boolean): Int {
    for (index in indices.reversed()) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun IntArray.indexOfLast(predicate: (Int) -> Boolean): Int {
    for (index in indices.reversed()) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun LongArray.indexOfLast(predicate: (Long) -> Boolean): Int {
    for (index in indices.reversed()) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun FloatArray.indexOfLast(predicate: (Float) -> Boolean): Int {
    for (index in indices.reversed()) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun DoubleArray.indexOfLast(predicate: (Double) -> Boolean): Int {
    for (index in indices.reversed()) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun BooleanArray.indexOfLast(predicate: (Boolean) -> Boolean): Int {
    for (index in indices.reversed()) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the array does not contain such element.
 */
public inline fun CharArray.indexOfLast(predicate: (Char) -> Boolean): Int {
    for (index in indices.reversed()) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun <T> Array<out T>.last(): T {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[lastIndex]
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun ByteArray.last(): Byte {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[lastIndex]
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun ShortArray.last(): Short {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[lastIndex]
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun IntArray.last(): Int {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[lastIndex]
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun LongArray.last(): Long {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[lastIndex]
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun FloatArray.last(): Float {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[lastIndex]
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun DoubleArray.last(): Double {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[lastIndex]
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun BooleanArray.last(): Boolean {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[lastIndex]
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the array is empty.
 */
public fun CharArray.last(): Char {
    if (isEmpty())
        throw NoSuchElementException("Array is empty.")
    return this[lastIndex]
}

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun <T> Array<out T>.last(predicate: (T) -> Boolean): T {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun ByteArray.last(predicate: (Byte) -> Boolean): Byte {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun ShortArray.last(predicate: (Short) -> Boolean): Short {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun IntArray.last(predicate: (Int) -> Boolean): Int {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun LongArray.last(predicate: (Long) -> Boolean): Long {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun FloatArray.last(predicate: (Float) -> Boolean): Float {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun DoubleArray.last(predicate: (Double) -> Boolean): Double {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun BooleanArray.last(predicate: (Boolean) -> Boolean): Boolean {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun CharArray.last(predicate: (Char) -> Boolean): Char {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun <T> Array<out T>.filterNot(predicate: (T) -> Boolean): List<T> {
    return filterNotTo(ArrayList<T>(), predicate)
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun ByteArray.filterNot(predicate: (Byte) -> Boolean): List<Byte> {
    return filterNotTo(ArrayList<Byte>(), predicate)
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun ShortArray.filterNot(predicate: (Short) -> Boolean): List<Short> {
    return filterNotTo(ArrayList<Short>(), predicate)
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun IntArray.filterNot(predicate: (Int) -> Boolean): List<Int> {
    return filterNotTo(ArrayList<Int>(), predicate)
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun LongArray.filterNot(predicate: (Long) -> Boolean): List<Long> {
    return filterNotTo(ArrayList<Long>(), predicate)
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun FloatArray.filterNot(predicate: (Float) -> Boolean): List<Float> {
    return filterNotTo(ArrayList<Float>(), predicate)
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun DoubleArray.filterNot(predicate: (Double) -> Boolean): List<Double> {
    return filterNotTo(ArrayList<Double>(), predicate)
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun BooleanArray.filterNot(predicate: (Boolean) -> Boolean): List<Boolean> {
    return filterNotTo(ArrayList<Boolean>(), predicate)
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun CharArray.filterNot(predicate: (Char) -> Boolean): List<Char> {
    return filterNotTo(ArrayList<Char>(), predicate)
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <T, C : MutableCollection<in T>> Array<out T>.filterNotTo(destination: C, predicate: (T) -> Boolean): C {
    for (element in this) if (!predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Byte>> ByteArray.filterNotTo(destination: C, predicate: (Byte) -> Boolean): C {
    for (element in this) if (!predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Short>> ShortArray.filterNotTo(destination: C, predicate: (Short) -> Boolean): C {
    for (element in this) if (!predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Int>> IntArray.filterNotTo(destination: C, predicate: (Int) -> Boolean): C {
    for (element in this) if (!predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Long>> LongArray.filterNotTo(destination: C, predicate: (Long) -> Boolean): C {
    for (element in this) if (!predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Float>> FloatArray.filterNotTo(destination: C, predicate: (Float) -> Boolean): C {
    for (element in this) if (!predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Double>> DoubleArray.filterNotTo(destination: C, predicate: (Double) -> Boolean): C {
    for (element in this) if (!predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Boolean>> BooleanArray.filterNotTo(destination: C, predicate: (Boolean) -> Boolean): C {
    for (element in this) if (!predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Char>> CharArray.filterNotTo(destination: C, predicate: (Char) -> Boolean): C {
    for (element in this) if (!predicate(element)) destination.add(element)
    return destination
}

/**
 * Returns a list containing all elements that are not `null`.
 */
public fun <T : Any> Array<out T?>.filterNotNull(): List<T> {
    return filterNotNullTo(ArrayList<T>())
}

/**
 * Appends all elements that are not `null` to the given [destination].
 */
public fun <C : MutableCollection<in T>, T : Any> Array<out T?>.filterNotNullTo(destination: C): C {
    for (element in this) if (element != null) destination.add(element)
    return destination
}

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun <T> Array<out T>.find(predicate: (T) -> Boolean): T? {
    return firstOrNull(predicate)
}

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.find(predicate: (Byte) -> Boolean): Byte? {
    return firstOrNull(predicate)
}

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun ShortArray.find(predicate: (Short) -> Boolean): Short? {
    return firstOrNull(predicate)
}

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun IntArray.find(predicate: (Int) -> Boolean): Int? {
    return firstOrNull(predicate)
}

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun LongArray.find(predicate: (Long) -> Boolean): Long? {
    return firstOrNull(predicate)
}

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun FloatArray.find(predicate: (Float) -> Boolean): Float? {
    return firstOrNull(predicate)
}

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun DoubleArray.find(predicate: (Double) -> Boolean): Double? {
    return firstOrNull(predicate)
}

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun BooleanArray.find(predicate: (Boolean) -> Boolean): Boolean? {
    return firstOrNull(predicate)
}

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun CharArray.find(predicate: (Char) -> Boolean): Char? {
    return firstOrNull(predicate)
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun <T> Array<out T>.findLast(predicate: (T) -> Boolean): T? {
    return lastOrNull(predicate)
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.findLast(predicate: (Byte) -> Boolean): Byte? {
    return lastOrNull(predicate)
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun ShortArray.findLast(predicate: (Short) -> Boolean): Short? {
    return lastOrNull(predicate)
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun IntArray.findLast(predicate: (Int) -> Boolean): Int? {
    return lastOrNull(predicate)
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun LongArray.findLast(predicate: (Long) -> Boolean): Long? {
    return lastOrNull(predicate)
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun FloatArray.findLast(predicate: (Float) -> Boolean): Float? {
    return lastOrNull(predicate)
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun DoubleArray.findLast(predicate: (Double) -> Boolean): Double? {
    return lastOrNull(predicate)
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun BooleanArray.findLast(predicate: (Boolean) -> Boolean): Boolean? {
    return lastOrNull(predicate)
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun CharArray.findLast(predicate: (Char) -> Boolean): Char? {
    return lastOrNull(predicate)
}

/**
 * Returns the first element, or `null` if the array is empty.
 */
public fun <T> Array<out T>.firstOrNull(): T? {
    return if (isEmpty()) null else this[0]
}

/**
 * Returns the first element, or `null` if the array is empty.
 */
public fun ByteArray.firstOrNull(): Byte? {
    return if (isEmpty()) null else this[0]
}

/**
 * Returns the first element, or `null` if the array is empty.
 */
public fun ShortArray.firstOrNull(): Short? {
    return if (isEmpty()) null else this[0]
}

/**
 * Returns the first element, or `null` if the array is empty.
 */
public fun IntArray.firstOrNull(): Int? {
    return if (isEmpty()) null else this[0]
}

/**
 * Returns the first element, or `null` if the array is empty.
 */
public fun LongArray.firstOrNull(): Long? {
    return if (isEmpty()) null else this[0]
}

/**
 * Returns the first element, or `null` if the array is empty.
 */
public fun FloatArray.firstOrNull(): Float? {
    return if (isEmpty()) null else this[0]
}

/**
 * Returns the first element, or `null` if the array is empty.
 */
public fun DoubleArray.firstOrNull(): Double? {
    return if (isEmpty()) null else this[0]
}

/**
 * Returns the first element, or `null` if the array is empty.
 */
public fun BooleanArray.firstOrNull(): Boolean? {
    return if (isEmpty()) null else this[0]
}

/**
 * Returns the first element, or `null` if the array is empty.
 */
public fun CharArray.firstOrNull(): Char? {
    return if (isEmpty()) null else this[0]
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
public inline fun <T> Array<out T>.firstOrNull(predicate: (T) -> Boolean): T? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
public inline fun ByteArray.firstOrNull(predicate: (Byte) -> Boolean): Byte? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
public inline fun ShortArray.firstOrNull(predicate: (Short) -> Boolean): Short? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
public inline fun IntArray.firstOrNull(predicate: (Int) -> Boolean): Int? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
public inline fun LongArray.firstOrNull(predicate: (Long) -> Boolean): Long? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
public inline fun FloatArray.firstOrNull(predicate: (Float) -> Boolean): Float? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
public inline fun DoubleArray.firstOrNull(predicate: (Double) -> Boolean): Double? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
public inline fun BooleanArray.firstOrNull(predicate: (Boolean) -> Boolean): Boolean? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
public inline fun CharArray.firstOrNull(predicate: (Char) -> Boolean): Char? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns the last element, or `null` if the array is empty.
 */
public fun <T> Array<out T>.lastOrNull(): T? {
    return if (isEmpty()) null else this[size - 1]
}

/**
 * Returns the last element, or `null` if the array is empty.
 */
public fun ByteArray.lastOrNull(): Byte? {
    return if (isEmpty()) null else this[size - 1]
}

/**
 * Returns the last element, or `null` if the array is empty.
 */
public fun ShortArray.lastOrNull(): Short? {
    return if (isEmpty()) null else this[size - 1]
}

/**
 * Returns the last element, or `null` if the array is empty.
 */
public fun IntArray.lastOrNull(): Int? {
    return if (isEmpty()) null else this[size - 1]
}

/**
 * Returns the last element, or `null` if the array is empty.
 */
public fun LongArray.lastOrNull(): Long? {
    return if (isEmpty()) null else this[size - 1]
}

/**
 * Returns the last element, or `null` if the array is empty.
 */
public fun FloatArray.lastOrNull(): Float? {
    return if (isEmpty()) null else this[size - 1]
}

/**
 * Returns the last element, or `null` if the array is empty.
 */
public fun DoubleArray.lastOrNull(): Double? {
    return if (isEmpty()) null else this[size - 1]
}

/**
 * Returns the last element, or `null` if the array is empty.
 */
public fun BooleanArray.lastOrNull(): Boolean? {
    return if (isEmpty()) null else this[size - 1]
}

/**
 * Returns the last element, or `null` if the array is empty.
 */
public fun CharArray.lastOrNull(): Char? {
    return if (isEmpty()) null else this[size - 1]
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun <T> Array<out T>.lastOrNull(predicate: (T) -> Boolean): T? {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    return null
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun ByteArray.lastOrNull(predicate: (Byte) -> Boolean): Byte? {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    return null
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun ShortArray.lastOrNull(predicate: (Short) -> Boolean): Short? {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    return null
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun IntArray.lastOrNull(predicate: (Int) -> Boolean): Int? {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    return null
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun LongArray.lastOrNull(predicate: (Long) -> Boolean): Long? {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    return null
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun FloatArray.lastOrNull(predicate: (Float) -> Boolean): Float? {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    return null
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun DoubleArray.lastOrNull(predicate: (Double) -> Boolean): Double? {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    return null
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun BooleanArray.lastOrNull(predicate: (Boolean) -> Boolean): Boolean? {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    return null
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun CharArray.lastOrNull(predicate: (Char) -> Boolean): Char? {
    for (index in this.indices.reversed()) {
        val element = this[index]
        if (predicate(element)) return element
    }
    return null
}

/**
 * Returns the single element, or throws an exception if the array is empty or has more than one element.
 */
public fun <T> Array<out T>.single(): T {
    return when (size) {
        0 -> throw NoSuchElementException("Array is empty.")
        1 -> this[0]
        else -> throw IllegalArgumentException("Array has more than one element.")
    }
}

/**
 * Returns the single element, or throws an exception if the array is empty or has more than one element.
 */
public fun ByteArray.single(): Byte {
    return when (size) {
        0 -> throw NoSuchElementException("Array is empty.")
        1 -> this[0]
        else -> throw IllegalArgumentException("Array has more than one element.")
    }
}

/**
 * Returns the single element, or throws an exception if the array is empty or has more than one element.
 */
public fun ShortArray.single(): Short {
    return when (size) {
        0 -> throw NoSuchElementException("Array is empty.")
        1 -> this[0]
        else -> throw IllegalArgumentException("Array has more than one element.")
    }
}

/**
 * Returns the single element, or throws an exception if the array is empty or has more than one element.
 */
public fun IntArray.single(): Int {
    return when (size) {
        0 -> throw NoSuchElementException("Array is empty.")
        1 -> this[0]
        else -> throw IllegalArgumentException("Array has more than one element.")
    }
}

/**
 * Returns the single element, or throws an exception if the array is empty or has more than one element.
 */
public fun LongArray.single(): Long {
    return when (size) {
        0 -> throw NoSuchElementException("Array is empty.")
        1 -> this[0]
        else -> throw IllegalArgumentException("Array has more than one element.")
    }
}

/**
 * Returns the single element, or throws an exception if the array is empty or has more than one element.
 */
public fun DoubleArray.single(): Double {
    return when (size) {
        0 -> throw NoSuchElementException("Array is empty.")
        1 -> this[0]
        else -> throw IllegalArgumentException("Array has more than one element.")
    }
}

/**
 * Returns the single element, or throws an exception if the array is empty or has more than one element.
 */
public fun BooleanArray.single(): Boolean {
    return when (size) {
        0 -> throw NoSuchElementException("Array is empty.")
        1 -> this[0]
        else -> throw IllegalArgumentException("Array has more than one element.")
    }
}

/**
 * Returns the single element, or throws an exception if the array is empty or has more than one element.
 */
public fun CharArray.single(): Char {
    return when (size) {
        0 -> throw NoSuchElementException("Array is empty.")
        1 -> this[0]
        else -> throw IllegalArgumentException("Array has more than one element.")
    }
}

/**
 * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
 */
public inline fun <T> Array<out T>.single(predicate: (T) -> Boolean): T {
    var single: T? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Array contains more than one matching element.")
            single = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Array contains no element matching the predicate.")
    return single as T
}

/**
 * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
 */
public inline fun ByteArray.single(predicate: (Byte) -> Boolean): Byte {
    var single: Byte? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Array contains more than one matching element.")
            single = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Array contains no element matching the predicate.")
    return single as Byte
}

/**
 * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
 */
public inline fun ShortArray.single(predicate: (Short) -> Boolean): Short {
    var single: Short? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Array contains more than one matching element.")
            single = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Array contains no element matching the predicate.")
    return single as Short
}

/**
 * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
 */
public inline fun IntArray.single(predicate: (Int) -> Boolean): Int {
    var single: Int? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Array contains more than one matching element.")
            single = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Array contains no element matching the predicate.")
    return single as Int
}

/**
 * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
 */
public inline fun LongArray.single(predicate: (Long) -> Boolean): Long {
    var single: Long? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Array contains more than one matching element.")
            single = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Array contains no element matching the predicate.")
    return single as Long
}

/**
 * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
 */
public inline fun FloatArray.single(predicate: (Float) -> Boolean): Float {
    var single: Float? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Array contains more than one matching element.")
            single = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Array contains no element matching the predicate.")
    return single as Float
}

/**
 * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
 */
public inline fun DoubleArray.single(predicate: (Double) -> Boolean): Double {
    var single: Double? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Array contains more than one matching element.")
            single = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Array contains no element matching the predicate.")
    return single as Double
}

/**
 * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
 */
public inline fun BooleanArray.single(predicate: (Boolean) -> Boolean): Boolean {
    var single: Boolean? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Array contains more than one matching element.")
            single = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Array contains no element matching the predicate.")
    return single as Boolean
}

/**
 * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
 */
public inline fun CharArray.single(predicate: (Char) -> Boolean): Char {
    var single: Char? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Array contains more than one matching element.")
            single = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Array contains no element matching the predicate.")
    return single as Char
}

/**
 * Returns single element, or `null` if the array is empty or has more than one element.
 */
public fun <T> Array<out T>.singleOrNull(): T? {
    return if (size == 1) this[0] else null
}

/**
 * Returns single element, or `null` if the array is empty or has more than one element.
 */
public fun ByteArray.singleOrNull(): Byte? {
    return if (size == 1) this[0] else null
}

/**
 * Returns single element, or `null` if the array is empty or has more than one element.
 */
public fun ShortArray.singleOrNull(): Short? {
    return if (size == 1) this[0] else null
}

/**
 * Returns single element, or `null` if the array is empty or has more than one element.
 */
public fun IntArray.singleOrNull(): Int? {
    return if (size == 1) this[0] else null
}

/**
 * Returns single element, or `null` if the array is empty or has more than one element.
 */
public fun LongArray.singleOrNull(): Long? {
    return if (size == 1) this[0] else null
}

/**
 * Returns single element, or `null` if the array is empty or has more than one element.
 */
public fun FloatArray.singleOrNull(): Float? {
    return if (size == 1) this[0] else null
}

/**
 * Returns single element, or `null` if the array is empty or has more than one element.
 */
public fun DoubleArray.singleOrNull(): Double? {
    return if (size == 1) this[0] else null
}

/**
 * Returns single element, or `null` if the array is empty or has more than one element.
 */
public fun BooleanArray.singleOrNull(): Boolean? {
    return if (size == 1) this[0] else null
}

/**
 * Returns single element, or `null` if the array is empty or has more than one element.
 */
public fun CharArray.singleOrNull(): Char? {
    return if (size == 1) this[0] else null
}

/**
 * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
 */
public inline fun <T> Array<out T>.singleOrNull(predicate: (T) -> Boolean): T? {
    var single: T? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
 */
public inline fun ByteArray.singleOrNull(predicate: (Byte) -> Boolean): Byte? {
    var single: Byte? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
 */
public inline fun ShortArray.singleOrNull(predicate: (Short) -> Boolean): Short? {
    var single: Short? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
 */
public inline fun IntArray.singleOrNull(predicate: (Int) -> Boolean): Int? {
    var single: Int? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
 */
public inline fun LongArray.singleOrNull(predicate: (Long) -> Boolean): Long? {
    var single: Long? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
 */
public inline fun FloatArray.singleOrNull(predicate: (Float) -> Boolean): Float? {
    var single: Float? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
 */
public inline fun DoubleArray.singleOrNull(predicate: (Double) -> Boolean): Double? {
    var single: Double? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
 */
public inline fun BooleanArray.singleOrNull(predicate: (Boolean) -> Boolean): Boolean? {
    var single: Boolean? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
 */
public inline fun CharArray.singleOrNull(predicate: (Char) -> Boolean): Char? {
    var single: Char? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns a list containing all elements except first [n] elements.
 */
public fun <T> Array<out T>.drop(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return takeLast((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except first [n] elements.
 */
public fun ByteArray.drop(n: Int): List<Byte> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return takeLast((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except first [n] elements.
 */
public fun ShortArray.drop(n: Int): List<Short> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return takeLast((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except first [n] elements.
 */
public fun IntArray.drop(n: Int): List<Int> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return takeLast((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except first [n] elements.
 */
public fun LongArray.drop(n: Int): List<Long> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return takeLast((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except first [n] elements.
 */
public fun FloatArray.drop(n: Int): List<Float> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return takeLast((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except first [n] elements.
 */
public fun DoubleArray.drop(n: Int): List<Double> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return takeLast((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except first [n] elements.
 */
public fun BooleanArray.drop(n: Int): List<Boolean> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return takeLast((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except first [n] elements.
 */
public fun CharArray.drop(n: Int): List<Char> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return takeLast((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except last [n] elements.
 */
public fun <T> Array<out T>.dropLast(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return take((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except last [n] elements.
 */
public fun ByteArray.dropLast(n: Int): List<Byte> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return take((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except last [n] elements.
 */
public fun ShortArray.dropLast(n: Int): List<Short> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return take((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except last [n] elements.
 */
public fun IntArray.dropLast(n: Int): List<Int> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return take((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except last [n] elements.
 */
public fun LongArray.dropLast(n: Int): List<Long> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return take((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except last [n] elements.
 */
public fun FloatArray.dropLast(n: Int): List<Float> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return take((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except last [n] elements.
 */
public fun DoubleArray.dropLast(n: Int): List<Double> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return take((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except last [n] elements.
 */
public fun BooleanArray.dropLast(n: Int): List<Boolean> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return take((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except last [n] elements.
 */
public fun CharArray.dropLast(n: Int): List<Char> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return take((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except last elements that satisfy the given [predicate].
 */
public inline fun <T> Array<out T>.dropLastWhile(predicate: (T) -> Boolean): List<T> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return take(index + 1)
        }
    }
    return emptyList()
}

/**
 * Returns a list containing all elements except last elements that satisfy the given [predicate].
 */
public inline fun ByteArray.dropLastWhile(predicate: (Byte) -> Boolean): List<Byte> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return take(index + 1)
        }
    }
    return emptyList()
}

/**
 * Returns a list containing all elements except last elements that satisfy the given [predicate].
 */
public inline fun ShortArray.dropLastWhile(predicate: (Short) -> Boolean): List<Short> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return take(index + 1)
        }
    }
    return emptyList()
}

/**
 * Returns a list containing all elements except last elements that satisfy the given [predicate].
 */
public inline fun IntArray.dropLastWhile(predicate: (Int) -> Boolean): List<Int> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return take(index + 1)
        }
    }
    return emptyList()
}

/**
 * Returns a list containing all elements except last elements that satisfy the given [predicate].
 */
public inline fun LongArray.dropLastWhile(predicate: (Long) -> Boolean): List<Long> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return take(index + 1)
        }
    }
    return emptyList()
}

/**
 * Returns a list containing all elements except last elements that satisfy the given [predicate].
 */
public inline fun FloatArray.dropLastWhile(predicate: (Float) -> Boolean): List<Float> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return take(index + 1)
        }
    }
    return emptyList()
}

/**
 * Returns a list containing all elements except last elements that satisfy the given [predicate].
 */
public inline fun DoubleArray.dropLastWhile(predicate: (Double) -> Boolean): List<Double> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return take(index + 1)
        }
    }
    return emptyList()
}

/**
 * Returns a list containing all elements except last elements that satisfy the given [predicate].
 */
public inline fun BooleanArray.dropLastWhile(predicate: (Boolean) -> Boolean): List<Boolean> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return take(index + 1)
        }
    }
    return emptyList()
}

/**
 * Returns a list containing all elements except last elements that satisfy the given [predicate].
 */
public inline fun CharArray.dropLastWhile(predicate: (Char) -> Boolean): List<Char> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return take(index + 1)
        }
    }
    return emptyList()
}

/**
 * Returns a list containing all elements except first elements that satisfy the given [predicate].
 */
public inline fun <T> Array<out T>.dropWhile(predicate: (T) -> Boolean): List<T> {
    var yielding = false
    val list = ArrayList<T>()
    for (item in this)
        if (yielding)
            list.add(item)
        else if (!predicate(item)) {
            list.add(item)
            yielding = true
        }
    return list
}

/**
 * Returns a list containing all elements except first elements that satisfy the given [predicate].
 */
public inline fun ByteArray.dropWhile(predicate: (Byte) -> Boolean): List<Byte> {
    var yielding = false
    val list = ArrayList<Byte>()
    for (item in this)
        if (yielding)
            list.add(item)
        else if (!predicate(item)) {
            list.add(item)
            yielding = true
        }
    return list
}

/**
 * Returns a list containing all elements except first elements that satisfy the given [predicate].
 */
public inline fun ShortArray.dropWhile(predicate: (Short) -> Boolean): List<Short> {
    var yielding = false
    val list = ArrayList<Short>()
    for (item in this)
        if (yielding)
            list.add(item)
        else if (!predicate(item)) {
            list.add(item)
            yielding = true
        }
    return list
}

/**
 * Returns a list containing all elements except first elements that satisfy the given [predicate].
 */
public inline fun IntArray.dropWhile(predicate: (Int) -> Boolean): List<Int> {
    var yielding = false
    val list = ArrayList<Int>()
    for (item in this)
        if (yielding)
            list.add(item)
        else if (!predicate(item)) {
            list.add(item)
            yielding = true
        }
    return list
}

/**
 * Returns a list containing all elements except first elements that satisfy the given [predicate].
 */
public inline fun LongArray.dropWhile(predicate: (Long) -> Boolean): List<Long> {
    var yielding = false
    val list = ArrayList<Long>()
    for (item in this)
        if (yielding)
            list.add(item)
        else if (!predicate(item)) {
            list.add(item)
            yielding = true
        }
    return list
}

/**
 * Returns a list containing all elements except first elements that satisfy the given [predicate].
 */
public inline fun FloatArray.dropWhile(predicate: (Float) -> Boolean): List<Float> {
    var yielding = false
    val list = ArrayList<Float>()
    for (item in this)
        if (yielding)
            list.add(item)
        else if (!predicate(item)) {
            list.add(item)
            yielding = true
        }
    return list
}

/**
 * Returns a list containing all elements except first elements that satisfy the given [predicate].
 */
public inline fun DoubleArray.dropWhile(predicate: (Double) -> Boolean): List<Double> {
    var yielding = false
    val list = ArrayList<Double>()
    for (item in this)
        if (yielding)
            list.add(item)
        else if (!predicate(item)) {
            list.add(item)
            yielding = true
        }
    return list
}

/**
 * Returns a list containing all elements except first elements that satisfy the given [predicate].
 */
public inline fun BooleanArray.dropWhile(predicate: (Boolean) -> Boolean): List<Boolean> {
    var yielding = false
    val list = ArrayList<Boolean>()
    for (item in this)
        if (yielding)
            list.add(item)
        else if (!predicate(item)) {
            list.add(item)
            yielding = true
        }
    return list
}

/**
 * Returns a list containing all elements except first elements that satisfy the given [predicate].
 */
public inline fun CharArray.dropWhile(predicate: (Char) -> Boolean): List<Char> {
    var yielding = false
    val list = ArrayList<Char>()
    for (item in this)
        if (yielding)
            list.add(item)
        else if (!predicate(item)) {
            list.add(item)
            yielding = true
        }
    return list
}

/**
 * Returns a list containing only elements matching the given [predicate].
 */
public inline fun <T> Array<out T>.filter(predicate: (T) -> Boolean): List<T> {
    return filterTo(ArrayList<T>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 */
public inline fun ByteArray.filter(predicate: (Byte) -> Boolean): List<Byte> {
    return filterTo(ArrayList<Byte>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 */
public inline fun ShortArray.filter(predicate: (Short) -> Boolean): List<Short> {
    return filterTo(ArrayList<Short>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 */
public inline fun IntArray.filter(predicate: (Int) -> Boolean): List<Int> {
    return filterTo(ArrayList<Int>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 */
public inline fun LongArray.filter(predicate: (Long) -> Boolean): List<Long> {
    return filterTo(ArrayList<Long>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 */
public inline fun FloatArray.filter(predicate: (Float) -> Boolean): List<Float> {
    return filterTo(ArrayList<Float>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 */
public inline fun DoubleArray.filter(predicate: (Double) -> Boolean): List<Double> {
    return filterTo(ArrayList<Double>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 */
public inline fun BooleanArray.filter(predicate: (Boolean) -> Boolean): List<Boolean> {
    return filterTo(ArrayList<Boolean>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 */
public inline fun CharArray.filter(predicate: (Char) -> Boolean): List<Char> {
    return filterTo(ArrayList<Char>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <T> Array<out T>.filterIndexed(predicate: (index: Int, T) -> Boolean): List<T> {
    return filterIndexedTo(ArrayList<T>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun ByteArray.filterIndexed(predicate: (index: Int, Byte) -> Boolean): List<Byte> {
    return filterIndexedTo(ArrayList<Byte>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun ShortArray.filterIndexed(predicate: (index: Int, Short) -> Boolean): List<Short> {
    return filterIndexedTo(ArrayList<Short>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun IntArray.filterIndexed(predicate: (index: Int, Int) -> Boolean): List<Int> {
    return filterIndexedTo(ArrayList<Int>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun LongArray.filterIndexed(predicate: (index: Int, Long) -> Boolean): List<Long> {
    return filterIndexedTo(ArrayList<Long>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun FloatArray.filterIndexed(predicate: (index: Int, Float) -> Boolean): List<Float> {
    return filterIndexedTo(ArrayList<Float>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun DoubleArray.filterIndexed(predicate: (index: Int, Double) -> Boolean): List<Double> {
    return filterIndexedTo(ArrayList<Double>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun BooleanArray.filterIndexed(predicate: (index: Int, Boolean) -> Boolean): List<Boolean> {
    return filterIndexedTo(ArrayList<Boolean>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun CharArray.filterIndexed(predicate: (index: Int, Char) -> Boolean): List<Char> {
    return filterIndexedTo(ArrayList<Char>(), predicate)
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <T, C : MutableCollection<in T>> Array<out T>.filterIndexedTo(destination: C, predicate: (index: Int, T) -> Boolean): C {
    forEachIndexed { index, element ->
        if (predicate(index, element)) destination.add(element)
    }
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <C : MutableCollection<in Byte>> ByteArray.filterIndexedTo(destination: C, predicate: (index: Int, Byte) -> Boolean): C {
    forEachIndexed { index, element ->
        if (predicate(index, element)) destination.add(element)
    }
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <C : MutableCollection<in Short>> ShortArray.filterIndexedTo(destination: C, predicate: (index: Int, Short) -> Boolean): C {
    forEachIndexed { index, element ->
        if (predicate(index, element)) destination.add(element)
    }
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <C : MutableCollection<in Int>> IntArray.filterIndexedTo(destination: C, predicate: (index: Int, Int) -> Boolean): C {
    forEachIndexed { index, element ->
        if (predicate(index, element)) destination.add(element)
    }
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <C : MutableCollection<in Long>> LongArray.filterIndexedTo(destination: C, predicate: (index: Int, Long) -> Boolean): C {
    forEachIndexed { index, element ->
        if (predicate(index, element)) destination.add(element)
    }
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <C : MutableCollection<in Float>> FloatArray.filterIndexedTo(destination: C, predicate: (index: Int, Float) -> Boolean): C {
    forEachIndexed { index, element ->
        if (predicate(index, element)) destination.add(element)
    }
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <C : MutableCollection<in Double>> DoubleArray.filterIndexedTo(destination: C, predicate: (index: Int, Double) -> Boolean): C {
    forEachIndexed { index, element ->
        if (predicate(index, element)) destination.add(element)
    }
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <C : MutableCollection<in Boolean>> BooleanArray.filterIndexedTo(destination: C, predicate: (index: Int, Boolean) -> Boolean): C {
    forEachIndexed { index, element ->
        if (predicate(index, element)) destination.add(element)
    }
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <C : MutableCollection<in Char>> CharArray.filterIndexedTo(destination: C, predicate: (index: Int, Char) -> Boolean): C {
    forEachIndexed { index, element ->
        if (predicate(index, element)) destination.add(element)
    }
    return destination
}

/**
 * Returns a list containing all elements that are instances of specified type parameter R.
 */
public inline fun <reified R> Array<*>.filterIsInstance(): List<@kotlin.internal.NoInfer R> {
    return filterIsInstanceTo(ArrayList<R>())
}

/**
 * Appends all elements that are instances of specified type parameter R to the given [destination].
 */
public inline fun <reified R, C : MutableCollection<in R>> Array<*>.filterIsInstanceTo(destination: C): C {
    for (element in this) if (element is R) destination.add(element)
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 */
public inline fun <T, C : MutableCollection<in T>> Array<out T>.filterTo(destination: C, predicate: (T) -> Boolean): C {
    for (element in this) if (predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Byte>> ByteArray.filterTo(destination: C, predicate: (Byte) -> Boolean): C {
    for (element in this) if (predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Short>> ShortArray.filterTo(destination: C, predicate: (Short) -> Boolean): C {
    for (element in this) if (predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Int>> IntArray.filterTo(destination: C, predicate: (Int) -> Boolean): C {
    for (element in this) if (predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Long>> LongArray.filterTo(destination: C, predicate: (Long) -> Boolean): C {
    for (element in this) if (predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Float>> FloatArray.filterTo(destination: C, predicate: (Float) -> Boolean): C {
    for (element in this) if (predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Double>> DoubleArray.filterTo(destination: C, predicate: (Double) -> Boolean): C {
    for (element in this) if (predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Boolean>> BooleanArray.filterTo(destination: C, predicate: (Boolean) -> Boolean): C {
    for (element in this) if (predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 */
public inline fun <C : MutableCollection<in Char>> CharArray.filterTo(destination: C, predicate: (Char) -> Boolean): C {
    for (element in this) if (predicate(element)) destination.add(element)
    return destination
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun <T> Array<out T>.slice(indices: IntRange): List<T> {
    if (indices.isEmpty()) return listOf()
    return copyOfRange(indices.start, indices.endInclusive + 1).asList()
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun ByteArray.slice(indices: IntRange): List<Byte> {
    if (indices.isEmpty()) return listOf()
    return copyOfRange(indices.start, indices.endInclusive + 1).asList()
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun ShortArray.slice(indices: IntRange): List<Short> {
    if (indices.isEmpty()) return listOf()
    return copyOfRange(indices.start, indices.endInclusive + 1).asList()
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun IntArray.slice(indices: IntRange): List<Int> {
    if (indices.isEmpty()) return listOf()
    return copyOfRange(indices.start, indices.endInclusive + 1).asList()
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun LongArray.slice(indices: IntRange): List<Long> {
    if (indices.isEmpty()) return listOf()
    return copyOfRange(indices.start, indices.endInclusive + 1).asList()
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun FloatArray.slice(indices: IntRange): List<Float> {
    if (indices.isEmpty()) return listOf()
    return copyOfRange(indices.start, indices.endInclusive + 1).asList()
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun DoubleArray.slice(indices: IntRange): List<Double> {
    if (indices.isEmpty()) return listOf()
    return copyOfRange(indices.start, indices.endInclusive + 1).asList()
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun BooleanArray.slice(indices: IntRange): List<Boolean> {
    if (indices.isEmpty()) return listOf()
    return copyOfRange(indices.start, indices.endInclusive + 1).asList()
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun CharArray.slice(indices: IntRange): List<Char> {
    if (indices.isEmpty()) return listOf()
    return copyOfRange(indices.start, indices.endInclusive + 1).asList()
}

/**
 * Returns a list containing elements at specified [indices].
 */
public fun <T> Array<out T>.slice(indices: Iterable<Int>): List<T> {
    val size = indices.collectionSizeOrDefault(10)
    if (size == 0) return emptyList()
    val list = ArrayList<T>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
public fun ByteArray.slice(indices: Iterable<Int>): List<Byte> {
    val size = indices.collectionSizeOrDefault(10)
    if (size == 0) return emptyList()
    val list = ArrayList<Byte>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
public fun ShortArray.slice(indices: Iterable<Int>): List<Short> {
    val size = indices.collectionSizeOrDefault(10)
    if (size == 0) return emptyList()
    val list = ArrayList<Short>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
public fun IntArray.slice(indices: Iterable<Int>): List<Int> {
    val size = indices.collectionSizeOrDefault(10)
    if (size == 0) return emptyList()
    val list = ArrayList<Int>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
public fun LongArray.slice(indices: Iterable<Int>): List<Long> {
    val size = indices.collectionSizeOrDefault(10)
    if (size == 0) return emptyList()
    val list = ArrayList<Long>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
public fun FloatArray.slice(indices: Iterable<Int>): List<Float> {
    val size = indices.collectionSizeOrDefault(10)
    if (size == 0) return emptyList()
    val list = ArrayList<Float>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
public fun DoubleArray.slice(indices: Iterable<Int>): List<Double> {
    val size = indices.collectionSizeOrDefault(10)
    if (size == 0) return emptyList()
    val list = ArrayList<Double>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
public fun BooleanArray.slice(indices: Iterable<Int>): List<Boolean> {
    val size = indices.collectionSizeOrDefault(10)
    if (size == 0) return emptyList()
    val list = ArrayList<Boolean>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing elements at specified [indices].
 */
public fun CharArray.slice(indices: Iterable<Int>): List<Char> {
    val size = indices.collectionSizeOrDefault(10)
    if (size == 0) return emptyList()
    val list = ArrayList<Char>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns an array containing elements of this array at specified [indices].
 */
public fun <T> Array<T>.sliceArray(indices: Collection<Int>): Array<T> {
    val result = arrayOfUninitializedElements<T>(indices.size)
    var targetIndex = 0
    for (sourceIndex in indices) {
        result[targetIndex++] = this[sourceIndex]
    }
    return result
}

/**
 * Returns an array containing elements of this array at specified [indices].
 */
public fun ByteArray.sliceArray(indices: Collection<Int>): ByteArray {
    val result = ByteArray(indices.size)
    var targetIndex = 0
    for (sourceIndex in indices) {
        result[targetIndex++] = this[sourceIndex]
    }
    return result
}

/**
 * Returns an array containing elements of this array at specified [indices].
 */
public fun ShortArray.sliceArray(indices: Collection<Int>): ShortArray {
    val result = ShortArray(indices.size)
    var targetIndex = 0
    for (sourceIndex in indices) {
        result[targetIndex++] = this[sourceIndex]
    }
    return result
}

/**
 * Returns an array containing elements of this array at specified [indices].
 */
public fun IntArray.sliceArray(indices: Collection<Int>): IntArray {
    val result = IntArray(indices.size)
    var targetIndex = 0
    for (sourceIndex in indices) {
        result[targetIndex++] = this[sourceIndex]
    }
    return result
}

/**
 * Returns an array containing elements of this array at specified [indices].
 */
public fun LongArray.sliceArray(indices: Collection<Int>): LongArray {
    val result = LongArray(indices.size)
    var targetIndex = 0
    for (sourceIndex in indices) {
        result[targetIndex++] = this[sourceIndex]
    }
    return result
}

/**
 * Returns an array containing elements of this array at specified [indices].
 */
public fun FloatArray.sliceArray(indices: Collection<Int>): FloatArray {
    val result = FloatArray(indices.size)
    var targetIndex = 0
    for (sourceIndex in indices) {
        result[targetIndex++] = this[sourceIndex]
    }
    return result
}

/**
 * Returns an array containing elements of this array at specified [indices].
 */
public fun DoubleArray.sliceArray(indices: Collection<Int>): DoubleArray {
    val result = DoubleArray(indices.size)
    var targetIndex = 0
    for (sourceIndex in indices) {
        result[targetIndex++] = this[sourceIndex]
    }
    return result
}

/**
 * Returns an array containing elements of this array at specified [indices].
 */
public fun BooleanArray.sliceArray(indices: Collection<Int>): BooleanArray {
    val result = BooleanArray(indices.size)
    var targetIndex = 0
    for (sourceIndex in indices) {
        result[targetIndex++] = this[sourceIndex]
    }
    return result
}

/**
 * Returns an array containing elements of this array at specified [indices].
 */
public fun CharArray.sliceArray(indices: Collection<Int>): CharArray {
    val result = CharArray(indices.size)
    var targetIndex = 0
    for (sourceIndex in indices) {
        result[targetIndex++] = this[sourceIndex]
    }
    return result
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun <T> Array<T>.sliceArray(indices: IntRange): Array<T> {
    if (indices.isEmpty()) return copyOfRange(0, 0)
    return copyOfRange(indices.start, indices.endInclusive + 1)
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun ByteArray.sliceArray(indices: IntRange): ByteArray {
    if (indices.isEmpty()) return ByteArray(0)
    return copyOfRange(indices.start, indices.endInclusive + 1)
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun ShortArray.sliceArray(indices: IntRange): ShortArray {
    if (indices.isEmpty()) return ShortArray(0)
    return copyOfRange(indices.start, indices.endInclusive + 1)
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun IntArray.sliceArray(indices: IntRange): IntArray {
    if (indices.isEmpty()) return IntArray(0)
    return copyOfRange(indices.start, indices.endInclusive + 1)
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun LongArray.sliceArray(indices: IntRange): LongArray {
    if (indices.isEmpty()) return LongArray(0)
    return copyOfRange(indices.start, indices.endInclusive + 1)
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun FloatArray.sliceArray(indices: IntRange): FloatArray {
    if (indices.isEmpty()) return FloatArray(0)
    return copyOfRange(indices.start, indices.endInclusive + 1)
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun DoubleArray.sliceArray(indices: IntRange): DoubleArray {
    if (indices.isEmpty()) return DoubleArray(0)
    return copyOfRange(indices.start, indices.endInclusive + 1)
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun BooleanArray.sliceArray(indices: IntRange): BooleanArray {
    if (indices.isEmpty()) return BooleanArray(0)
    return copyOfRange(indices.start, indices.endInclusive + 1)
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun CharArray.sliceArray(indices: IntRange): CharArray {
    if (indices.isEmpty()) return CharArray(0)
    return copyOfRange(indices.start, indices.endInclusive + 1)
}

/**
 * Returns a list containing first [n] elements.
 */
public fun <T> Array<out T>.take(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    if (n >= size) return toList()
    if (n == 1) return listOf(this[0])
    var count = 0
    val list = ArrayList<T>(n)
    for (item in this) {
        if (count++ == n)
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first [n] elements.
 */
public fun ByteArray.take(n: Int): List<Byte> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    if (n >= size) return toList()
    if (n == 1) return listOf(this[0])
    var count = 0
    val list = ArrayList<Byte>(n)
    for (item in this) {
        if (count++ == n)
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first [n] elements.
 */
public fun ShortArray.take(n: Int): List<Short> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    if (n >= size) return toList()
    if (n == 1) return listOf(this[0])
    var count = 0
    val list = ArrayList<Short>(n)
    for (item in this) {
        if (count++ == n)
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first [n] elements.
 */
public fun IntArray.take(n: Int): List<Int> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    if (n >= size) return toList()
    if (n == 1) return listOf(this[0])
    var count = 0
    val list = ArrayList<Int>(n)
    for (item in this) {
        if (count++ == n)
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first [n] elements.
 */
public fun LongArray.take(n: Int): List<Long> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    if (n >= size) return toList()
    if (n == 1) return listOf(this[0])
    var count = 0
    val list = ArrayList<Long>(n)
    for (item in this) {
        if (count++ == n)
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first [n] elements.
 */
public fun FloatArray.take(n: Int): List<Float> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    if (n >= size) return toList()
    if (n == 1) return listOf(this[0])
    var count = 0
    val list = ArrayList<Float>(n)
    for (item in this) {
        if (count++ == n)
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first [n] elements.
 */
public fun DoubleArray.take(n: Int): List<Double> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    if (n >= size) return toList()
    if (n == 1) return listOf(this[0])
    var count = 0
    val list = ArrayList<Double>(n)
    for (item in this) {
        if (count++ == n)
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first [n] elements.
 */
public fun BooleanArray.take(n: Int): List<Boolean> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    if (n >= size) return toList()
    if (n == 1) return listOf(this[0])
    var count = 0
    val list = ArrayList<Boolean>(n)
    for (item in this) {
        if (count++ == n)
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first [n] elements.
 */
public fun CharArray.take(n: Int): List<Char> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    if (n >= size) return toList()
    if (n == 1) return listOf(this[0])
    var count = 0
    val list = ArrayList<Char>(n)
    for (item in this) {
        if (count++ == n)
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing last [n] elements.
 */
public fun <T> Array<out T>.takeLast(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    val size = size
    if (n >= size) return toList()
    if (n == 1) return listOf(this[size - 1])
    val list = ArrayList<T>(n)
    for (index in size - n .. size - 1)
        list.add(this[index])
    return list
}

/**
 * Returns a list containing last [n] elements.
 */
public fun ByteArray.takeLast(n: Int): List<Byte> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    val size = size
    if (n >= size) return toList()
    if (n == 1) return listOf(this[size - 1])
    val list = ArrayList<Byte>(n)
    for (index in size - n .. size - 1)
        list.add(this[index])
    return list
}

/**
 * Returns a list containing last [n] elements.
 */
public fun ShortArray.takeLast(n: Int): List<Short> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    val size = size
    if (n >= size) return toList()
    if (n == 1) return listOf(this[size - 1])
    val list = ArrayList<Short>(n)
    for (index in size - n .. size - 1)
        list.add(this[index])
    return list
}

/**
 * Returns a list containing last [n] elements.
 */
public fun IntArray.takeLast(n: Int): List<Int> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    val size = size
    if (n >= size) return toList()
    if (n == 1) return listOf(this[size - 1])
    val list = ArrayList<Int>(n)
    for (index in size - n .. size - 1)
        list.add(this[index])
    return list
}

/**
 * Returns a list containing last [n] elements.
 */
public fun LongArray.takeLast(n: Int): List<Long> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    val size = size
    if (n >= size) return toList()
    if (n == 1) return listOf(this[size - 1])
    val list = ArrayList<Long>(n)
    for (index in size - n .. size - 1)
        list.add(this[index])
    return list
}

/**
 * Returns a list containing last [n] elements.
 */
public fun FloatArray.takeLast(n: Int): List<Float> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    val size = size
    if (n >= size) return toList()
    if (n == 1) return listOf(this[size - 1])
    val list = ArrayList<Float>(n)
    for (index in size - n .. size - 1)
        list.add(this[index])
    return list
}

/**
 * Returns a list containing last [n] elements.
 */
public fun DoubleArray.takeLast(n: Int): List<Double> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    val size = size
    if (n >= size) return toList()
    if (n == 1) return listOf(this[size - 1])
    val list = ArrayList<Double>(n)
    for (index in size - n .. size - 1)
        list.add(this[index])
    return list
}

/**
 * Returns a list containing last [n] elements.
 */
public fun BooleanArray.takeLast(n: Int): List<Boolean> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    val size = size
    if (n >= size) return toList()
    if (n == 1) return listOf(this[size - 1])
    val list = ArrayList<Boolean>(n)
    for (index in size - n .. size - 1)
        list.add(this[index])
    return list
}

/**
 * Returns a list containing last [n] elements.
 */
public fun CharArray.takeLast(n: Int): List<Char> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    val size = size
    if (n >= size) return toList()
    if (n == 1) return listOf(this[size - 1])
    val list = ArrayList<Char>(n)
    for (index in size - n .. size - 1)
        list.add(this[index])
    return list
}

/**
 * Returns a list containing last elements satisfying the given [predicate].
 */
public inline fun <T> Array<out T>.takeLastWhile(predicate: (T) -> Boolean): List<T> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return drop(index + 1)
        }
    }
    return toList()
}

/**
 * Returns a list containing last elements satisfying the given [predicate].
 */
public inline fun ByteArray.takeLastWhile(predicate: (Byte) -> Boolean): List<Byte> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return drop(index + 1)
        }
    }
    return toList()
}

/**
 * Returns a list containing last elements satisfying the given [predicate].
 */
public inline fun ShortArray.takeLastWhile(predicate: (Short) -> Boolean): List<Short> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return drop(index + 1)
        }
    }
    return toList()
}

/**
 * Returns a list containing last elements satisfying the given [predicate].
 */
public inline fun IntArray.takeLastWhile(predicate: (Int) -> Boolean): List<Int> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return drop(index + 1)
        }
    }
    return toList()
}

/**
 * Returns a list containing last elements satisfying the given [predicate].
 */
public inline fun LongArray.takeLastWhile(predicate: (Long) -> Boolean): List<Long> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return drop(index + 1)
        }
    }
    return toList()
}

/**
 * Returns a list containing last elements satisfying the given [predicate].
 */
public inline fun FloatArray.takeLastWhile(predicate: (Float) -> Boolean): List<Float> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return drop(index + 1)
        }
    }
    return toList()
}

/**
 * Returns a list containing last elements satisfying the given [predicate].
 */
public inline fun DoubleArray.takeLastWhile(predicate: (Double) -> Boolean): List<Double> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return drop(index + 1)
        }
    }
    return toList()
}

/**
 * Returns a list containing last elements satisfying the given [predicate].
 */
public inline fun BooleanArray.takeLastWhile(predicate: (Boolean) -> Boolean): List<Boolean> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return drop(index + 1)
        }
    }
    return toList()
}

/**
 * Returns a list containing last elements satisfying the given [predicate].
 */
public inline fun CharArray.takeLastWhile(predicate: (Char) -> Boolean): List<Char> {
    for (index in lastIndex downTo 0) {
        if (!predicate(this[index])) {
            return drop(index + 1)
        }
    }
    return toList()
}

/**
 * Returns a list containing first elements satisfying the given [predicate].
 */
public inline fun <T> Array<out T>.takeWhile(predicate: (T) -> Boolean): List<T> {
    val list = ArrayList<T>()
    for (item in this) {
        if (!predicate(item))
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first elements satisfying the given [predicate].
 */
public inline fun ByteArray.takeWhile(predicate: (Byte) -> Boolean): List<Byte> {
    val list = ArrayList<Byte>()
    for (item in this) {
        if (!predicate(item))
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first elements satisfying the given [predicate].
 */
public inline fun ShortArray.takeWhile(predicate: (Short) -> Boolean): List<Short> {
    val list = ArrayList<Short>()
    for (item in this) {
        if (!predicate(item))
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first elements satisfying the given [predicate].
 */
public inline fun IntArray.takeWhile(predicate: (Int) -> Boolean): List<Int> {
    val list = ArrayList<Int>()
    for (item in this) {
        if (!predicate(item))
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first elements satisfying the given [predicate].
 */
public inline fun LongArray.takeWhile(predicate: (Long) -> Boolean): List<Long> {
    val list = ArrayList<Long>()
    for (item in this) {
        if (!predicate(item))
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first elements satisfying the given [predicate].
 */
public inline fun FloatArray.takeWhile(predicate: (Float) -> Boolean): List<Float> {
    val list = ArrayList<Float>()
    for (item in this) {
        if (!predicate(item))
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first elements satisfying the given [predicate].
 */
public inline fun DoubleArray.takeWhile(predicate: (Double) -> Boolean): List<Double> {
    val list = ArrayList<Double>()
    for (item in this) {
        if (!predicate(item))
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first elements satisfying the given [predicate].
 */
public inline fun BooleanArray.takeWhile(predicate: (Boolean) -> Boolean): List<Boolean> {
    val list = ArrayList<Boolean>()
    for (item in this) {
        if (!predicate(item))
            break
        list.add(item)
    }
    return list
}

/**
 * Returns a list containing first elements satisfying the given [predicate].
 */
public inline fun CharArray.takeWhile(predicate: (Char) -> Boolean): List<Char> {
    val list = ArrayList<Char>()
    for (item in this) {
        if (!predicate(item))
            break
        list.add(item)
    }
    return list
}

/**
 * Reverses elements in the array in-place.
 */
public fun <T> Array<T>.reverse(): Unit {
    val midPoint = (size / 2) - 1
    if (midPoint < 0) return
    var reverseIndex = lastIndex
    for (index in 0..midPoint) {
        val tmp = this[index]
        this[index] = this[reverseIndex]
        this[reverseIndex] = tmp
        reverseIndex--
    }
}

/**
 * Reverses elements in the array in-place.
 */
public fun ByteArray.reverse(): Unit {
    val midPoint = (size / 2) - 1
    if (midPoint < 0) return
    var reverseIndex = lastIndex
    for (index in 0..midPoint) {
        val tmp = this[index]
        this[index] = this[reverseIndex]
        this[reverseIndex] = tmp
        reverseIndex--
    }
}

/**
 * Reverses elements in the array in-place.
 */
public fun ShortArray.reverse(): Unit {
    val midPoint = (size / 2) - 1
    if (midPoint < 0) return
    var reverseIndex = lastIndex
    for (index in 0..midPoint) {
        val tmp = this[index]
        this[index] = this[reverseIndex]
        this[reverseIndex] = tmp
        reverseIndex--
    }
}

/**
 * Reverses elements in the array in-place.
 */
public fun IntArray.reverse(): Unit {
    val midPoint = (size / 2) - 1
    if (midPoint < 0) return
    var reverseIndex = lastIndex
    for (index in 0..midPoint) {
        val tmp = this[index]
        this[index] = this[reverseIndex]
        this[reverseIndex] = tmp
        reverseIndex--
    }
}

/**
 * Reverses elements in the array in-place.
 */
public fun LongArray.reverse(): Unit {
    val midPoint = (size / 2) - 1
    if (midPoint < 0) return
    var reverseIndex = lastIndex
    for (index in 0..midPoint) {
        val tmp = this[index]
        this[index] = this[reverseIndex]
        this[reverseIndex] = tmp
        reverseIndex--
    }
}

/**
 * Reverses elements in the array in-place.
 */
public fun FloatArray.reverse(): Unit {
    val midPoint = (size / 2) - 1
    if (midPoint < 0) return
    var reverseIndex = lastIndex
    for (index in 0..midPoint) {
        val tmp = this[index]
        this[index] = this[reverseIndex]
        this[reverseIndex] = tmp
        reverseIndex--
    }
}

/**
 * Reverses elements in the array in-place.
 */
public fun DoubleArray.reverse(): Unit {
    val midPoint = (size / 2) - 1
    if (midPoint < 0) return
    var reverseIndex = lastIndex
    for (index in 0..midPoint) {
        val tmp = this[index]
        this[index] = this[reverseIndex]
        this[reverseIndex] = tmp
        reverseIndex--
    }
}

/**
 * Reverses elements in the array in-place.
 */
public fun BooleanArray.reverse(): Unit {
    val midPoint = (size / 2) - 1
    if (midPoint < 0) return
    var reverseIndex = lastIndex
    for (index in 0..midPoint) {
        val tmp = this[index]
        this[index] = this[reverseIndex]
        this[reverseIndex] = tmp
        reverseIndex--
    }
}

/**
 * Reverses elements in the array in-place.
 */
public fun CharArray.reverse(): Unit {
    val midPoint = (size / 2) - 1
    if (midPoint < 0) return
    var reverseIndex = lastIndex
    for (index in 0..midPoint) {
        val tmp = this[index]
        this[index] = this[reverseIndex]
        this[reverseIndex] = tmp
        reverseIndex--
    }
}

/**
 * Returns a list with elements in reversed order.
 */
public fun <T> Array<out T>.reversed(): List<T> {
    if (isEmpty()) return emptyList()
    val list = toMutableList()
    list.reverse()
    return list
}

/**
 * Returns a list with elements in reversed order.
 */
public fun ByteArray.reversed(): List<Byte> {
    if (isEmpty()) return emptyList()
    val list = toMutableList()
    list.reverse()
    return list
}

/**
 * Returns a list with elements in reversed order.
 */
public fun ShortArray.reversed(): List<Short> {
    if (isEmpty()) return emptyList()
    val list = toMutableList()
    list.reverse()
    return list
}

/**
 * Returns a list with elements in reversed order.
 */
public fun IntArray.reversed(): List<Int> {
    if (isEmpty()) return emptyList()
    val list = toMutableList()
    list.reverse()
    return list
}

/**
 * Returns a list with elements in reversed order.
 */
public fun LongArray.reversed(): List<Long> {
    if (isEmpty()) return emptyList()
    val list = toMutableList()
    list.reverse()
    return list
}

/**
 * Returns a list with elements in reversed order.
 */
public fun FloatArray.reversed(): List<Float> {
    if (isEmpty()) return emptyList()
    val list = toMutableList()
    list.reverse()
    return list
}

/**
 * Returns a list with elements in reversed order.
 */
public fun DoubleArray.reversed(): List<Double> {
    if (isEmpty()) return emptyList()
    val list = toMutableList()
    list.reverse()
    return list
}

/**
 * Returns a list with elements in reversed order.
 */
public fun BooleanArray.reversed(): List<Boolean> {
    if (isEmpty()) return emptyList()
    val list = toMutableList()
    list.reverse()
    return list
}

/**
 * Returns a list with elements in reversed order.
 */
public fun CharArray.reversed(): List<Char> {
    if (isEmpty()) return emptyList()
    val list = toMutableList()
    list.reverse()
    return list
}

/**
 * Returns an array with elements of this array in reversed order.
 */
public fun <T> Array<T>.reversedArray(): Array<T> {
    if (isEmpty()) return this
    val result = arrayOfUninitializedElements<T>(size)
    val lastIndex = lastIndex
    for (i in 0..lastIndex)
        result[lastIndex - i] = this[i]
    return result
}

/**
 * Returns an array with elements of this array in reversed order.
 */
public fun ByteArray.reversedArray(): ByteArray {
    if (isEmpty()) return this
    val result = ByteArray(size)
    val lastIndex = lastIndex
    for (i in 0..lastIndex)
        result[lastIndex - i] = this[i]
    return result
}

/**
 * Returns an array with elements of this array in reversed order.
 */
public fun ShortArray.reversedArray(): ShortArray {
    if (isEmpty()) return this
    val result = ShortArray(size)
    val lastIndex = lastIndex
    for (i in 0..lastIndex)
        result[lastIndex - i] = this[i]
    return result
}

/**
 * Returns an array with elements of this array in reversed order.
 */
public fun IntArray.reversedArray(): IntArray {
    if (isEmpty()) return this
    val result = IntArray(size)
    val lastIndex = lastIndex
    for (i in 0..lastIndex)
        result[lastIndex - i] = this[i]
    return result
}

/**
 * Returns an array with elements of this array in reversed order.
 */
public fun LongArray.reversedArray(): LongArray {
    if (isEmpty()) return this
    val result = LongArray(size)
    val lastIndex = lastIndex
    for (i in 0..lastIndex)
        result[lastIndex - i] = this[i]
    return result
}

/**
 * Returns an array with elements of this array in reversed order.
 */
public fun FloatArray.reversedArray(): FloatArray {
    if (isEmpty()) return this
    val result = FloatArray(size)
    val lastIndex = lastIndex
    for (i in 0..lastIndex)
        result[lastIndex - i] = this[i]
    return result
}

/**
 * Returns an array with elements of this array in reversed order.
 */
public fun DoubleArray.reversedArray(): DoubleArray {
    if (isEmpty()) return this
    val result = DoubleArray(size)
    val lastIndex = lastIndex
    for (i in 0..lastIndex)
        result[lastIndex - i] = this[i]
    return result
}

/**
 * Returns an array with elements of this array in reversed order.
 */
public fun BooleanArray.reversedArray(): BooleanArray {
    if (isEmpty()) return this
    val result = BooleanArray(size)
    val lastIndex = lastIndex
    for (i in 0..lastIndex)
        result[lastIndex - i] = this[i]
    return result
}

/**
 * Returns an array with elements of this array in reversed order.
 */
public fun CharArray.reversedArray(): CharArray {
    if (isEmpty()) return this
    val result = CharArray(size)
    val lastIndex = lastIndex
    for (i in 0..lastIndex)
        result[lastIndex - i] = this[i]
    return result
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original array.
 */
public inline fun <T, R> Array<out T>.map(transform: (T) -> R): List<R> {
    return mapTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original array.
 */
public inline fun <R> ByteArray.map(transform: (Byte) -> R): List<R> {
    return mapTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original array.
 */
public inline fun <R> ShortArray.map(transform: (Short) -> R): List<R> {
    return mapTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original array.
 */
public inline fun <R> IntArray.map(transform: (Int) -> R): List<R> {
    return mapTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original array.
 */
public inline fun <R> LongArray.map(transform: (Long) -> R): List<R> {
    return mapTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original array.
 */
public inline fun <R> FloatArray.map(transform: (Float) -> R): List<R> {
    return mapTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original array.
 */
public inline fun <R> DoubleArray.map(transform: (Double) -> R): List<R> {
    return mapTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original array.
 */
public inline fun <R> BooleanArray.map(transform: (Boolean) -> R): List<R> {
    return mapTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original array.
 */
public inline fun <R> CharArray.map(transform: (Char) -> R): List<R> {
    return mapTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element and its index in the original array.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <T, R> Array<out T>.mapIndexed(transform: (index: Int, T) -> R): List<R> {
    return mapIndexedTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element and its index in the original array.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R> ByteArray.mapIndexed(transform: (index: Int, Byte) -> R): List<R> {
    return mapIndexedTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element and its index in the original array.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R> ShortArray.mapIndexed(transform: (index: Int, Short) -> R): List<R> {
    return mapIndexedTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element and its index in the original array.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R> IntArray.mapIndexed(transform: (index: Int, Int) -> R): List<R> {
    return mapIndexedTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element and its index in the original array.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R> LongArray.mapIndexed(transform: (index: Int, Long) -> R): List<R> {
    return mapIndexedTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element and its index in the original array.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R> FloatArray.mapIndexed(transform: (index: Int, Float) -> R): List<R> {
    return mapIndexedTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element and its index in the original array.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R> DoubleArray.mapIndexed(transform: (index: Int, Double) -> R): List<R> {
    return mapIndexedTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element and its index in the original array.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R> BooleanArray.mapIndexed(transform: (index: Int, Boolean) -> R): List<R> {
    return mapIndexedTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element and its index in the original array.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R> CharArray.mapIndexed(transform: (index: Int, Char) -> R): List<R> {
    return mapIndexedTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing only the non-null results of applying the given [transform] function
 * to each element and its index in the original array.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <T, R : Any> Array<out T>.mapIndexedNotNull(transform: (index: Int, T) -> R?): List<R> {
    return mapIndexedNotNullTo(ArrayList<R>(), transform)
}

/**
 * Applies the given [transform] function to each element and its index in the original array
 * and appends only the non-null results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <T, R : Any, C : MutableCollection<in R>> Array<out T>.mapIndexedNotNullTo(destination: C, transform: (index: Int, T) -> R?): C {
    forEachIndexed { index, element -> transform(index, element)?.let { destination.add(it) } }
    return destination
}

/**
 * Applies the given [transform] function to each element and its index in the original array
 * and appends the results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <T, R, C : MutableCollection<in R>> Array<out T>.mapIndexedTo(destination: C, transform: (index: Int, T) -> R): C {
    var index = 0
    for (item in this)
        destination.add(transform(index++, item))
    return destination
}

/**
 * Applies the given [transform] function to each element and its index in the original array
 * and appends the results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R, C : MutableCollection<in R>> ByteArray.mapIndexedTo(destination: C, transform: (index: Int, Byte) -> R): C {
    var index = 0
    for (item in this)
        destination.add(transform(index++, item))
    return destination
}

/**
 * Applies the given [transform] function to each element and its index in the original array
 * and appends the results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R, C : MutableCollection<in R>> ShortArray.mapIndexedTo(destination: C, transform: (index: Int, Short) -> R): C {
    var index = 0
    for (item in this)
        destination.add(transform(index++, item))
    return destination
}

/**
 * Applies the given [transform] function to each element and its index in the original array
 * and appends the results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R, C : MutableCollection<in R>> IntArray.mapIndexedTo(destination: C, transform: (index: Int, Int) -> R): C {
    var index = 0
    for (item in this)
        destination.add(transform(index++, item))
    return destination
}

/**
 * Applies the given [transform] function to each element and its index in the original array
 * and appends the results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R, C : MutableCollection<in R>> LongArray.mapIndexedTo(destination: C, transform: (index: Int, Long) -> R): C {
    var index = 0
    for (item in this)
        destination.add(transform(index++, item))
    return destination
}

/**
 * Applies the given [transform] function to each element and its index in the original array
 * and appends the results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R, C : MutableCollection<in R>> FloatArray.mapIndexedTo(destination: C, transform: (index: Int, Float) -> R): C {
    var index = 0
    for (item in this)
        destination.add(transform(index++, item))
    return destination
}

/**
 * Applies the given [transform] function to each element and its index in the original array
 * and appends the results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R, C : MutableCollection<in R>> DoubleArray.mapIndexedTo(destination: C, transform: (index: Int, Double) -> R): C {
    var index = 0
    for (item in this)
        destination.add(transform(index++, item))
    return destination
}

/**
 * Applies the given [transform] function to each element and its index in the original array
 * and appends the results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R, C : MutableCollection<in R>> BooleanArray.mapIndexedTo(destination: C, transform: (index: Int, Boolean) -> R): C {
    var index = 0
    for (item in this)
        destination.add(transform(index++, item))
    return destination
}

/**
 * Applies the given [transform] function to each element and its index in the original array
 * and appends the results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <R, C : MutableCollection<in R>> CharArray.mapIndexedTo(destination: C, transform: (index: Int, Char) -> R): C {
    var index = 0
    for (item in this)
        destination.add(transform(index++, item))
    return destination
}

/**
 * Returns a list containing only the non-null results of applying the given [transform] function
 * to each element in the original array.
 */
public inline fun <T, R : Any> Array<out T>.mapNotNull(transform: (T) -> R?): List<R> {
    return mapNotNullTo(ArrayList<R>(), transform)
}

/**
 * Applies the given [transform] function to each element in the original array
 * and appends only the non-null results to the given [destination].
 */
public inline fun <T, R : Any, C : MutableCollection<in R>> Array<out T>.mapNotNullTo(destination: C, transform: (T) -> R?): C {
    forEach { element -> transform(element)?.let { destination.add(it) } }
    return destination
}

/**
 * Applies the given [transform] function to each element of the original array
 * and appends the results to the given [destination].
 */
public inline fun <T, R, C : MutableCollection<in R>> Array<out T>.mapTo(destination: C, transform: (T) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Applies the given [transform] function to each element of the original array
 * and appends the results to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> ByteArray.mapTo(destination: C, transform: (Byte) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Applies the given [transform] function to each element of the original array
 * and appends the results to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> ShortArray.mapTo(destination: C, transform: (Short) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Applies the given [transform] function to each element of the original array
 * and appends the results to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> IntArray.mapTo(destination: C, transform: (Int) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Applies the given [transform] function to each element of the original array
 * and appends the results to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> LongArray.mapTo(destination: C, transform: (Long) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Applies the given [transform] function to each element of the original array
 * and appends the results to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> FloatArray.mapTo(destination: C, transform: (Float) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Applies the given [transform] function to each element of the original array
 * and appends the results to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> DoubleArray.mapTo(destination: C, transform: (Double) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Applies the given [transform] function to each element of the original array
 * and appends the results to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> BooleanArray.mapTo(destination: C, transform: (Boolean) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Applies the given [transform] function to each element of the original array
 * and appends the results to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> CharArray.mapTo(destination: C, transform: (Char) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Returns a lazy [Iterable] of [IndexedValue] for each element of the original array.
 */
public fun <T> Array<out T>.withIndex(): Iterable<IndexedValue<T>> {
    return IndexingIterable { iterator() }
}

/**
 * Returns a lazy [Iterable] of [IndexedValue] for each element of the original array.
 */
public fun ByteArray.withIndex(): Iterable<IndexedValue<Byte>> {
    return IndexingIterable { iterator() }
}

/**
 * Returns a lazy [Iterable] of [IndexedValue] for each element of the original array.
 */
public fun ShortArray.withIndex(): Iterable<IndexedValue<Short>> {
    return IndexingIterable { iterator() }
}

/**
 * Returns a lazy [Iterable] of [IndexedValue] for each element of the original array.
 */
public fun IntArray.withIndex(): Iterable<IndexedValue<Int>> {
    return IndexingIterable { iterator() }
}

/**
 * Returns a lazy [Iterable] of [IndexedValue] for each element of the original array.
 */
public fun LongArray.withIndex(): Iterable<IndexedValue<Long>> {
    return IndexingIterable { iterator() }
}

/**
 * Returns a lazy [Iterable] of [IndexedValue] for each element of the original array.
 */
public fun FloatArray.withIndex(): Iterable<IndexedValue<Float>> {
    return IndexingIterable { iterator() }
}

/**
 * Returns a lazy [Iterable] of [IndexedValue] for each element of the original array.
 */
public fun DoubleArray.withIndex(): Iterable<IndexedValue<Double>> {
    return IndexingIterable { iterator() }
}

/**
 * Returns a lazy [Iterable] of [IndexedValue] for each element of the original array.
 */
public fun BooleanArray.withIndex(): Iterable<IndexedValue<Boolean>> {
    return IndexingIterable { iterator() }
}

/**
 * Returns a lazy [Iterable] of [IndexedValue] for each element of the original array.
 */
public fun CharArray.withIndex(): Iterable<IndexedValue<Char>> {
    return IndexingIterable { iterator() }
}

/**
 * Returns a list containing only distinct elements from the given array.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public fun <T> Array<out T>.distinct(): List<T> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given array.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public fun ByteArray.distinct(): List<Byte> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given array.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public fun ShortArray.distinct(): List<Short> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given array.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public fun IntArray.distinct(): List<Int> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given array.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public fun LongArray.distinct(): List<Long> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given array.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public fun FloatArray.distinct(): List<Float> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given array.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public fun DoubleArray.distinct(): List<Double> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given array.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public fun BooleanArray.distinct(): List<Boolean> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given array.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public fun CharArray.distinct(): List<Char> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only elements from the given array
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public inline fun <T, K> Array<out T>.distinctBy(selector: (T) -> K): List<T> {
    val set = HashSet<K>()
    val list = ArrayList<T>()
    for (e in this) {
        val key = selector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only elements from the given array
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public inline fun <K> ByteArray.distinctBy(selector: (Byte) -> K): List<Byte> {
    val set = HashSet<K>()
    val list = ArrayList<Byte>()
    for (e in this) {
        val key = selector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only elements from the given array
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public inline fun <K> ShortArray.distinctBy(selector: (Short) -> K): List<Short> {
    val set = HashSet<K>()
    val list = ArrayList<Short>()
    for (e in this) {
        val key = selector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only elements from the given array
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public inline fun <K> IntArray.distinctBy(selector: (Int) -> K): List<Int> {
    val set = HashSet<K>()
    val list = ArrayList<Int>()
    for (e in this) {
        val key = selector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only elements from the given array
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public inline fun <K> LongArray.distinctBy(selector: (Long) -> K): List<Long> {
    val set = HashSet<K>()
    val list = ArrayList<Long>()
    for (e in this) {
        val key = selector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only elements from the given array
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public inline fun <K> FloatArray.distinctBy(selector: (Float) -> K): List<Float> {
    val set = HashSet<K>()
    val list = ArrayList<Float>()
    for (e in this) {
        val key = selector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only elements from the given array
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public inline fun <K> DoubleArray.distinctBy(selector: (Double) -> K): List<Double> {
    val set = HashSet<K>()
    val list = ArrayList<Double>()
    for (e in this) {
        val key = selector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only elements from the given array
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public inline fun <K> BooleanArray.distinctBy(selector: (Boolean) -> K): List<Boolean> {
    val set = HashSet<K>()
    val list = ArrayList<Boolean>()
    for (e in this) {
        val key = selector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only elements from the given array
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting list are in the same order as they were in the source array.
 */
public inline fun <K> CharArray.distinctBy(selector: (Char) -> K): List<Char> {
    val set = HashSet<K>()
    val list = ArrayList<Char>()
    for (e in this) {
        val key = selector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun <T> Array<out T>.intersect(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun ByteArray.intersect(other: Iterable<Byte>): Set<Byte> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun ShortArray.intersect(other: Iterable<Short>): Set<Short> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun IntArray.intersect(other: Iterable<Int>): Set<Int> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun LongArray.intersect(other: Iterable<Long>): Set<Long> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun FloatArray.intersect(other: Iterable<Float>): Set<Float> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun DoubleArray.intersect(other: Iterable<Double>): Set<Double> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun BooleanArray.intersect(other: Iterable<Boolean>): Set<Boolean> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun CharArray.intersect(other: Iterable<Char>): Set<Char> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this array and not contained by the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun <T> Array<out T>.subtract(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this array and not contained by the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun ByteArray.subtract(other: Iterable<Byte>): Set<Byte> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this array and not contained by the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun ShortArray.subtract(other: Iterable<Short>): Set<Short> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this array and not contained by the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun IntArray.subtract(other: Iterable<Int>): Set<Int> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this array and not contained by the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun LongArray.subtract(other: Iterable<Long>): Set<Long> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this array and not contained by the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun FloatArray.subtract(other: Iterable<Float>): Set<Float> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this array and not contained by the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun DoubleArray.subtract(other: Iterable<Double>): Set<Double> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this array and not contained by the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun BooleanArray.subtract(other: Iterable<Boolean>): Set<Boolean> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this array and not contained by the specified collection.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public infix fun CharArray.subtract(other: Iterable<Char>): Set<Char> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given array.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun <T> Array<out T>.toMutableSet(): MutableSet<T> {
    val set = LinkedHashSet<T>(mapCapacity(size))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given array.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun ByteArray.toMutableSet(): MutableSet<Byte> {
    val set = LinkedHashSet<Byte>(mapCapacity(size))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given array.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun ShortArray.toMutableSet(): MutableSet<Short> {
    val set = LinkedHashSet<Short>(mapCapacity(size))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given array.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun IntArray.toMutableSet(): MutableSet<Int> {
    val set = LinkedHashSet<Int>(mapCapacity(size))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given array.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun LongArray.toMutableSet(): MutableSet<Long> {
    val set = LinkedHashSet<Long>(mapCapacity(size))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given array.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun FloatArray.toMutableSet(): MutableSet<Float> {
    val set = LinkedHashSet<Float>(mapCapacity(size))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given array.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun DoubleArray.toMutableSet(): MutableSet<Double> {
    val set = LinkedHashSet<Double>(mapCapacity(size))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given array.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun BooleanArray.toMutableSet(): MutableSet<Boolean> {
    val set = LinkedHashSet<Boolean>(mapCapacity(size))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given array.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun CharArray.toMutableSet(): MutableSet<Char> {
    val set = LinkedHashSet<Char>(mapCapacity(size))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 *
 * The returned set preserves the element iteration order of the original array.
 * Those elements of the [other] collection that are unique are iterated in the end
 * in the order of the [other] collection.
 */
public infix fun <T> Array<out T>.union(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 *
 * The returned set preserves the element iteration order of the original array.
 * Those elements of the [other] collection that are unique are iterated in the end
 * in the order of the [other] collection.
 */
public infix fun ByteArray.union(other: Iterable<Byte>): Set<Byte> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 *
 * The returned set preserves the element iteration order of the original array.
 * Those elements of the [other] collection that are unique are iterated in the end
 * in the order of the [other] collection.
 */
public infix fun ShortArray.union(other: Iterable<Short>): Set<Short> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 *
 * The returned set preserves the element iteration order of the original array.
 * Those elements of the [other] collection that are unique are iterated in the end
 * in the order of the [other] collection.
 */
public infix fun IntArray.union(other: Iterable<Int>): Set<Int> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 *
 * The returned set preserves the element iteration order of the original array.
 * Those elements of the [other] collection that are unique are iterated in the end
 * in the order of the [other] collection.
 */
public infix fun LongArray.union(other: Iterable<Long>): Set<Long> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 *
 * The returned set preserves the element iteration order of the original array.
 * Those elements of the [other] collection that are unique are iterated in the end
 * in the order of the [other] collection.
 */
public infix fun FloatArray.union(other: Iterable<Float>): Set<Float> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 *
 * The returned set preserves the element iteration order of the original array.
 * Those elements of the [other] collection that are unique are iterated in the end
 * in the order of the [other] collection.
 */
public infix fun DoubleArray.union(other: Iterable<Double>): Set<Double> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 *
 * The returned set preserves the element iteration order of the original array.
 * Those elements of the [other] collection that are unique are iterated in the end
 * in the order of the [other] collection.
 */
public infix fun BooleanArray.union(other: Iterable<Boolean>): Set<Boolean> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 *
 * The returned set preserves the element iteration order of the original array.
 * Those elements of the [other] collection that are unique are iterated in the end
 * in the order of the [other] collection.
 */
public infix fun CharArray.union(other: Iterable<Char>): Set<Char> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns an average value of elements in the array.
 */
public fun Array<out Byte>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) Double.NaN else sum / count
}

/**
 * Returns an average value of elements in the array.
 */
public fun Array<out Short>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) Double.NaN else sum / count
}

/**
 * Returns an average value of elements in the array.
 */
public fun Array<out Int>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) Double.NaN else sum / count
}

/**
 * Returns an average value of elements in the array.
 */
public fun Array<out Long>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) Double.NaN else sum / count
}

/**
 * Returns an average value of elements in the array.
 */
public fun Array<out Float>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) Double.NaN else sum / count
}

/**
 * Returns an average value of elements in the array.
 */
public fun Array<out Double>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) Double.NaN else sum / count
}

/**
 * Returns an average value of elements in the array.
 */
public fun ByteArray.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) Double.NaN else sum / count
}

/**
 * Returns an average value of elements in the array.
 */
public fun ShortArray.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) Double.NaN else sum / count
}

/**
 * Returns an average value of elements in the array.
 */
public fun IntArray.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) Double.NaN else sum / count
}

/**
 * Returns an average value of elements in the array.
 */
public fun LongArray.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) Double.NaN else sum / count
}

/**
 * Returns an average value of elements in the array.
 */
public fun FloatArray.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) Double.NaN else sum / count
}

/**
 * Returns an average value of elements in the array.
 */
public fun DoubleArray.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) Double.NaN else sum / count
}


/**
 * Returns the sum of all elements in the array.
 */
public fun Array<out Byte>.sum(): Int {
    var sum: Int = 0
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the array.
 */
public fun Array<out Short>.sum(): Int {
    var sum: Int = 0
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the array.
 */
public fun Array<out Int>.sum(): Int {
    var sum: Int = 0
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the array.
 */
public fun Array<out Long>.sum(): Long {
    var sum: Long = 0L
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the array.
 */
public fun Array<out Float>.sum(): Float {
    var sum: Float = 0.0f
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the array.
 */
public fun Array<out Double>.sum(): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the array.
 */
public fun ByteArray.sum(): Int {
    var sum: Int = 0
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the array.
 */
public fun ShortArray.sum(): Int {
    var sum: Int = 0
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the array.
 */
public fun IntArray.sum(): Int {
    var sum: Int = 0
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the array.
 */
public fun LongArray.sum(): Long {
    var sum: Long = 0L
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the array.
 */
public fun FloatArray.sum(): Float {
    var sum: Float = 0.0f
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the array.
 */
public fun DoubleArray.sum(): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += element
    }
    return sum
}

// From _Arrays.kt.
/**
 * Returns the range of valid indices for the array.
 */
public val <T> Array<out T>.indices: IntRange
    get() = IntRange(0, lastIndex)

/**
 * Returns the range of valid indices for the array.
 */
public val ByteArray.indices: IntRange
    get() = IntRange(0, lastIndex)

/**
 * Returns the range of valid indices for the array.
 */
public val ShortArray.indices: IntRange
    get() = IntRange(0, lastIndex)

/**
 * Returns the range of valid indices for the array.
 */
public val IntArray.indices: IntRange
    get() = IntRange(0, lastIndex)

/**
 * Returns the range of valid indices for the array.
 */
public val LongArray.indices: IntRange
    get() = IntRange(0, lastIndex)

/**
 * Returns the range of valid indices for the array.
 */
public val FloatArray.indices: IntRange
    get() = IntRange(0, lastIndex)

/**
 * Returns the range of valid indices for the array.
 */
public val DoubleArray.indices: IntRange
    get() = IntRange(0, lastIndex)

/**
 * Returns the range of valid indices for the array.
 */
public val BooleanArray.indices: IntRange
    get() = IntRange(0, lastIndex)

/**
 * Returns the range of valid indices for the array.
 */
public val CharArray.indices: IntRange
    get() = IntRange(0, lastIndex)

/**
 * Returns the last valid index for the array.
 */
public val <T> Array<out T>.lastIndex: Int
    get() = size - 1

/**
 * Returns the last valid index for the array.
 */
public val ByteArray.lastIndex: Int
    get() = size - 1

/**
 * Returns the last valid index for the array.
 */
public val ShortArray.lastIndex: Int
    get() = size - 1

/**
 * Returns the last valid index for the array.
 */
public val IntArray.lastIndex: Int
    get() = size - 1

/**
 * Returns the last valid index for the array.
 */
public val LongArray.lastIndex: Int
    get() = size - 1

/**
 * Returns the last valid index for the array.
 */
public val FloatArray.lastIndex: Int
    get() = size - 1

/**
 * Returns the last valid index for the array.
 */
public val DoubleArray.lastIndex: Int
    get() = size - 1

/**
 * Returns the last valid index for the array.
 */
public val BooleanArray.lastIndex: Int
    get() = size - 1

/**
 * Returns the last valid index for the array.
 */
public val CharArray.lastIndex: Int
    get() = size - 1

/**
 * Returns `true` if the array is empty.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Array<out T>.isEmpty(): Boolean {
    return size == 0
}

/**
 * Returns `true` if the array is empty.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.isEmpty(): Boolean {
    return size == 0
}

/**
 * Returns `true` if the array is empty.
 */
@kotlin.internal.InlineOnly
public inline fun ShortArray.isEmpty(): Boolean {
    return size == 0
}

/**
 * Returns `true` if the array is empty.
 */
@kotlin.internal.InlineOnly
public inline fun IntArray.isEmpty(): Boolean {
    return size == 0
}

/**
 * Returns `true` if the array is empty.
 */
@kotlin.internal.InlineOnly
public inline fun LongArray.isEmpty(): Boolean {
    return size == 0
}

/**
 * Returns `true` if the array is empty.
 */
@kotlin.internal.InlineOnly
public inline fun FloatArray.isEmpty(): Boolean {
    return size == 0
}

/**
 * Returns `true` if the array is empty.
 */
@kotlin.internal.InlineOnly
public inline fun DoubleArray.isEmpty(): Boolean {
    return size == 0
}
/**
 * Returns `true` if the array is empty.
 */
@kotlin.internal.InlineOnly
public inline fun BooleanArray.isEmpty(): Boolean {
    return size == 0
}

/**
 * Returns `true` if the array is empty.
 */
@kotlin.internal.InlineOnly
public inline fun CharArray.isEmpty(): Boolean {
    return size == 0
}

/**
 * Returns `true` if the array is not empty.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Array<out T>.isNotEmpty(): Boolean {
    return !isEmpty()
}

/**
 * Returns `true` if the array is not empty.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.isNotEmpty(): Boolean {
    return !isEmpty()
}

/**
 * Returns `true` if the array is not empty.
 */
@kotlin.internal.InlineOnly
public inline fun ShortArray.isNotEmpty(): Boolean {
    return !isEmpty()
}

/**
 * Returns `true` if the array is not empty.
 */
@kotlin.internal.InlineOnly
public inline fun IntArray.isNotEmpty(): Boolean {
    return !isEmpty()
}

/**
 * Returns `true` if the array is not empty.
 */
@kotlin.internal.InlineOnly
public inline fun LongArray.isNotEmpty(): Boolean {
    return !isEmpty()
}

/**
 * Returns `true` if the array is not empty.
 */
@kotlin.internal.InlineOnly
public inline fun FloatArray.isNotEmpty(): Boolean {
    return !isEmpty()
}

/**
 * Returns `true` if the array is not empty.
 */
@kotlin.internal.InlineOnly
public inline fun DoubleArray.isNotEmpty(): Boolean {
    return !isEmpty()
}

/**
 * Returns `true` if the array is not empty.
 */
@kotlin.internal.InlineOnly
public inline fun BooleanArray.isNotEmpty(): Boolean {
    return !isEmpty()
}

/**
 * Returns `true` if the array is not empty.
 */
@kotlin.internal.InlineOnly
public inline fun CharArray.isNotEmpty(): Boolean {
    return !isEmpty()
}

/**
 * Returns last index of [element], or -1 if the array does not contain element.
 */
public fun <@kotlin.internal.OnlyInputTypes T> Array<out T>.lastIndexOf(element: T): Int {
    if (element == null) {
        for (index in indices.reversed()) {
            if (this[index] == null) {
                return index
            }
        }
    } else {
        for (index in indices.reversed()) {
            if (element == this[index]) {
                return index
            }
        }
    }
    return -1
}

/**
 * Returns last index of [element], or -1 if the array does not contain element.
 */
public fun ByteArray.lastIndexOf(element: Byte): Int {
    for (index in indices.reversed()) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns last index of [element], or -1 if the array does not contain element.
 */
public fun ShortArray.lastIndexOf(element: Short): Int {
    for (index in indices.reversed()) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns last index of [element], or -1 if the array does not contain element.
 */
public fun IntArray.lastIndexOf(element: Int): Int {
    for (index in indices.reversed()) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns last index of [element], or -1 if the array does not contain element.
 */
public fun LongArray.lastIndexOf(element: Long): Int {
    for (index in indices.reversed()) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns last index of [element], or -1 if the array does not contain element.
 */
public fun FloatArray.lastIndexOf(element: Float): Int {
    for (index in indices.reversed()) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns last index of [element], or -1 if the array does not contain element.
 */
public fun DoubleArray.lastIndexOf(element: Double): Int {
    for (index in indices.reversed()) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns last index of [element], or -1 if the array does not contain element.
 */
public fun BooleanArray.lastIndexOf(element: Boolean): Int {
    for (index in indices.reversed()) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Returns last index of [element], or -1 if the array does not contain element.
 */
public fun CharArray.lastIndexOf(element: Char): Int {
    for (index in indices.reversed()) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

/**
 * Appends all elements to the given [destination] collection.
 */
public fun <T, C : MutableCollection<in T>> Array<out T>.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

/**
 * Appends all elements to the given [destination] collection.
 */
public fun <C : MutableCollection<in Byte>> ByteArray.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

/**
 * Appends all elements to the given [destination] collection.
 */
public fun <C : MutableCollection<in Short>> ShortArray.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

/**
 * Appends all elements to the given [destination] collection.
 */
public fun <C : MutableCollection<in Int>> IntArray.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

/**
 * Appends all elements to the given [destination] collection.
 */
public fun <C : MutableCollection<in Long>> LongArray.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

/**
 * Appends all elements to the given [destination] collection.
 */
public fun <C : MutableCollection<in Float>> FloatArray.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

/**
 * Appends all elements to the given [destination] collection.
 */
public fun <C : MutableCollection<in Double>> DoubleArray.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

/**
 * Appends all elements to the given [destination] collection.
 */
public fun <C : MutableCollection<in Boolean>> BooleanArray.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

/**
 * Appends all elements to the given [destination] collection.
 */
public fun <C : MutableCollection<in Char>> CharArray.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

/**
 * Returns a [List] containing all elements.
 */
public fun <T> Array<out T>.toList(): List<T> {
    return when (size) {
        0 -> emptyList()
        1 -> listOf(this[0])
        else -> this.toMutableList()
    }
}

/**
 * Returns a [List] containing all elements.
 */
public fun ByteArray.toList(): List<Byte> {
    return when (size) {
        0 -> emptyList()
        1 -> listOf(this[0])
        else -> this.toMutableList()
    }
}

/**
 * Returns a [List] containing all elements.
 */
public fun ShortArray.toList(): List<Short> {
    return when (size) {
        0 -> emptyList()
        1 -> listOf(this[0])
        else -> this.toMutableList()
    }
}

/**
 * Returns a [List] containing all elements.
 */
public fun IntArray.toList(): List<Int> {
    return when (size) {
        0 -> emptyList()
        1 -> listOf(this[0])
        else -> this.toMutableList()
    }
}

/**
 * Returns a [List] containing all elements.
 */
public fun LongArray.toList(): List<Long> {
    return when (size) {
        0 -> emptyList()
        1 -> listOf(this[0])
        else -> this.toMutableList()
    }
}

/**
 * Returns a [List] containing all elements.
 */
public fun FloatArray.toList(): List<Float> {
    return when (size) {
        0 -> emptyList()
        1 -> listOf(this[0])
        else -> this.toMutableList()
    }
}

/**
 * Returns a [List] containing all elements.
 */
public fun DoubleArray.toList(): List<Double> {
    return when (size) {
        0 -> emptyList()
        1 -> listOf(this[0])
        else -> this.toMutableList()
    }
}

/**
 * Returns a [List] containing all elements.
 */
public fun BooleanArray.toList(): List<Boolean> {
    return when (size) {
        0 -> emptyList()
        1 -> listOf(this[0])
        else -> this.toMutableList()
    }
}

/**
 * Returns a [List] containing all elements.
 */
public fun CharArray.toList(): List<Char> {
    return when (size) {
        0 -> emptyList()
        1 -> listOf(this[0])
        else -> this.toMutableList()
    }
}


/**
 * Returns a [MutableList] filled with all elements of this array.
 */
public fun <T> Array<out T>.toMutableList(): MutableList<T> {
    return ArrayList(this.asCollection())
}


/**
 * Returns a [MutableList] filled with all elements of this array.
 */
public fun ByteArray.toMutableList(): MutableList<Byte> {
    val list = ArrayList<Byte>(size)
    for (item in this) list.add(item)
    return list
}

/**
 * Returns a [MutableList] filled with all elements of this array.
 */
public fun ShortArray.toMutableList(): MutableList<Short> {
    val list = ArrayList<Short>(size)
    for (item in this) list.add(item)
    return list
}

/**
 * Returns a [MutableList] filled with all elements of this array.
 */
public fun IntArray.toMutableList(): MutableList<Int> {
    val list = ArrayList<Int>(size)
    for (item in this) list.add(item)
    return list
}

/**
 * Returns a [MutableList] filled with all elements of this array.
 */
public fun LongArray.toMutableList(): MutableList<Long> {
    val list = ArrayList<Long>(size)
    for (item in this) list.add(item)
    return list
}

/**
 * Returns a [MutableList] filled with all elements of this array.
 */
public fun FloatArray.toMutableList(): MutableList<Float> {
    val list = ArrayList<Float>(size)
    for (item in this) list.add(item)
    return list
}

/**
 * Returns a [MutableList] filled with all elements of this array.
 */
public fun DoubleArray.toMutableList(): MutableList<Double> {
    val list = ArrayList<Double>(size)
    for (item in this) list.add(item)
    return list
}

/**
 * Returns a [MutableList] filled with all elements of this array.
 */
public fun BooleanArray.toMutableList(): MutableList<Boolean> {
    val list = ArrayList<Boolean>(size)
    for (item in this) list.add(item)
    return list
}

/**
 * Returns a [MutableList] filled with all elements of this array.
 */
public fun CharArray.toMutableList(): MutableList<Char> {
    val list = ArrayList<Char>(size)
    for (item in this) list.add(item)
    return list
}

/**
 * Returns a [HashSet] of all elements.
 */
public fun <T> Array<out T>.toHashSet(): HashSet<T> {
    return toCollection(HashSet<T>(mapCapacity(size)))
}

/**
 * Returns a [HashSet] of all elements.
 */
public fun ByteArray.toHashSet(): HashSet<Byte> {
    return toCollection(HashSet<Byte>(mapCapacity(size)))
}

/**
 * Returns a [HashSet] of all elements.
 */
public fun ShortArray.toHashSet(): HashSet<Short> {
    return toCollection(HashSet<Short>(mapCapacity(size)))
}

/**
 * Returns a [HashSet] of all elements.
 */
public fun IntArray.toHashSet(): HashSet<Int> {
    return toCollection(HashSet<Int>(mapCapacity(size)))
}

/**
 * Returns a [HashSet] of all elements.
 */
public fun LongArray.toHashSet(): HashSet<Long> {
    return toCollection(HashSet<Long>(mapCapacity(size)))
}

/**
 * Returns a [HashSet] of all elements.
 */
public fun FloatArray.toHashSet(): HashSet<Float> {
    return toCollection(HashSet<Float>(mapCapacity(size)))
}

/**
 * Returns a [HashSet] of all elements.
 */
public fun DoubleArray.toHashSet(): HashSet<Double> {
    return toCollection(HashSet<Double>(mapCapacity(size)))
}

/**
 * Returns a [HashSet] of all elements.
 */
public fun BooleanArray.toHashSet(): HashSet<Boolean> {
    return toCollection(HashSet<Boolean>(mapCapacity(size)))
}

/**
 * Returns a [HashSet] of all elements.
 */
public fun CharArray.toHashSet(): HashSet<Char> {
    return toCollection(HashSet<Char>(mapCapacity(size)))
}

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun <T> Array<out T>.toSet(): Set<T> {
    return when (size) {
        0 -> emptySet()
        1 -> setOf(this[0])
        else -> toCollection(LinkedHashSet<T>(mapCapacity(size)))
    }
}

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun ByteArray.toSet(): Set<Byte> {
    return when (size) {
        0 -> emptySet()
        1 -> setOf(this[0])
        else -> toCollection(LinkedHashSet<Byte>(mapCapacity(size)))
    }
}

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun ShortArray.toSet(): Set<Short> {
    return when (size) {
        0 -> emptySet()
        1 -> setOf(this[0])
        else -> toCollection(LinkedHashSet<Short>(mapCapacity(size)))
    }
}

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun IntArray.toSet(): Set<Int> {
    return when (size) {
        0 -> emptySet()
        1 -> setOf(this[0])
        else -> toCollection(LinkedHashSet<Int>(mapCapacity(size)))
    }
}

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun LongArray.toSet(): Set<Long> {
    return when (size) {
        0 -> emptySet()
        1 -> setOf(this[0])
        else -> toCollection(LinkedHashSet<Long>(mapCapacity(size)))
    }
}

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun FloatArray.toSet(): Set<Float> {
    return when (size) {
        0 -> emptySet()
        1 -> setOf(this[0])
        else -> toCollection(LinkedHashSet<Float>(mapCapacity(size)))
    }
}

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun DoubleArray.toSet(): Set<Double> {
    return when (size) {
        0 -> emptySet()
        1 -> setOf(this[0])
        else -> toCollection(LinkedHashSet<Double>(mapCapacity(size)))
    }
}

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun BooleanArray.toSet(): Set<Boolean> {
    return when (size) {
        0 -> emptySet()
        1 -> setOf(this[0])
        else -> toCollection(LinkedHashSet<Boolean>(mapCapacity(size)))
    }
}

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original array.
 */
public fun CharArray.toSet(): Set<Char> {
    return when (size) {
        0 -> emptySet()
        1 -> setOf(this[0])
        else -> toCollection(LinkedHashSet<Char>(mapCapacity(size)))
    }
}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked on each element of original array.
 */
public inline fun <T, R> Array<out T>.flatMap(transform: (T) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked on each element of original array.
 */
public inline fun <R> ByteArray.flatMap(transform: (Byte) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked on each element of original array.
 */
public inline fun <R> ShortArray.flatMap(transform: (Short) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked on each element of original array.
 */
public inline fun <R> IntArray.flatMap(transform: (Int) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked on each element of original array.
 */
public inline fun <R> LongArray.flatMap(transform: (Long) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked on each element of original array.
 */
public inline fun <R> FloatArray.flatMap(transform: (Float) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked on each element of original array.
 */
public inline fun <R> DoubleArray.flatMap(transform: (Double) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked on each element of original array.
 */
public inline fun <R> BooleanArray.flatMap(transform: (Boolean) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked on each element of original array.
 */
public inline fun <R> CharArray.flatMap(transform: (Char) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Appends all elements yielded from results of [transform] function being invoked on each element of original array, to the given [destination].
 */
public inline fun <T, R, C : MutableCollection<in R>> Array<out T>.flatMapTo(destination: C, transform: (T) -> Iterable<R>): C {
    for (element in this) {
        val list = transform(element)
        destination.addAll(list)
    }
    return destination
}

/**
 * Appends all elements yielded from results of [transform] function being invoked on each element of original array, to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> ByteArray.flatMapTo(destination: C, transform: (Byte) -> Iterable<R>): C {
    for (element in this) {
        val list = transform(element)
        destination.addAll(list)
    }
    return destination
}

/**
 * Appends all elements yielded from results of [transform] function being invoked on each element of original array, to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> ShortArray.flatMapTo(destination: C, transform: (Short) -> Iterable<R>): C {
    for (element in this) {
        val list = transform(element)
        destination.addAll(list)
    }
    return destination
}

/**
 * Appends all elements yielded from results of [transform] function being invoked on each element of original array, to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> IntArray.flatMapTo(destination: C, transform: (Int) -> Iterable<R>): C {
    for (element in this) {
        val list = transform(element)
        destination.addAll(list)
    }
    return destination
}

/**
 * Appends all elements yielded from results of [transform] function being invoked on each element of original array, to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> LongArray.flatMapTo(destination: C, transform: (Long) -> Iterable<R>): C {
    for (element in this) {
        val list = transform(element)
        destination.addAll(list)
    }
    return destination
}

/**
 * Appends all elements yielded from results of [transform] function being invoked on each element of original array, to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> FloatArray.flatMapTo(destination: C, transform: (Float) -> Iterable<R>): C {
    for (element in this) {
        val list = transform(element)
        destination.addAll(list)
    }
    return destination
}

/**
 * Appends all elements yielded from results of [transform] function being invoked on each element of original array, to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> DoubleArray.flatMapTo(destination: C, transform: (Double) -> Iterable<R>): C {
    for (element in this) {
        val list = transform(element)
        destination.addAll(list)
    }
    return destination
}

/**
 * Appends all elements yielded from results of [transform] function being invoked on each element of original array, to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> BooleanArray.flatMapTo(destination: C, transform: (Boolean) -> Iterable<R>): C {
    for (element in this) {
        val list = transform(element)
        destination.addAll(list)
    }
    return destination
}

/**
 * Appends all elements yielded from results of [transform] function being invoked on each element of original array, to the given [destination].
 */
public inline fun <R, C : MutableCollection<in R>> CharArray.flatMapTo(destination: C, transform: (Char) -> Iterable<R>): C {
    for (element in this) {
        val list = transform(element)
        destination.addAll(list)
    }
    return destination
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <T, K> Array<out T>.groupBy(keySelector: (T) -> K): Map<K, List<T>> {
    return groupByTo(LinkedHashMap<K, MutableList<T>>(), keySelector)
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K> ByteArray.groupBy(keySelector: (Byte) -> K): Map<K, List<Byte>> {
    return groupByTo(LinkedHashMap<K, MutableList<Byte>>(), keySelector)
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K> ShortArray.groupBy(keySelector: (Short) -> K): Map<K, List<Short>> {
    return groupByTo(LinkedHashMap<K, MutableList<Short>>(), keySelector)
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K> IntArray.groupBy(keySelector: (Int) -> K): Map<K, List<Int>> {
    return groupByTo(LinkedHashMap<K, MutableList<Int>>(), keySelector)
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K> LongArray.groupBy(keySelector: (Long) -> K): Map<K, List<Long>> {
    return groupByTo(LinkedHashMap<K, MutableList<Long>>(), keySelector)
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K> FloatArray.groupBy(keySelector: (Float) -> K): Map<K, List<Float>> {
    return groupByTo(LinkedHashMap<K, MutableList<Float>>(), keySelector)
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K> DoubleArray.groupBy(keySelector: (Double) -> K): Map<K, List<Double>> {
    return groupByTo(LinkedHashMap<K, MutableList<Double>>(), keySelector)
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K> BooleanArray.groupBy(keySelector: (Boolean) -> K): Map<K, List<Boolean>> {
    return groupByTo(LinkedHashMap<K, MutableList<Boolean>>(), keySelector)
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K> CharArray.groupBy(keySelector: (Char) -> K): Map<K, List<Char>> {
    return groupByTo(LinkedHashMap<K, MutableList<Char>>(), keySelector)
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and returns a map where each group key is associated with a list of corresponding values.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <T, K, V> Array<out T>.groupBy(keySelector: (T) -> K, valueTransform: (T) -> V): Map<K, List<V>> {
    return groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and returns a map where each group key is associated with a list of corresponding values.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V> ByteArray.groupBy(keySelector: (Byte) -> K, valueTransform: (Byte) -> V): Map<K, List<V>> {
    return groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and returns a map where each group key is associated with a list of corresponding values.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V> ShortArray.groupBy(keySelector: (Short) -> K, valueTransform: (Short) -> V): Map<K, List<V>> {
    return groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and returns a map where each group key is associated with a list of corresponding values.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V> IntArray.groupBy(keySelector: (Int) -> K, valueTransform: (Int) -> V): Map<K, List<V>> {
    return groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and returns a map where each group key is associated with a list of corresponding values.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V> LongArray.groupBy(keySelector: (Long) -> K, valueTransform: (Long) -> V): Map<K, List<V>> {
    return groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and returns a map where each group key is associated with a list of corresponding values.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V> FloatArray.groupBy(keySelector: (Float) -> K, valueTransform: (Float) -> V): Map<K, List<V>> {
    return groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and returns a map where each group key is associated with a list of corresponding values.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V> DoubleArray.groupBy(keySelector: (Double) -> K, valueTransform: (Double) -> V): Map<K, List<V>> {
    return groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and returns a map where each group key is associated with a list of corresponding values.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V> BooleanArray.groupBy(keySelector: (Boolean) -> K, valueTransform: (Boolean) -> V): Map<K, List<V>> {
    return groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and returns a map where each group key is associated with a list of corresponding values.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original array.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V> CharArray.groupBy(keySelector: (Char) -> K, valueTransform: (Char) -> V): Map<K, List<V>> {
    return groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and puts to the [destination] map each group key associated with a list of corresponding elements.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <T, K, M : MutableMap<in K, MutableList<T>>> Array<out T>.groupByTo(destination: M, keySelector: (T) -> K): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<T>() }
        list.add(element)
    }
    return destination
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and puts to the [destination] map each group key associated with a list of corresponding elements.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K, M : MutableMap<in K, MutableList<Byte>>> ByteArray.groupByTo(destination: M, keySelector: (Byte) -> K): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<Byte>() }
        list.add(element)
    }
    return destination
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and puts to the [destination] map each group key associated with a list of corresponding elements.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K, M : MutableMap<in K, MutableList<Short>>> ShortArray.groupByTo(destination: M, keySelector: (Short) -> K): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<Short>() }
        list.add(element)
    }
    return destination
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and puts to the [destination] map each group key associated with a list of corresponding elements.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K, M : MutableMap<in K, MutableList<Int>>> IntArray.groupByTo(destination: M, keySelector: (Int) -> K): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<Int>() }
        list.add(element)
    }
    return destination
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and puts to the [destination] map each group key associated with a list of corresponding elements.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K, M : MutableMap<in K, MutableList<Long>>> LongArray.groupByTo(destination: M, keySelector: (Long) -> K): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<Long>() }
        list.add(element)
    }
    return destination
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and puts to the [destination] map each group key associated with a list of corresponding elements.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K, M : MutableMap<in K, MutableList<Float>>> FloatArray.groupByTo(destination: M, keySelector: (Float) -> K): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<Float>() }
        list.add(element)
    }
    return destination
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and puts to the [destination] map each group key associated with a list of corresponding elements.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K, M : MutableMap<in K, MutableList<Double>>> DoubleArray.groupByTo(destination: M, keySelector: (Double) -> K): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<Double>() }
        list.add(element)
    }
    return destination
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and puts to the [destination] map each group key associated with a list of corresponding elements.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K, M : MutableMap<in K, MutableList<Boolean>>> BooleanArray.groupByTo(destination: M, keySelector: (Boolean) -> K): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<Boolean>() }
        list.add(element)
    }
    return destination
}

/**
 * Groups elements of the original array by the key returned by the given [keySelector] function
 * applied to each element and puts to the [destination] map each group key associated with a list of corresponding elements.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <K, M : MutableMap<in K, MutableList<Char>>> CharArray.groupByTo(destination: M, keySelector: (Char) -> K): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<Char>() }
        list.add(element)
    }
    return destination
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and puts to the [destination] map each group key associated with a list of corresponding values.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <T, K, V, M : MutableMap<in K, MutableList<V>>> Array<out T>.groupByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<V>() }
        list.add(valueTransform(element))
    }
    return destination
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and puts to the [destination] map each group key associated with a list of corresponding values.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V, M : MutableMap<in K, MutableList<V>>> ByteArray.groupByTo(destination: M, keySelector: (Byte) -> K, valueTransform: (Byte) -> V): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<V>() }
        list.add(valueTransform(element))
    }
    return destination
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and puts to the [destination] map each group key associated with a list of corresponding values.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V, M : MutableMap<in K, MutableList<V>>> ShortArray.groupByTo(destination: M, keySelector: (Short) -> K, valueTransform: (Short) -> V): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<V>() }
        list.add(valueTransform(element))
    }
    return destination
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and puts to the [destination] map each group key associated with a list of corresponding values.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V, M : MutableMap<in K, MutableList<V>>> IntArray.groupByTo(destination: M, keySelector: (Int) -> K, valueTransform: (Int) -> V): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<V>() }
        list.add(valueTransform(element))
    }
    return destination
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and puts to the [destination] map each group key associated with a list of corresponding values.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V, M : MutableMap<in K, MutableList<V>>> LongArray.groupByTo(destination: M, keySelector: (Long) -> K, valueTransform: (Long) -> V): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<V>() }
        list.add(valueTransform(element))
    }
    return destination
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and puts to the [destination] map each group key associated with a list of corresponding values.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V, M : MutableMap<in K, MutableList<V>>> FloatArray.groupByTo(destination: M, keySelector: (Float) -> K, valueTransform: (Float) -> V): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<V>() }
        list.add(valueTransform(element))
    }
    return destination
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and puts to the [destination] map each group key associated with a list of corresponding values.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V, M : MutableMap<in K, MutableList<V>>> DoubleArray.groupByTo(destination: M, keySelector: (Double) -> K, valueTransform: (Double) -> V): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<V>() }
        list.add(valueTransform(element))
    }
    return destination
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and puts to the [destination] map each group key associated with a list of corresponding values.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V, M : MutableMap<in K, MutableList<V>>> BooleanArray.groupByTo(destination: M, keySelector: (Boolean) -> K, valueTransform: (Boolean) -> V): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<V>() }
        list.add(valueTransform(element))
    }
    return destination
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original array
 * by the key returned by the given [keySelector] function applied to the element
 * and puts to the [destination] map each group key associated with a list of corresponding values.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <K, V, M : MutableMap<in K, MutableList<V>>> CharArray.groupByTo(destination: M, keySelector: (Char) -> K, valueTransform: (Char) -> V): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<V>() }
        list.add(valueTransform(element))
    }
    return destination
}

/**
 * Creates a [Grouping] source from an array to be used later with one of group-and-fold operations
 * using the specified [keySelector] function to extract a key from each element.
 *
 * @sample samples.collections.Collections.Transformations.groupingByEachCount
 */
@SinceKotlin("1.1")
public inline fun <T, K> Array<out T>.groupingBy(crossinline keySelector: (T) -> K): Grouping<T, K> {
    return object : Grouping<T, K> {
        override fun sourceIterator(): Iterator<T> = this@groupingBy.iterator()
        override fun keyOf(element: T): K = keySelector(element)
    }
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Array<T>.copyOf(): Array<T> {
    return this.copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.copyOf(): ByteArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public inline fun ShortArray.copyOf(): ShortArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public inline fun IntArray.copyOf(): IntArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public inline fun LongArray.copyOf(): LongArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public inline fun FloatArray.copyOf(): FloatArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public inline fun DoubleArray.copyOf(): DoubleArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public inline fun BooleanArray.copyOf(): BooleanArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public inline fun CharArray.copyOf(): CharArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public inline fun <T> Array<T>.copyOf(newSize: Int): Array<T?> {
    return copyOfNulls(newSize) as Array<T?>
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.copyOf(newSize: Int): ByteArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public inline fun ShortArray.copyOf(newSize: Int): ShortArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public inline fun IntArray.copyOf(newSize: Int): IntArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public inline fun LongArray.copyOf(newSize: Int): LongArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public inline fun FloatArray.copyOf(newSize: Int): FloatArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public inline fun DoubleArray.copyOf(newSize: Int): DoubleArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public inline fun BooleanArray.copyOf(newSize: Int): BooleanArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public inline fun CharArray.copyOf(newSize: Int): CharArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of range of original array.
 */
// TODO: The method may check input or return Array<T?>.
// Now we check its input (fromIndex <= toIndex < size).
// Sync its behaviour wiht Kotlin JVM when the problem is solved there.
@kotlin.internal.InlineOnly
public inline fun <T> Array<T>.copyOfRange(fromIndex: Int, toIndex: Int): Array<T> {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.copyOfRange(fromIndex: Int, toIndex: Int): ByteArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public inline fun ShortArray.copyOfRange(fromIndex: Int, toIndex: Int): ShortArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public inline fun IntArray.copyOfRange(fromIndex: Int, toIndex: Int): IntArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public inline fun LongArray.copyOfRange(fromIndex: Int, toIndex: Int): LongArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public inline fun FloatArray.copyOfRange(fromIndex: Int, toIndex: Int): FloatArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public inline fun DoubleArray.copyOfRange(fromIndex: Int, toIndex: Int): DoubleArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public inline fun BooleanArray.copyOfRange(fromIndex: Int, toIndex: Int): BooleanArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public inline fun CharArray.copyOfRange(fromIndex: Int, toIndex: Int): CharArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}


/**
 * Sorts the array in-place according to the order specified by the given [comparator].
 */
public fun <T> Array<out T>.sortWith(comparator: Comparator<in T>): Unit {
    if (size > 1) sortArrayWith(this, 0, size, comparator)
}

/**
 * Sorts elements in the array in-place according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> Array<out T>.sortBy(crossinline selector: (T) -> R?): Unit {
    if (size > 1) sortWith(compareBy(selector))
}

/**
 * Sorts elements in the array in-place descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> Array<out T>.sortByDescending(crossinline selector: (T) -> R?): Unit {
    if (size > 1) sortWith(compareByDescending(selector))
}

/**
 * Sorts elements in the array in-place descending according to their natural sort order.
 */
public fun <T : Comparable<T>> Array<out T>.sortDescending(): Unit {
    sortWith(reverseOrder())
}

/**
 * Sorts elements in the array in-place descending according to their natural sort order.
 */
public fun ByteArray.sortDescending(): Unit {
    if (size > 1) {
        sort()
        reverse()
    }
}

/**
 * Sorts elements in the array in-place descending according to their natural sort order.
 */
public fun ShortArray.sortDescending(): Unit {
    if (size > 1) {
        sort()
        reverse()
    }
}

/**
 * Sorts elements in the array in-place descending according to their natural sort order.
 */
public fun IntArray.sortDescending(): Unit {
    if (size > 1) {
        sort()
        reverse()
    }
}

/**
 * Sorts elements in the array in-place descending according to their natural sort order.
 */
public fun LongArray.sortDescending(): Unit {
    if (size > 1) {
        sort()
        reverse()
    }
}

/**
 * Sorts elements in the array in-place descending according to their natural sort order.
 */
public fun FloatArray.sortDescending(): Unit {
    if (size > 1) {
        sort()
        reverse()
    }
}

/**
 * Sorts elements in the array in-place descending according to their natural sort order.
 */
public fun DoubleArray.sortDescending(): Unit {
    if (size > 1) {
        sort()
        reverse()
    }
}

/**
 * Sorts elements in the array in-place descending according to their natural sort order.
 */
public fun CharArray.sortDescending(): Unit {
    if (size > 1) {
        sort()
        reverse()
    }
}

/**
 * Returns a list of all elements sorted according to their natural sort order.
 */
public fun <T : Comparable<T>> Array<out T>.sorted(): List<T> {
    return sortedArray().asList()
}

/**
 * Returns a list of all elements sorted according to their natural sort order.
 */
public fun ByteArray.sorted(): List<Byte> {
    return toTypedArray().apply { sort() }.asList()
}

/**
 * Returns a list of all elements sorted according to their natural sort order.
 */
public fun ShortArray.sorted(): List<Short> {
    return toTypedArray().apply { sort() }.asList()
}

/**
 * Returns a list of all elements sorted according to their natural sort order.
 */
public fun IntArray.sorted(): List<Int> {
    return toTypedArray().apply { sort() }.asList()
}

/**
 * Returns a list of all elements sorted according to their natural sort order.
 */
public fun LongArray.sorted(): List<Long> {
    return toTypedArray().apply { sort() }.asList()
}

/**
 * Returns a list of all elements sorted according to their natural sort order.
 */
public fun FloatArray.sorted(): List<Float> {
    return toTypedArray().apply { sort() }.asList()
}

/**
 * Returns a list of all elements sorted according to their natural sort order.
 */
public fun DoubleArray.sorted(): List<Double> {
    return toTypedArray().apply { sort() }.asList()
}

/**
 * Returns a list of all elements sorted according to their natural sort order.
 */
public fun CharArray.sorted(): List<Char> {
    return toTypedArray().apply { sort() }.asList()
}

/**
 * Returns an array with all elements of this array sorted according to their natural sort order.
 */
public fun <T : Comparable<T>> Array<T>.sortedArray(): Array<T> {
    if (isEmpty()) return this
    return this.copyOf().apply { sort() }
}

/**
 * Returns an array with all elements of this array sorted according to their natural sort order.
 */
public fun ByteArray.sortedArray(): ByteArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sort() }
}

/**
 * Returns an array with all elements of this array sorted according to their natural sort order.
 */
public fun ShortArray.sortedArray(): ShortArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sort() }
}

/**
 * Returns an array with all elements of this array sorted according to their natural sort order.
 */
public fun IntArray.sortedArray(): IntArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sort() }
}

/**
 * Returns an array with all elements of this array sorted according to their natural sort order.
 */
public fun LongArray.sortedArray(): LongArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sort() }
}

/**
 * Returns an array with all elements of this array sorted according to their natural sort order.
 */
public fun FloatArray.sortedArray(): FloatArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sort() }
}

/**
 * Returns an array with all elements of this array sorted according to their natural sort order.
 */
public fun DoubleArray.sortedArray(): DoubleArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sort() }
}

/**
 * Returns an array with all elements of this array sorted according to their natural sort order.
 */
public fun CharArray.sortedArray(): CharArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sort() }
}

/**
 * Returns an array with all elements of this array sorted descending according to their natural sort order.
 */
public fun <T : Comparable<T>> Array<T>.sortedArrayDescending(): Array<T> {
    if (isEmpty()) return this
    return this.copyOf().apply { sortWith(reverseOrder()) }
}

/**
 * Returns an array with all elements of this array sorted descending according to their natural sort order.
 */
public fun ByteArray.sortedArrayDescending(): ByteArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sortDescending() }
}

/**
 * Returns an array with all elements of this array sorted descending according to their natural sort order.
 */
public fun ShortArray.sortedArrayDescending(): ShortArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sortDescending() }
}

/**
 * Returns an array with all elements of this array sorted descending according to their natural sort order.
 */
public fun IntArray.sortedArrayDescending(): IntArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sortDescending() }
}

/**
 * Returns an array with all elements of this array sorted descending according to their natural sort order.
 */
public fun LongArray.sortedArrayDescending(): LongArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sortDescending() }
}

/**
 * Returns an array with all elements of this array sorted descending according to their natural sort order.
 */
public fun FloatArray.sortedArrayDescending(): FloatArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sortDescending() }
}

/**
 * Returns an array with all elements of this array sorted descending according to their natural sort order.
 */
public fun DoubleArray.sortedArrayDescending(): DoubleArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sortDescending() }
}

/**
 * Returns an array with all elements of this array sorted descending according to their natural sort order.
 */
public fun CharArray.sortedArrayDescending(): CharArray {
    if (isEmpty()) return this
    return this.copyOf().apply { sortDescending() }
}

/**
 * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> Array<out T>.sortedBy(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> ByteArray.sortedBy(crossinline selector: (Byte) -> R?): List<Byte> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> ShortArray.sortedBy(crossinline selector: (Short) -> R?): List<Short> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> IntArray.sortedBy(crossinline selector: (Int) -> R?): List<Int> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> LongArray.sortedBy(crossinline selector: (Long) -> R?): List<Long> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> FloatArray.sortedBy(crossinline selector: (Float) -> R?): List<Float> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> DoubleArray.sortedBy(crossinline selector: (Double) -> R?): List<Double> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> BooleanArray.sortedBy(crossinline selector: (Boolean) -> R?): List<Boolean> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> CharArray.sortedBy(crossinline selector: (Char) -> R?): List<Char> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> Array<out T>.sortedByDescending(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> ByteArray.sortedByDescending(crossinline selector: (Byte) -> R?): List<Byte> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> ShortArray.sortedByDescending(crossinline selector: (Short) -> R?): List<Short> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> IntArray.sortedByDescending(crossinline selector: (Int) -> R?): List<Int> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> LongArray.sortedByDescending(crossinline selector: (Long) -> R?): List<Long> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> FloatArray.sortedByDescending(crossinline selector: (Float) -> R?): List<Float> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> DoubleArray.sortedByDescending(crossinline selector: (Double) -> R?): List<Double> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> BooleanArray.sortedByDescending(crossinline selector: (Boolean) -> R?): List<Boolean> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <R : Comparable<R>> CharArray.sortedByDescending(crossinline selector: (Char) -> R?): List<Char> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a list of all elements sorted descending according to their natural sort order.
 */
public fun <T : Comparable<T>> Array<out T>.sortedDescending(): List<T> {
    return sortedWith(reverseOrder())
}

/**
 * Returns a list of all elements sorted descending according to their natural sort order.
 */
public fun ByteArray.sortedDescending(): List<Byte> {
    return copyOf().apply { sort() }.reversed()
}

/**
 * Returns a list of all elements sorted descending according to their natural sort order.
 */
public fun ShortArray.sortedDescending(): List<Short> {
    return copyOf().apply { sort() }.reversed()
}

/**
 * Returns a list of all elements sorted descending according to their natural sort order.
 */
public fun IntArray.sortedDescending(): List<Int> {
    return copyOf().apply { sort() }.reversed()
}

/**
 * Returns a list of all elements sorted descending according to their natural sort order.
 */
public fun LongArray.sortedDescending(): List<Long> {
    return copyOf().apply { sort() }.reversed()
}

/**
 * Returns a list of all elements sorted descending according to their natural sort order.
 */
public fun FloatArray.sortedDescending(): List<Float> {
    return copyOf().apply { sort() }.reversed()
}

/**
 * Returns a list of all elements sorted descending according to their natural sort order.
 */
public fun DoubleArray.sortedDescending(): List<Double> {
    return copyOf().apply { sort() }.reversed()
}

/**
 * Returns a list of all elements sorted descending according to their natural sort order.
 */
public fun CharArray.sortedDescending(): List<Char> {
    return copyOf().apply { sort() }.reversed()
}

/**
 * Sorts the array in-place according to the natural order of its elements.
 *
 * @throws ClassCastException if any element of the array is not [Comparable].
 */
public fun <T> Array<out T>.sort(): Unit {
    if (size > 1) sortArrayComparable(this)
}

public fun <T> Array<out T>.sortWith(comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size): Unit {
    sortArrayWith(this, fromIndex, toIndex, comparator)
}

/**
 * Sorts the array in-place.
 */
public fun IntArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Sorts the array in-place.
 */
public fun LongArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Sorts the array in-place.
 */
public fun ByteArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Sorts the array in-place.
 */
public fun ShortArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Sorts the array in-place.
 */
public fun DoubleArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Sorts the array in-place.
 */
public fun FloatArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Sorts the array in-place.
 */
public fun CharArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public fun ByteArray.toTypedArray(): Array<Byte> {
    val result = arrayOfNulls<Byte>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Byte>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public fun ShortArray.toTypedArray(): Array<Short> {
    val result = arrayOfNulls<Short>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Short>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public fun IntArray.toTypedArray(): Array<Int> {
    val result = arrayOfNulls<Int>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Int>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public fun LongArray.toTypedArray(): Array<Long> {
    val result = arrayOfNulls<Long>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Long>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public fun FloatArray.toTypedArray(): Array<Float> {
    val result = arrayOfNulls<Float>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Float>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public fun DoubleArray.toTypedArray(): Array<Double> {
    val result = arrayOfNulls<Double>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Double>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public fun BooleanArray.toTypedArray(): Array<Boolean> {
    val result = arrayOfNulls<Boolean>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Boolean>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public fun CharArray.toTypedArray(): Array<Char> {
    val result = arrayOfNulls<Char>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Char>
}

/**
 * Returns a list of all elements sorted according to the specified [comparator].
 */
public fun <T> Array<out T>.sortedWith(comparator: Comparator<in T>): List<T> {
    return sortedArrayWith(comparator).asList()
}

/**
 * Returns a list of all elements sorted according to the specified [comparator].
 */
public fun ByteArray.sortedWith(comparator: Comparator<in Byte>): List<Byte> {
    return toTypedArray().apply { sortWith(comparator) }.asList()
}

/**
 * Returns a list of all elements sorted according to the specified [comparator].
 */
public fun ShortArray.sortedWith(comparator: Comparator<in Short>): List<Short> {
    return toTypedArray().apply { sortWith(comparator) }.asList()
}

/**
 * Returns a list of all elements sorted according to the specified [comparator].
 */
public fun IntArray.sortedWith(comparator: Comparator<in Int>): List<Int> {
    return toTypedArray().apply { sortWith(comparator) }.asList()
}

/**
 * Returns a list of all elements sorted according to the specified [comparator].
 */
public fun LongArray.sortedWith(comparator: Comparator<in Long>): List<Long> {
    return toTypedArray().apply { sortWith(comparator) }.asList()
}

/**
 * Returns a list of all elements sorted according to the specified [comparator].
 */
public fun FloatArray.sortedWith(comparator: Comparator<in Float>): List<Float> {
    return toTypedArray().apply { sortWith(comparator) }.asList()
}

/**
 * Returns a list of all elements sorted according to the specified [comparator].
 */
public fun DoubleArray.sortedWith(comparator: Comparator<in Double>): List<Double> {
    return toTypedArray().apply { sortWith(comparator) }.asList()
}

/**
 * Returns a list of all elements sorted according to the specified [comparator].
 */
public fun BooleanArray.sortedWith(comparator: Comparator<in Boolean>): List<Boolean> {
    return toTypedArray().apply { sortWith(comparator) }.asList()
}

/**
 * Returns a list of all elements sorted according to the specified [comparator].
 */
public fun CharArray.sortedWith(comparator: Comparator<in Char>): List<Char> {
    return toTypedArray().apply { sortWith(comparator) }.asList()
}

/**
 * Returns an array with all elements of this array sorted according the specified [comparator].
 */
public fun <T> Array<out T>.sortedArrayWith(comparator: Comparator<in T>): Array<out T> {
    if (isEmpty()) return this
    return this.copyOf().apply { sortWith(comparator) }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> Array<out T>.contentToString(): String {
    return this.subarrayContentToString(offset = 0, length = this.size)
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public fun ByteArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public fun ShortArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public fun IntArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public fun LongArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public fun FloatArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public fun DoubleArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public fun BooleanArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public fun CharArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}


/**
 * Returns a string representation of the contents of the subarray of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> Array<out T>.subarrayContentToString(offset: Int, length: Int): String {
    val sb = StringBuilder(2 + length * 3)
    sb.append("[")
    var i = 0
    while (i < length) {
        if (i > 0) sb.append(", ")
        sb.append(this[offset + i])
        i++
    }
    sb.append("]")
    return sb.toString()
}

/**
 * Returns `true` if the two specified arrays are *deeply* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 *
 * If two corresponding elements are nested arrays, they are also compared deeply.
 * If any of arrays contains itself on any nesting level the behavior is undefined.
 */
@SinceKotlin("1.1")
public infix fun <T> Array<out T>.contentDeepEquals(other: Array<out T>): Boolean {
    if (this === other)
        return true
    if (other == null)
        return false

    if (size != other.size)
        return false

    for (i in indices) {
        val e1 = this[i]
        val e2 = other[i]

        if (e1 === e2)
            continue
        if (e1 == null)
            return false

        // Figure out whether the two elements are equal
        val eq = when {
            e1 is Array<*> && e2 is Array<*>         -> e1 contentDeepEquals e2
            e1 is ByteArray && e2 is ByteArray       -> e1 contentEquals e2
            e1 is ShortArray && e2 is ShortArray     -> e1 contentEquals e2
            e1 is IntArray && e2 is IntArray         -> e1 contentEquals e2
            e1 is LongArray && e2 is LongArray       -> e1 contentEquals e2
            e1 is CharArray && e2 is CharArray       -> e1 contentEquals e2
            e1 is FloatArray && e2 is FloatArray     -> e1 contentEquals e2
            e1 is DoubleArray && e2 is DoubleArray   -> e1 contentEquals e2
            e1 is BooleanArray && e2 is BooleanArray -> e1 contentEquals e2
            else -> e1 == e2
        }
        if (!eq)
            return false

    }
    return true
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 * Nested arrays are treated as lists too.
 *
 * If any of arrays contains itself on any nesting level the behavior is undefined.
 */
@SinceKotlin("1.1")
public fun <T> Array<out T>.contentDeepHashCode(): Int {
    var result = 1
    for (element in this) {
        var elementHash = 0
        if (element is Array<*>)
            elementHash = element.contentDeepHashCode()
        else
            element?.let { elementHash = element.hashCode() }

        result = 31 * result + elementHash
    }
    return result
}

/**
 * Returns a string representation of the contents of this array as if it is a [List].
 * Nested arrays are treated as lists too.
 *
 * If any of arrays contains itself on any nesting level that reference
 * is rendered as `"[...]"` to prevent recursion.
 */
@SinceKotlin("1.1")
public fun <T> Array<out T>.contentDeepToString(): String {
    var bufLen = 20 * size
    if (size != 0 && bufLen <= 0)
        bufLen = Int.MAX_VALUE
    val buf = StringBuilder(bufLen)
    contentDeepToString(buf, mutableSetOf())
    return buf.toString()
}

/**
 * Internal recursive function to build a string for [contentDeepToString].
 */
private fun <T> Array<out T>.contentDeepToString(buf: StringBuilder, dejaVu: MutableSet<Array<Any?>>) {
    if (size == 0) {
        buf.append("[]")
        return
    }

    val iMax = size - 1
    @Suppress("UNCHECKED_CAST")
    dejaVu.add(this as Array<Any?>) // TODO: Is the cast correct?
    buf.append('[')
    var i = 0
    while (true) {
        val element = this[i]
        when {
            element is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                if (dejaVu.contains(element as Array<Any?>)) {
                    buf.append("[...]")
                } else {
                    element.contentDeepToString(buf, dejaVu)
                }
            }
            element is ByteArray    -> buf.append(element.contentToString())
            element is ShortArray   -> buf.append(element.contentToString())
            element is IntArray     -> buf.append(element.contentToString())
            element is LongArray    -> buf.append(element.contentToString())
            element is CharArray    -> buf.append(element.contentToString())
            element is FloatArray   -> buf.append(element.contentToString())
            element is DoubleArray  -> buf.append(element.contentToString())
            element is BooleanArray -> buf.append(element.contentToString())
            else -> buf.append(element.toString())
        }
        if (i == iMax)
            break
        buf.append(", ")
        i++
    }
    buf.append(']')
    dejaVu.remove(this)
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public infix fun <T> Array<out T>.contentEquals(other: Array<out T>): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true;
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public infix fun ByteArray.contentEquals(other: ByteArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true;
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public infix fun ShortArray.contentEquals(other: ShortArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true;
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public infix fun IntArray.contentEquals(other: IntArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true;
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public infix fun LongArray.contentEquals(other: LongArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true;
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public infix fun FloatArray.contentEquals(other: FloatArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true;
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public infix fun DoubleArray.contentEquals(other: DoubleArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true;
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public infix fun BooleanArray.contentEquals(other: BooleanArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true;
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public infix fun CharArray.contentEquals(other: CharArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true;
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public fun <T> Array<out T>.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + (element?.hashCode() ?: 0)
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public fun ByteArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + (element?.hashCode() ?: 0)
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public fun ShortArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + (element?.hashCode() ?: 0)
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public fun IntArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + (element?.hashCode() ?: 0)
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public fun LongArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + (element?.hashCode() ?: 0)
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public fun FloatArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + (element?.hashCode() ?: 0)
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public fun DoubleArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + (element?.hashCode() ?: 0)
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public fun BooleanArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + (element?.hashCode() ?: 0)
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public fun CharArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + (element?.hashCode() ?: 0)
    return result
}

/**
 * Returns an array of Boolean containing all of the elements of this generic array.
 */
public fun Array<out Boolean>.toBooleanArray(): BooleanArray {
    val result = BooleanArray(size)
    for (index in indices)
        result[index] = this[index]
    return result
}

/**
 * Returns an array of Byte containing all of the elements of this generic array.
 */
public fun Array<out Byte>.toByteArray(): ByteArray {
    val result = ByteArray(size)
    for (index in indices)
        result[index] = this[index]
    return result
}

/**
 * Returns an array of Char containing all of the elements of this generic array.
 */
public fun Array<out Char>.toCharArray(): CharArray {
    val result = CharArray(size)
    for (index in indices)
        result[index] = this[index]
    return result
}

/**
 * Returns an array of Double containing all of the elements of this generic array.
 */
public fun Array<out Double>.toDoubleArray(): DoubleArray {
    val result = DoubleArray(size)
    for (index in indices)
        result[index] = this[index]
    return result
}

/**
 * Returns an array of Float containing all of the elements of this generic array.
 */
public fun Array<out Float>.toFloatArray(): FloatArray {
    val result = FloatArray(size)
    for (index in indices)
        result[index] = this[index]
    return result
}

/**
 * Returns an array of Int containing all of the elements of this generic array.
 */
public fun Array<out Int>.toIntArray(): IntArray {
    val result = IntArray(size)
    for (index in indices)
        result[index] = this[index]
    return result
}

/**
 * Returns an array of Long containing all of the elements of this generic array.
 */
public fun Array<out Long>.toLongArray(): LongArray {
    val result = LongArray(size)
    for (index in indices)
        result[index] = this[index]
    return result
}

/**
 * Returns an array of Short containing all of the elements of this generic array.
 */
public fun Array<out Short>.toShortArray(): ShortArray {
    val result = ShortArray(size)
    for (index in indices)
        result[index] = this[index]
    return result
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <T, K, V> Array<out T>.associate(transform: (T) -> Pair<K, V>): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateTo(LinkedHashMap<K, V>(capacity), transform)
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> ByteArray.associate(transform: (Byte) -> Pair<K, V>): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateTo(LinkedHashMap<K, V>(capacity), transform)
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> ShortArray.associate(transform: (Short) -> Pair<K, V>): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateTo(LinkedHashMap<K, V>(capacity), transform)
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> IntArray.associate(transform: (Int) -> Pair<K, V>): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateTo(LinkedHashMap<K, V>(capacity), transform)
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> LongArray.associate(transform: (Long) -> Pair<K, V>): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateTo(LinkedHashMap<K, V>(capacity), transform)
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> FloatArray.associate(transform: (Float) -> Pair<K, V>): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateTo(LinkedHashMap<K, V>(capacity), transform)
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> DoubleArray.associate(transform: (Double) -> Pair<K, V>): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateTo(LinkedHashMap<K, V>(capacity), transform)
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> BooleanArray.associate(transform: (Boolean) -> Pair<K, V>): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateTo(LinkedHashMap<K, V>(capacity), transform)
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> CharArray.associate(transform: (Char) -> Pair<K, V>): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateTo(LinkedHashMap<K, V>(capacity), transform)
}

/**
 * Returns a [Map] containing the elements from the given array indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <T, K> Array<out T>.associateBy(keySelector: (T) -> K): Map<K, T> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, T>(capacity), keySelector)
}

/**
 * Returns a [Map] containing the elements from the given array indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K> ByteArray.associateBy(keySelector: (Byte) -> K): Map<K, Byte> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, Byte>(capacity), keySelector)
}

/**
 * Returns a [Map] containing the elements from the given array indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K> ShortArray.associateBy(keySelector: (Short) -> K): Map<K, Short> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, Short>(capacity), keySelector)
}

/**
 * Returns a [Map] containing the elements from the given array indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K> IntArray.associateBy(keySelector: (Int) -> K): Map<K, Int> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, Int>(capacity), keySelector)
}

/**
 * Returns a [Map] containing the elements from the given array indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K> LongArray.associateBy(keySelector: (Long) -> K): Map<K, Long> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, Long>(capacity), keySelector)
}

/**
 * Returns a [Map] containing the elements from the given array indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K> FloatArray.associateBy(keySelector: (Float) -> K): Map<K, Float> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, Float>(capacity), keySelector)
}

/**
 * Returns a [Map] containing the elements from the given array indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K> DoubleArray.associateBy(keySelector: (Double) -> K): Map<K, Double> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, Double>(capacity), keySelector)
}

/**
 * Returns a [Map] containing the elements from the given array indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K> BooleanArray.associateBy(keySelector: (Boolean) -> K): Map<K, Boolean> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, Boolean>(capacity), keySelector)
}

/**
 * Returns a [Map] containing the elements from the given array indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K> CharArray.associateBy(keySelector: (Char) -> K): Map<K, Char> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, Char>(capacity), keySelector)
}

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <T, K, V> Array<out T>.associateBy(keySelector: (T) -> K, valueTransform: (T) -> V): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
}

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> ByteArray.associateBy(keySelector: (Byte) -> K, valueTransform: (Byte) -> V): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
}

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> ShortArray.associateBy(keySelector: (Short) -> K, valueTransform: (Short) -> V): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
}

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> IntArray.associateBy(keySelector: (Int) -> K, valueTransform: (Int) -> V): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
}

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> LongArray.associateBy(keySelector: (Long) -> K, valueTransform: (Long) -> V): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
}

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> FloatArray.associateBy(keySelector: (Float) -> K, valueTransform: (Float) -> V): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
}

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> DoubleArray.associateBy(keySelector: (Double) -> K, valueTransform: (Double) -> V): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
}

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> BooleanArray.associateBy(keySelector: (Boolean) -> K, valueTransform: (Boolean) -> V): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
}

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public inline fun <K, V> CharArray.associateBy(keySelector: (Char) -> K, valueTransform: (Char) -> V): Map<K, V> {
    val capacity = mapCapacity(size).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function applied to each element of the given array
 * and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <T, K, M : MutableMap<in K, in T>> Array<out T>.associateByTo(destination: M, keySelector: (T) -> K): M {
    for (element in this) {
        destination.put(keySelector(element), element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function applied to each element of the given array
 * and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, M : MutableMap<in K, in Byte>> ByteArray.associateByTo(destination: M, keySelector: (Byte) -> K): M {
    for (element in this) {
        destination.put(keySelector(element), element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function applied to each element of the given array
 * and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, M : MutableMap<in K, in Short>> ShortArray.associateByTo(destination: M, keySelector: (Short) -> K): M {
    for (element in this) {
        destination.put(keySelector(element), element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function applied to each element of the given array
 * and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, M : MutableMap<in K, in Int>> IntArray.associateByTo(destination: M, keySelector: (Int) -> K): M {
    for (element in this) {
        destination.put(keySelector(element), element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function applied to each element of the given array
 * and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, M : MutableMap<in K, in Long>> LongArray.associateByTo(destination: M, keySelector: (Long) -> K): M {
    for (element in this) {
        destination.put(keySelector(element), element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function applied to each element of the given array
 * and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, M : MutableMap<in K, in Float>> FloatArray.associateByTo(destination: M, keySelector: (Float) -> K): M {
    for (element in this) {
        destination.put(keySelector(element), element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function applied to each element of the given array
 * and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, M : MutableMap<in K, in Double>> DoubleArray.associateByTo(destination: M, keySelector: (Double) -> K): M {
    for (element in this) {
        destination.put(keySelector(element), element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function applied to each element of the given array
 * and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, M : MutableMap<in K, in Boolean>> BooleanArray.associateByTo(destination: M, keySelector: (Boolean) -> K): M {
    for (element in this) {
        destination.put(keySelector(element), element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function applied to each element of the given array
 * and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, M : MutableMap<in K, in Char>> CharArray.associateByTo(destination: M, keySelector: (Char) -> K): M {
    for (element in this) {
        destination.put(keySelector(element), element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function and
 * and value is provided by the [valueTransform] function applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <T, K, V, M : MutableMap<in K, in V>> Array<out T>.associateByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M {
    for (element in this) {
        destination.put(keySelector(element), valueTransform(element))
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function and
 * and value is provided by the [valueTransform] function applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> ByteArray.associateByTo(destination: M, keySelector: (Byte) -> K, valueTransform: (Byte) -> V): M {
    for (element in this) {
        destination.put(keySelector(element), valueTransform(element))
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function and
 * and value is provided by the [valueTransform] function applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> ShortArray.associateByTo(destination: M, keySelector: (Short) -> K, valueTransform: (Short) -> V): M {
    for (element in this) {
        destination.put(keySelector(element), valueTransform(element))
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function and
 * and value is provided by the [valueTransform] function applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> IntArray.associateByTo(destination: M, keySelector: (Int) -> K, valueTransform: (Int) -> V): M {
    for (element in this) {
        destination.put(keySelector(element), valueTransform(element))
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function and
 * and value is provided by the [valueTransform] function applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> LongArray.associateByTo(destination: M, keySelector: (Long) -> K, valueTransform: (Long) -> V): M {
    for (element in this) {
        destination.put(keySelector(element), valueTransform(element))
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function and
 * and value is provided by the [valueTransform] function applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> FloatArray.associateByTo(destination: M, keySelector: (Float) -> K, valueTransform: (Float) -> V): M {
    for (element in this) {
        destination.put(keySelector(element), valueTransform(element))
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function and
 * and value is provided by the [valueTransform] function applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> DoubleArray.associateByTo(destination: M, keySelector: (Double) -> K, valueTransform: (Double) -> V): M {
    for (element in this) {
        destination.put(keySelector(element), valueTransform(element))
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function and
 * and value is provided by the [valueTransform] function applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> BooleanArray.associateByTo(destination: M, keySelector: (Boolean) -> K, valueTransform: (Boolean) -> V): M {
    for (element in this) {
        destination.put(keySelector(element), valueTransform(element))
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function and
 * and value is provided by the [valueTransform] function applied to elements of the given array.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> CharArray.associateByTo(destination: M, keySelector: (Char) -> K, valueTransform: (Char) -> V): M {
    for (element in this) {
        destination.put(keySelector(element), valueTransform(element))
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs
 * provided by [transform] function applied to each element of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <T, K, V, M : MutableMap<in K, in V>> Array<out T>.associateTo(destination: M, transform: (T) -> Pair<K, V>): M {
    for (element in this) {
        destination += transform(element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs
 * provided by [transform] function applied to each element of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> ByteArray.associateTo(destination: M, transform: (Byte) -> Pair<K, V>): M {
    for (element in this) {
        destination += transform(element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs
 * provided by [transform] function applied to each element of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> ShortArray.associateTo(destination: M, transform: (Short) -> Pair<K, V>): M {
    for (element in this) {
        destination += transform(element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs
 * provided by [transform] function applied to each element of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> IntArray.associateTo(destination: M, transform: (Int) -> Pair<K, V>): M {
    for (element in this) {
        destination += transform(element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs
 * provided by [transform] function applied to each element of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> LongArray.associateTo(destination: M, transform: (Long) -> Pair<K, V>): M {
    for (element in this) {
        destination += transform(element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs
 * provided by [transform] function applied to each element of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> FloatArray.associateTo(destination: M, transform: (Float) -> Pair<K, V>): M {
    for (element in this) {
        destination += transform(element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs
 * provided by [transform] function applied to each element of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> DoubleArray.associateTo(destination: M, transform: (Double) -> Pair<K, V>): M {
    for (element in this) {
        destination += transform(element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs
 * provided by [transform] function applied to each element of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> BooleanArray.associateTo(destination: M, transform: (Boolean) -> Pair<K, V>): M {
    for (element in this) {
        destination += transform(element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs
 * provided by [transform] function applied to each element of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <K, V, M : MutableMap<in K, in V>> CharArray.associateTo(destination: M, transform: (Char) -> Pair<K, V>): M {
    for (element in this) {
        destination += transform(element)
    }
    return destination
}

// From Library.kt.
/**
 * Returns an array of objects of the given type with the given [size], initialized with null values.
 */
public inline fun <reified @PureReifiable T> arrayOfNulls(size: Int): Array<T?> =
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        arrayOfUninitializedElements<T?>(size)

/**
 * Returns a *typed* array containing all of the elements of this collection.
 *
 * Allocates an array of runtime type `T` having its size equal to the size of this collection
 * and populates the array with the elements of this collection.
 */
public inline fun <reified T> Collection<T>.toTypedArray(): Array<T> {
    val result = arrayOfNulls<T>(size)
    var index = 0
    for (element in this) result[index++] = element
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}

/**
 * Returns an array containing the specified elements.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified @PureReifiable T> arrayOf(vararg elements: T): Array<T> = elements as Array<T>

private val kEmptyArray = arrayOf<Any>()

@Suppress("UNCHECKED_CAST")
public fun <T> emptyArray() = kEmptyArray as Array<T>

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

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <T, A : Appendable> Array<out T>.joinTo(
        buffer: A, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "",
        limit: Int = -1, truncated: CharSequence = "...", transform: ((T) -> CharSequence)? = null): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            buffer.appendElement(element, transform)
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <A : Appendable> ByteArray.joinTo(buffer: A, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Byte) -> CharSequence)? = null): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            if (transform != null)
                buffer.append(transform(element))
            else
                buffer.append(element.toString())
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <A : Appendable> ShortArray.joinTo(buffer: A, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Short) -> CharSequence)? = null): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            if (transform != null)
                buffer.append(transform(element))
            else
                buffer.append(element.toString())
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <A : Appendable> IntArray.joinTo(buffer: A, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Int) -> CharSequence)? = null): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            if (transform != null)
                buffer.append(transform(element))
            else
                buffer.append(element.toString())
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <A : Appendable> LongArray.joinTo(buffer: A, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Long) -> CharSequence)? = null): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            if (transform != null)
                buffer.append(transform(element))
            else
                buffer.append(element.toString())
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <A : Appendable> FloatArray.joinTo(buffer: A, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Float) -> CharSequence)? = null): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            if (transform != null)
                buffer.append(transform(element))
            else
                buffer.append(element.toString())
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <A : Appendable> DoubleArray.joinTo(buffer: A, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Double) -> CharSequence)? = null): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            if (transform != null)
                buffer.append(transform(element))
            else
                buffer.append(element.toString())
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <A : Appendable> BooleanArray.joinTo(buffer: A, separator: CharSequence = ", ", prefix: CharSequence = "",
                                                postfix: CharSequence = "", limit: Int = -1,
                                                truncated: CharSequence = "...",
                                                transform: ((Boolean) -> CharSequence)? = null): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            if (transform != null)
                buffer.append(transform(element))
            else
                buffer.append(element.toString())
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <A : Appendable> CharArray.joinTo(buffer: A, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "",
                                             limit: Int = -1, truncated: CharSequence = "...", transform: ((Char) -> CharSequence)? = null): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            if (transform != null)
                buffer.append(transform(element))
            else
                buffer.append(element)
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <T> Array<out T>.joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((T) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun ByteArray.joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Byte) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun ShortArray.joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Short) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun IntArray.joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Int) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun LongArray.joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Long) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun FloatArray.joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Float) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun DoubleArray.joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Double) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun BooleanArray.joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Boolean) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun CharArray.joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Char) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Creates an [Iterable] instance that wraps the original array returning its elements when being iterated.
 */
public fun <T> Array<out T>.asIterable(): Iterable<T> {
    if (isEmpty()) return emptyList()
    return Iterable { this.iterator() }
}

/**
 * Creates an [Iterable] instance that wraps the original array returning its elements when being iterated.
 */
public fun ByteArray.asIterable(): Iterable<Byte> {
    if (isEmpty()) return emptyList()
    return Iterable { this.iterator() }
}

/**
 * Creates an [Iterable] instance that wraps the original array returning its elements when being iterated.
 */
public fun ShortArray.asIterable(): Iterable<Short> {
    if (isEmpty()) return emptyList()
    return Iterable { this.iterator() }
}

/**
 * Creates an [Iterable] instance that wraps the original array returning its elements when being iterated.
 */
public fun IntArray.asIterable(): Iterable<Int> {
    if (isEmpty()) return emptyList()
    return Iterable { this.iterator() }
}

/**
 * Creates an [Iterable] instance that wraps the original array returning its elements when being iterated.
 */
public fun LongArray.asIterable(): Iterable<Long> {
    if (isEmpty()) return emptyList()
    return Iterable { this.iterator() }
}

/**
 * Creates an [Iterable] instance that wraps the original array returning its elements when being iterated.
 */
public fun FloatArray.asIterable(): Iterable<Float> {
    if (isEmpty()) return emptyList()
    return Iterable { this.iterator() }
}

/**
 * Creates an [Iterable] instance that wraps the original array returning its elements when being iterated.
 */
public fun DoubleArray.asIterable(): Iterable<Double> {
    if (isEmpty()) return emptyList()
    return Iterable { this.iterator() }
}

/**
 * Creates an [Iterable] instance that wraps the original array returning its elements when being iterated.
 */
public fun BooleanArray.asIterable(): Iterable<Boolean> {
    if (isEmpty()) return emptyList()
    return Iterable { this.iterator() }
}

/**
 * Creates an [Iterable] instance that wraps the original array returning its elements when being iterated.
 */
public fun CharArray.asIterable(): Iterable<Char> {
    if (isEmpty()) return emptyList()
    return Iterable { this.iterator() }
}

/**
 * Creates a [Sequence] instance that wraps the original array returning its elements when being iterated.
 *
 * @sample samples.collections.Sequences.Building.sequenceFromArray
 */
public fun <T> Array<out T>.asSequence(): Sequence<T> {
    if (isEmpty()) return emptySequence()
    return Sequence { this.iterator() }
}

/**
 * Creates a [Sequence] instance that wraps the original array returning its elements when being iterated.
 *
 * @sample samples.collections.Sequences.Building.sequenceFromArray
 */
public fun ByteArray.asSequence(): Sequence<Byte> {
    if (isEmpty()) return emptySequence()
    return Sequence { this.iterator() }
}

/**
 * Creates a [Sequence] instance that wraps the original array returning its elements when being iterated.
 *
 * @sample samples.collections.Sequences.Building.sequenceFromArray
 */
public fun ShortArray.asSequence(): Sequence<Short> {
    if (isEmpty()) return emptySequence()
    return Sequence { this.iterator() }
}

/**
 * Creates a [Sequence] instance that wraps the original array returning its elements when being iterated.
 *
 * @sample samples.collections.Sequences.Building.sequenceFromArray
 */
public fun IntArray.asSequence(): Sequence<Int> {
    if (isEmpty()) return emptySequence()
    return Sequence { this.iterator() }
}

/**
 * Creates a [Sequence] instance that wraps the original array returning its elements when being iterated.
 *
 * @sample samples.collections.Sequences.Building.sequenceFromArray
 */
public fun LongArray.asSequence(): Sequence<Long> {
    if (isEmpty()) return emptySequence()
    return Sequence { this.iterator() }
}

/**
 * Creates a [Sequence] instance that wraps the original array returning its elements when being iterated.
 *
 * @sample samples.collections.Sequences.Building.sequenceFromArray
 */
public fun FloatArray.asSequence(): Sequence<Float> {
    if (isEmpty()) return emptySequence()
    return Sequence { this.iterator() }
}

/**
 * Creates a [Sequence] instance that wraps the original array returning its elements when being iterated.
 *
 * @sample samples.collections.Sequences.Building.sequenceFromArray
 */
public fun DoubleArray.asSequence(): Sequence<Double> {
    if (isEmpty()) return emptySequence()
    return Sequence { this.iterator() }
}

/**
 * Creates a [Sequence] instance that wraps the original array returning its elements when being iterated.
 *
 * @sample samples.collections.Sequences.Building.sequenceFromArray
 */
public fun BooleanArray.asSequence(): Sequence<Boolean> {
    if (isEmpty()) return emptySequence()
    return Sequence { this.iterator() }
}

/**
 * Creates a [Sequence] instance that wraps the original array returning its elements when being iterated.
 *
 * @sample samples.collections.Sequences.Building.sequenceFromArray
 */
public fun CharArray.asSequence(): Sequence<Char> {
    if (isEmpty()) return emptySequence()
    return Sequence { this.iterator() }
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun <T> Array<out T>.sumBy(selector: (T) -> Int): Int {
    var sum: Int = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun ByteArray.sumBy(selector: (Byte) -> Int): Int {
    var sum: Int = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun ShortArray.sumBy(selector: (Short) -> Int): Int {
    var sum: Int = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun IntArray.sumBy(selector: (Int) -> Int): Int {
    var sum: Int = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun LongArray.sumBy(selector: (Long) -> Int): Int {
    var sum: Int = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun FloatArray.sumBy(selector: (Float) -> Int): Int {
    var sum: Int = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun DoubleArray.sumBy(selector: (Double) -> Int): Int {
    var sum: Int = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun BooleanArray.sumBy(selector: (Boolean) -> Int): Int {
    var sum: Int = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun CharArray.sumBy(selector: (Char) -> Int): Int {
    var sum: Int = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun <T> Array<out T>.sumByDouble(selector: (T) -> Double): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun ByteArray.sumByDouble(selector: (Byte) -> Double): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun ShortArray.sumByDouble(selector: (Short) -> Double): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun IntArray.sumByDouble(selector: (Int) -> Double): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun LongArray.sumByDouble(selector: (Long) -> Double): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun FloatArray.sumByDouble(selector: (Float) -> Double): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun DoubleArray.sumByDouble(selector: (Double) -> Double): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun BooleanArray.sumByDouble(selector: (Boolean) -> Double): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array.
 */
public inline fun CharArray.sumByDouble(selector: (Char) -> Double): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns an original collection containing all the non-`null` elements, throwing an [IllegalArgumentException] if there are any `null` elements.
 */
public fun <T : Any> Array<T?>.requireNoNulls(): Array<T> {
    for (element in this) {
        if (element == null) {
            throw IllegalArgumentException("null element found in $this.")
        }
    }
    @Suppress("UNCHECKED_CAST")
    return this as Array<T>
}

/**
 * Splits the original array into pair of lists,
 * where *first* list contains elements for which [predicate] yielded `true`,
 * while *second* list contains elements for which [predicate] yielded `false`.
 */
public inline fun <T> Array<out T>.partition(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val first = ArrayList<T>()
    val second = ArrayList<T>()
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first, second)
}

/**
 * Splits the original array into pair of lists,
 * where *first* list contains elements for which [predicate] yielded `true`,
 * while *second* list contains elements for which [predicate] yielded `false`.
 */
public inline fun ByteArray.partition(predicate: (Byte) -> Boolean): Pair<List<Byte>, List<Byte>> {
    val first = ArrayList<Byte>()
    val second = ArrayList<Byte>()
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first, second)
}

/**
 * Splits the original array into pair of lists,
 * where *first* list contains elements for which [predicate] yielded `true`,
 * while *second* list contains elements for which [predicate] yielded `false`.
 */
public inline fun ShortArray.partition(predicate: (Short) -> Boolean): Pair<List<Short>, List<Short>> {
    val first = ArrayList<Short>()
    val second = ArrayList<Short>()
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first, second)
}

/**
 * Splits the original array into pair of lists,
 * where *first* list contains elements for which [predicate] yielded `true`,
 * while *second* list contains elements for which [predicate] yielded `false`.
 */
public inline fun IntArray.partition(predicate: (Int) -> Boolean): Pair<List<Int>, List<Int>> {
    val first = ArrayList<Int>()
    val second = ArrayList<Int>()
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first, second)
}

/**
 * Splits the original array into pair of lists,
 * where *first* list contains elements for which [predicate] yielded `true`,
 * while *second* list contains elements for which [predicate] yielded `false`.
 */
public inline fun LongArray.partition(predicate: (Long) -> Boolean): Pair<List<Long>, List<Long>> {
    val first = ArrayList<Long>()
    val second = ArrayList<Long>()
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first, second)
}

/**
 * Splits the original array into pair of lists,
 * where *first* list contains elements for which [predicate] yielded `true`,
 * while *second* list contains elements for which [predicate] yielded `false`.
 */
public inline fun FloatArray.partition(predicate: (Float) -> Boolean): Pair<List<Float>, List<Float>> {
    val first = ArrayList<Float>()
    val second = ArrayList<Float>()
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first, second)
}

/**
 * Splits the original array into pair of lists,
 * where *first* list contains elements for which [predicate] yielded `true`,
 * while *second* list contains elements for which [predicate] yielded `false`.
 */
public inline fun DoubleArray.partition(predicate: (Double) -> Boolean): Pair<List<Double>, List<Double>> {
    val first = ArrayList<Double>()
    val second = ArrayList<Double>()
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first, second)
}

/**
 * Splits the original array into pair of lists,
 * where *first* list contains elements for which [predicate] yielded `true`,
 * while *second* list contains elements for which [predicate] yielded `false`.
 */
public inline fun BooleanArray.partition(predicate: (Boolean) -> Boolean): Pair<List<Boolean>, List<Boolean>> {
    val first = ArrayList<Boolean>()
    val second = ArrayList<Boolean>()
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first, second)
}

/**
 * Splits the original array into pair of lists,
 * where *first* list contains elements for which [predicate] yielded `true`,
 * while *second* list contains elements for which [predicate] yielded `false`.
 */
public inline fun CharArray.partition(predicate: (Char) -> Boolean): Pair<List<Char>, List<Char>> {
    val first = ArrayList<Char>()
    val second = ArrayList<Char>()
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first, second)
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <T, R> Array<out T>.zip(other: Array<out R>): List<Pair<T, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> ByteArray.zip(other: Array<out R>): List<Pair<Byte, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> ShortArray.zip(other: Array<out R>): List<Pair<Short, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> IntArray.zip(other: Array<out R>): List<Pair<Int, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> LongArray.zip(other: Array<out R>): List<Pair<Long, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> FloatArray.zip(other: Array<out R>): List<Pair<Float, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> DoubleArray.zip(other: Array<out R>): List<Pair<Double, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> BooleanArray.zip(other: Array<out R>): List<Pair<Boolean, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> CharArray.zip(other: Array<out R>): List<Pair<Char, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <T, R, V> Array<out T>.zip(other: Array<out R>, transform: (a: T, b: R) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> ByteArray.zip(other: Array<out R>, transform: (a: Byte, b: R) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> ShortArray.zip(other: Array<out R>, transform: (a: Short, b: R) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> IntArray.zip(other: Array<out R>, transform: (a: Int, b: R) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> LongArray.zip(other: Array<out R>, transform: (a: Long, b: R) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> FloatArray.zip(other: Array<out R>, transform: (a: Float, b: R) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> DoubleArray.zip(other: Array<out R>, transform: (a: Double, b: R) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> BooleanArray.zip(other: Array<out R>, transform: (a: Boolean, b: R) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> CharArray.zip(other: Array<out R>, transform: (a: Char, b: R) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <T, R> Array<out T>.zip(other: Iterable<R>): List<Pair<T, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> ByteArray.zip(other: Iterable<R>): List<Pair<Byte, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> ShortArray.zip(other: Iterable<R>): List<Pair<Short, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> IntArray.zip(other: Iterable<R>): List<Pair<Int, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> LongArray.zip(other: Iterable<R>): List<Pair<Long, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> FloatArray.zip(other: Iterable<R>): List<Pair<Float, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> DoubleArray.zip(other: Iterable<R>): List<Pair<Double, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> BooleanArray.zip(other: Iterable<R>): List<Pair<Boolean, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <R> CharArray.zip(other: Iterable<R>): List<Pair<Char, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <T, R, V> Array<out T>.zip(other: Iterable<R>, transform: (a: T, b: R) -> V): List<V> {
    val arraySize = size
    val list = ArrayList<V>(minOf(other.collectionSizeOrDefault(10), arraySize))
    var i = 0
    for (element in other) {
        if (i >= arraySize) break
        list.add(transform(this[i++], element))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> ByteArray.zip(other: Iterable<R>, transform: (a: Byte, b: R) -> V): List<V> {
    val arraySize = size
    val list = ArrayList<V>(minOf(other.collectionSizeOrDefault(10), arraySize))
    var i = 0
    for (element in other) {
        if (i >= arraySize) break
        list.add(transform(this[i++], element))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> ShortArray.zip(other: Iterable<R>, transform: (a: Short, b: R) -> V): List<V> {
    val arraySize = size
    val list = ArrayList<V>(minOf(other.collectionSizeOrDefault(10), arraySize))
    var i = 0
    for (element in other) {
        if (i >= arraySize) break
        list.add(transform(this[i++], element))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> IntArray.zip(other: Iterable<R>, transform: (a: Int, b: R) -> V): List<V> {
    val arraySize = size
    val list = ArrayList<V>(minOf(other.collectionSizeOrDefault(10), arraySize))
    var i = 0
    for (element in other) {
        if (i >= arraySize) break
        list.add(transform(this[i++], element))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> LongArray.zip(other: Iterable<R>, transform: (a: Long, b: R) -> V): List<V> {
    val arraySize = size
    val list = ArrayList<V>(minOf(other.collectionSizeOrDefault(10), arraySize))
    var i = 0
    for (element in other) {
        if (i >= arraySize) break
        list.add(transform(this[i++], element))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> FloatArray.zip(other: Iterable<R>, transform: (a: Float, b: R) -> V): List<V> {
    val arraySize = size
    val list = ArrayList<V>(minOf(other.collectionSizeOrDefault(10), arraySize))
    var i = 0
    for (element in other) {
        if (i >= arraySize) break
        list.add(transform(this[i++], element))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> DoubleArray.zip(other: Iterable<R>, transform: (a: Double, b: R) -> V): List<V> {
    val arraySize = size
    val list = ArrayList<V>(minOf(other.collectionSizeOrDefault(10), arraySize))
    var i = 0
    for (element in other) {
        if (i >= arraySize) break
        list.add(transform(this[i++], element))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> BooleanArray.zip(other: Iterable<R>, transform: (a: Boolean, b: R) -> V): List<V> {
    val arraySize = size
    val list = ArrayList<V>(minOf(other.collectionSizeOrDefault(10), arraySize))
    var i = 0
    for (element in other) {
        if (i >= arraySize) break
        list.add(transform(this[i++], element))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <R, V> CharArray.zip(other: Iterable<R>, transform: (a: Char, b: R) -> V): List<V> {
    val arraySize = size
    val list = ArrayList<V>(minOf(other.collectionSizeOrDefault(10), arraySize))
    var i = 0
    for (element in other) {
        if (i >= arraySize) break
        list.add(transform(this[i++], element))
    }
    return list
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun ByteArray.zip(other: ByteArray): List<Pair<Byte, Byte>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun ShortArray.zip(other: ShortArray): List<Pair<Short, Short>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun IntArray.zip(other: IntArray): List<Pair<Int, Int>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun LongArray.zip(other: LongArray): List<Pair<Long, Long>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun FloatArray.zip(other: FloatArray): List<Pair<Float, Float>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun DoubleArray.zip(other: DoubleArray): List<Pair<Double, Double>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun BooleanArray.zip(other: BooleanArray): List<Pair<Boolean, Boolean>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun CharArray.zip(other: CharArray): List<Pair<Char, Char>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <V> ByteArray.zip(other: ByteArray, transform: (a: Byte, b: Byte) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <V> ShortArray.zip(other: ShortArray, transform: (a: Short, b: Short) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <V> IntArray.zip(other: IntArray, transform: (a: Int, b: Int) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <V> LongArray.zip(other: LongArray, transform: (a: Long, b: Long) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <V> FloatArray.zip(other: FloatArray, transform: (a: Float, b: Float) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <V> DoubleArray.zip(other: DoubleArray, transform: (a: Double, b: Double) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <V> BooleanArray.zip(other: BooleanArray, transform: (a: Boolean, b: Boolean) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <V> CharArray.zip(other: CharArray, transform: (a: Char, b: Char) -> V): List<V> {
    val size = minOf(size, other.size)
    val list = ArrayList<V>(size)
    for (i in 0..size-1) {
        list.add(transform(this[i], other[i]))
    }
    return list
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public operator fun <T> Array<T>.plus(element: T): Array<T> {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public operator fun ByteArray.plus(element: Byte): ByteArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public operator fun ShortArray.plus(element: Short): ShortArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public operator fun IntArray.plus(element: Int): IntArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public operator fun LongArray.plus(element: Long): LongArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public operator fun FloatArray.plus(element: Float): FloatArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public operator fun DoubleArray.plus(element: Double): DoubleArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public operator fun BooleanArray.plus(element: Boolean): BooleanArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public operator fun CharArray.plus(element: Char): CharArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public operator fun <T> Array<T>.plus(elements: Collection<T>): Array<T> {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public operator fun ByteArray.plus(elements: Collection<Byte>): ByteArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public operator fun ShortArray.plus(elements: Collection<Short>): ShortArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public operator fun IntArray.plus(elements: Collection<Int>): IntArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public operator fun LongArray.plus(elements: Collection<Long>): LongArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public operator fun FloatArray.plus(elements: Collection<Float>): FloatArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public operator fun DoubleArray.plus(elements: Collection<Double>): DoubleArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public operator fun BooleanArray.plus(elements: Collection<Boolean>): BooleanArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public operator fun CharArray.plus(elements: Collection<Char>): CharArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public operator fun <T> Array<T>.plus(elements: Array<out T>): Array<T> {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public operator fun ByteArray.plus(elements: ByteArray): ByteArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public operator fun ShortArray.plus(elements: ShortArray): ShortArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public operator fun IntArray.plus(elements: IntArray): IntArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public operator fun LongArray.plus(elements: LongArray): LongArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public operator fun FloatArray.plus(elements: FloatArray): FloatArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public operator fun DoubleArray.plus(elements: DoubleArray): DoubleArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public operator fun BooleanArray.plus(elements: BooleanArray): BooleanArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public operator fun CharArray.plus(elements: CharArray): CharArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
@kotlin.internal.InlineOnly
public inline fun <T> Array<T>.plusElement(element: T): Array<T> {
    return plus(element)
}
