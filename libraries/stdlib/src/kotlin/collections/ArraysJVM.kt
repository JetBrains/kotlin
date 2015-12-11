@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ArraysKt")

package kotlin

import java.io.ByteArrayInputStream
import java.nio.charset.Charset

// Array "constructor"
/**
 * Returns an array containing the specified elements.
 */
public inline fun <reified T> arrayOf(vararg elements: T) : Array<T> = elements as Array<T>

// "constructors" for primitive types array
/**
 * Returns an array containing the specified [Double] numbers.
 */
public fun doubleArrayOf(vararg elements: Double) : DoubleArray    = elements

/**
 * Returns an array containing the specified [Float] numbers.
 */
public fun floatArrayOf(vararg elements: Float) : FloatArray       = elements

/**
 * Returns an array containing the specified [Long] numbers.
 */
public fun longArrayOf(vararg elements: Long) : LongArray          = elements

/**
 * Returns an array containing the specified [Int] numbers.
 */
public fun intArrayOf(vararg elements: Int) : IntArray             = elements

/**
 * Returns an array containing the specified characters.
 */
public fun charArrayOf(vararg elements: Char) : CharArray          = elements

/**
 * Returns an array containing the specified [Short] numbers.
 */
public fun shortArrayOf(vararg elements: Short) : ShortArray       = elements

/**
 * Returns an array containing the specified [Byte] numbers.
 */
public fun byteArrayOf(vararg elements: Byte) : ByteArray          = elements

/**
 * Returns an array containing the specified boolean values.
 */
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
internal fun <T> arrayOfNulls(reference: Array<out T>, size: Int): Array<out T> {
    return java.lang.reflect.Array.newInstance(reference.javaClass.componentType, size) as Array<out T>
}
