@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ArraysKt")

package kotlin.collections

import java.io.ByteArrayInputStream
import java.nio.charset.Charset

// Array "constructor"
/**
 * Returns an array containing the specified elements.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public inline fun <reified T> arrayOf(vararg elements: T) : Array<T> = elements as Array<T>

// "constructors" for primitive types array
/**
 * Returns an array containing the specified [Double] numbers.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun doubleArrayOf(vararg elements: Double) : DoubleArray    = elements

/**
 * Returns an array containing the specified [Float] numbers.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun floatArrayOf(vararg elements: Float) : FloatArray       = elements

/**
 * Returns an array containing the specified [Long] numbers.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun longArrayOf(vararg elements: Long) : LongArray          = elements

/**
 * Returns an array containing the specified [Int] numbers.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun intArrayOf(vararg elements: Int) : IntArray             = elements

/**
 * Returns an array containing the specified characters.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun charArrayOf(vararg elements: Char) : CharArray          = elements

/**
 * Returns an array containing the specified [Short] numbers.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun shortArrayOf(vararg elements: Short) : ShortArray       = elements

/**
 * Returns an array containing the specified [Byte] numbers.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun byteArrayOf(vararg elements: Byte) : ByteArray          = elements

/**
 * Returns an array containing the specified boolean values.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun booleanArrayOf(vararg elements: Boolean) : BooleanArray = elements

/**
 * Converts the contents of this byte array to a string using the specified [charset].
 */
public fun ByteArray.toString(charset: String): String = String(this, charset)

/**
 * Converts the contents of this byte array to a string using the specified [charset].
 */
public fun ByteArray.toString(charset: Charset): String = String(this, charset)

/**
 * Returns a *typed* array containing all of the elements of this collection.
 *
 * Allocates an array of runtime type `T` having its size equal to the size of this collection
 * and populates the array with the elements of this collection.
 */
public inline fun <reified T> Collection<T>.toTypedArray(): Array<T> {
    val thisCollection = this as java.util.Collection<T>
    return thisCollection.toArray(arrayOfNulls<T>(thisCollection.size())) as Array<T>
}

/** Returns the array if it's not `null`, or an empty array otherwise. */
public inline fun <reified T> Array<out T>?.orEmpty(): Array<out T> = this ?: arrayOf<T>()

/** Internal unsafe construction of array based on reference array type */
internal fun <T> arrayOfNulls(reference: Array<T>, size: Int): Array<T> {
    return java.lang.reflect.Array.newInstance(reference.javaClass.componentType, size) as Array<T>
}
