package kotlin

import java.util.*

@library
public fun <T> arrayOf(vararg value : T): Array<T> = noImpl

// "constructors" for primitive types array

@library
public fun doubleArrayOf(vararg content : Double): DoubleArray    = noImpl

@library
public fun floatArrayOf(vararg content : Float): FloatArray       = noImpl

@library
public fun longArrayOf(vararg content : Long): LongArray          = noImpl

@library
public fun intArrayOf(vararg content : Int): IntArray             = noImpl

@library
public fun charArrayOf(vararg content : Char): CharArray          = noImpl

@library
public fun shortArrayOf(vararg content : Short): ShortArray       = noImpl

@library
public fun byteArrayOf(vararg content : Byte): ByteArray          = noImpl

@library
public fun booleanArrayOf(vararg content : Boolean): BooleanArray = noImpl

@library("copyToArray")
public fun <reified T> Collection<T>.toTypedArray(): Array<T> = noImpl


/**
 * Returns an immutable list containing only the specified object [value].
 */
public fun listOf<T>(value: T): List<T> = arrayListOf(value)

/**
 * Returns an immutable set containing only the specified object [value].
 */
public fun setOf<T>(value: T): Set<T> = hashSetOf(value)

/**
 * Returns an immutable map, mapping only the specified key to the
 * specified value.
 */
public fun mapOf<K, V>(keyValuePair: Pair<K, V>): Map<K, V> = hashMapOf(keyValuePair)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 */
public fun lazy<T>(initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [mode] parameter is ignored. */
public fun lazy<T>(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [lock] parameter is ignored.
 */
public fun lazy<T>(lock: Any?, initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)


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
    result.length += collection.size()
    var index: Int = array.length
    for (element in collection) result[index++] = element
    return result
}
