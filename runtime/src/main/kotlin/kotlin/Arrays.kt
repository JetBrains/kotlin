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

package kotlin

import kotlin.collections.*
import kotlin.internal.PureReifiable
import kotlin.util.sortArrayComparable
import kotlin.util.sortArrayWith
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
    public inline constructor(size: Int, init: (Int) -> Byte): this(size) {
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
    public inline constructor(size: Int, init: (Int) -> Char): this(size) {
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

    @SymbolName("Kotlin_CharArray_copyOf")
    external public fun copyOf(newSize: Int): CharArray

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
    public inline constructor(size: Int, init: (Int) -> Short): this(size) {
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
    public inline constructor(size: Int, init: (Int) -> Int): this(size) {
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
    public inline constructor(size: Int, init: (Int) -> Long): this(size) {
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
    public inline constructor(size: Int, init: (Int) -> Float): this(size) {
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
    public inline constructor(size: Int, init: (Int) -> Double): this(size) {
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
    public inline constructor(size: Int, init: (Int) -> Boolean): this(size) {
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
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun <T> Array<out T>.any(predicate: (T) -> Boolean): Boolean {
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
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun <T> Array<out T>.filterNot(predicate: (T) -> Boolean): List<T> {
    return filterNotTo(ArrayList<T>(), predicate)
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <T, C : MutableCollection<in T>> Array<out T>.filterNotTo(destination: C, predicate: (T) -> Boolean): C {
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
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun <T> Array<out T>.findLast(predicate: (T) -> Boolean): T? {
    return lastOrNull(predicate)
}

/**
 * Returns the first element, or `null` if the array is empty.
 */
public fun <T> Array<out T>.firstOrNull(): T? {
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
 * Returns the last element, or `null` if the array is empty.
 */
public fun <T> Array<out T>.lastOrNull(): T? {
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
 * Applies the given [transform] function to each element of the original array
 * and appends the results to the given [destination].
 */
public inline fun <T, R, C : MutableCollection<in R>> Array<out T>.mapTo(destination: C, transform: (T) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original array.
 */
public inline fun <T, R> Array<out T>.map(transform: (T) -> R): List<R> {
    return mapTo(ArrayList<R>(size), transform)
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
 * Returns a [MutableList] filled with all elements of this array.
 */
public fun <T> Array<out T>.toMutableList(): MutableList<T> {
    return ArrayList(this.asCollection())
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
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Array<T>.copyOf(): Array<T> {
    return this.copyOfUninitializedElements(size)
}

/**
 * Sorts the array in-place according to the order specified by the given [comparator].
 */
public fun <T> Array<out T>.sortWith(comparator: Comparator<in T>): Unit {
    if (size > 1) {
        sortArrayWith(this, 0, size - 1, comparator)
    }
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
 * Returns a list of all elements sorted according to their natural sort order.
 */
public fun <T : Comparable<T>> Array<out T>.sorted(): List<T> {
    return sortedArray().asList()
}

/**
 * Returns a list of all elements sorted according to their natural sort order.
 */
public fun IntArray.sorted(): List<Int> {
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
 * Returns an array with all elements of this array sorted descending according to their natural sort order.
 */
public fun <T : Comparable<T>> Array<T>.sortedArrayDescending(): Array<T> {
    if (isEmpty()) return this
    return this.copyOf().apply { sortWith(reverseOrder()) }
}

/**
 * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> Array<out T>.sortedBy(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> Array<out T>.sortedByDescending(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a list of all elements sorted descending according to their natural sort order.
 */
public fun <T : Comparable<T>> Array<out T>.sortedDescending(): List<T> {
    return sortedWith(reverseOrder())
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
 * Returns a list of all elements sorted according to the specified [comparator].
 */
public fun <T> Array<out T>.sortedWith(comparator: Comparator<in T>): List<T> {
    return sortedArrayWith(comparator).asList()
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
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline infix fun <T> Array<out T>.contentEquals(other: Array<out T>): Boolean {
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
@kotlin.internal.InlineOnly
public inline infix fun ByteArray.contentEquals(other: ByteArray): Boolean {
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
@kotlin.internal.InlineOnly
public inline infix fun ShortArray.contentEquals(other: ShortArray): Boolean {
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
@kotlin.internal.InlineOnly
public inline infix fun IntArray.contentEquals(other: IntArray): Boolean {
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
@kotlin.internal.InlineOnly
public inline infix fun LongArray.contentEquals(other: LongArray): Boolean {
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
@kotlin.internal.InlineOnly
public inline infix fun FloatArray.contentEquals(other: FloatArray): Boolean {
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
@kotlin.internal.InlineOnly
public inline infix fun DoubleArray.contentEquals(other: DoubleArray): Boolean {
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
@kotlin.internal.InlineOnly
public inline infix fun BooleanArray.contentEquals(other: BooleanArray): Boolean {
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
@kotlin.internal.InlineOnly
public inline infix fun CharArray.contentEquals(other: CharArray): Boolean {
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
