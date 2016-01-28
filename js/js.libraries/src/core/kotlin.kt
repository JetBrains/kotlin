package kotlin

import java.util.*

/**
 * Returns an empty array of the specified type [T].
 */
public inline fun <reified T> emptyArray(): Array<T> = arrayOfNulls<T>(0) as Array<T>

@library
public fun <T> arrayOf(vararg elements: T): Array<T> = noImpl

@library
public fun doubleArrayOf(vararg elements: Double): DoubleArray = noImpl

@library
public fun floatArrayOf(vararg elements: Float): FloatArray = noImpl

@library
public fun longArrayOf(vararg elements: Long): LongArray = noImpl

@library
public fun intArrayOf(vararg elements: Int): IntArray = noImpl

@library
public fun charArrayOf(vararg elements: Char): CharArray = noImpl

@library
public fun shortArrayOf(vararg elements: Short): ShortArray = noImpl

@library
public fun byteArrayOf(vararg elements: Byte): ByteArray = noImpl

@library
public fun booleanArrayOf(vararg elements: Boolean): BooleanArray = noImpl

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 */
public fun <T> lazy(initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [mode] parameter is ignored. */
public fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [lock] parameter is ignored.
 */
public fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)


internal fun <T> arrayOfNulls(reference: Array<out T>, size: Int): Array<T> {
    return arrayOfNulls<Any>(size) as Array<T>
}

internal fun arrayCopyResize(source: dynamic, newSize: Int, defaultValue: Any?): dynamic {
    val result = source.slice(0, newSize)
    var index: Int = source.length
    if (newSize > index) {
        result.length = newSize
        while (index < newSize) result[index++] = defaultValue
    }
    return result
}

internal fun <T> arrayPlusCollection(array: dynamic, collection: Collection<T>): dynamic {
    val result = array.slice(0)
    result.length += collection.size
    var index: Int = array.length
    for (element in collection) result[index++] = element
    return result
}

// copies vararg array due to different spread vararg behavior in JS.
// After fixing #KT-6491 may return `this`
internal inline fun <T> Array<out T>.varargToArrayOfAny(): Array<out Any?> = this.copyOf()
