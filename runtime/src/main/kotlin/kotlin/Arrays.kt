package kotlin

import kotlin.collections.*

/**
 * An array of bytes.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theByteArrayTypeInfo")
public final class ByteArray : Cloneable {
    // Constructors are handled with compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_ByteArray_get")
    external public operator fun get(index: Int): Byte

    @SymbolName("Kotlin_ByteArray_set")
    external public operator fun set(index: Int, value: Byte): Unit

    @SymbolName("Kotlin_ByteArray_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_ByteArray_getArrayLength")
    external private fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): kotlin.collections.ByteIterator {
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
public final class CharArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_CharArray_get")
    external public operator fun get(index: Int): Char

    @SymbolName("Kotlin_CharArray_set")
    external public operator fun set(index: Int, value: Char): Unit

    @SymbolName("Kotlin_CharArray_clone")
    external public override fun clone(): Any

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
public final class ShortArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_ShortArray_get")
    external public operator fun get(index: Int): Short

    @SymbolName("Kotlin_ShortArray_set")
    external public operator fun set(index: Int, value: Short): Unit

    @SymbolName("Kotlin_ShortArray_clone")
    external public override fun clone(): Any

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
public final class IntArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_IntArray_get")
    external public operator fun get(index: Int): Int

    @SymbolName("Kotlin_IntArray_set")
    external public operator fun set(index: Int, value: Int): Unit

    @SymbolName("Kotlin_IntArray_clone")
    external public override fun clone(): Any

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
public final class LongArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_LongArray_get")
    external public operator fun get(index: Int): Long

    @SymbolName("Kotlin_LongArray_set")
    external public operator fun set(index: Int, value: Long): Unit

    @SymbolName("Kotlin_LongArray_clone")
    external public override fun clone(): Any

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
public final class FloatArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_FloatArray_get")
    external public operator fun get(index: Int): Float

    @SymbolName("Kotlin_FloatArray_set")
    external public operator fun set(index: Int, value: Float): Unit

    @SymbolName("Kotlin_FloatArray_clone")
    external public override fun clone(): Any

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
public final class DoubleArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_DoubleArray_get")
    external public operator fun get(index: Int): Double

    @SymbolName("Kotlin_DoubleArray_set")
    external public operator fun set(index: Int, value: Double): Unit

    @SymbolName("Kotlin_DoubleArray_clone")
    external public override fun clone(): Any

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
public final class BooleanArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_BooleanArray_get")
    external public operator fun get(index: Int): Boolean

    @SymbolName("Kotlin_BooleanArray_set")
    external public operator fun set(index: Int, value: Boolean): Unit

    @SymbolName("Kotlin_BooleanArray_clone")
    external public override fun clone(): Any

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
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun <T> Array<out T>.all(predicate: (T) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if [element] is found in the array.
 */
public operator fun <@kotlin.internal.OnlyInputTypes T> Array<out T>.contains(element: T): Boolean {
    return indexOf(element) >= 0
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
 * Returns `true` if the array is empty.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Array<out T>.isEmpty(): Boolean {
    return size == 0
}

/**
 * Returns the range of valid indices for the array.
 */
public val <T> Array<out T>.indices: IntRange
    get() = IntRange(0, lastIndex)

/**
 * Returns the last valid index for the array.
 */
public val <T> Array<out T>.lastIndex: Int
    get() = size - 1

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


