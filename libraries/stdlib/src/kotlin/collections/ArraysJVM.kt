@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ArraysKt")

package kotlin

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import kotlin.jvm.internal.Intrinsic

// Array "constructor"
/**
 * Returns an array containing the specified elements.
 */
@Intrinsic("kotlin.arrays.array") public fun <reified T> arrayOf(vararg t : T) : Array<T> = t as Array<T>

// "constructors" for primitive types array
/**
 * Returns an array containing the specified [Double] numbers.
 */
@Intrinsic("kotlin.arrays.array") public fun doubleArrayOf(vararg content : Double) : DoubleArray    = content

/**
 * Returns an array containing the specified [Float] numbers.
 */
@Intrinsic("kotlin.arrays.array") public fun floatArrayOf(vararg content : Float) : FloatArray       = content

/**
 * Returns an array containing the specified [Long] numbers.
 */
@Intrinsic("kotlin.arrays.array") public fun longArrayOf(vararg content : Long) : LongArray          = content

/**
 * Returns an array containing the specified [Int] numbers.
 */
@Intrinsic("kotlin.arrays.array") public fun intArrayOf(vararg content : Int) : IntArray             = content

/**
 * Returns an array containing the specified characters.
 */
@Intrinsic("kotlin.arrays.array") public fun charArrayOf(vararg content : Char) : CharArray          = content

/**
 * Returns an array containing the specified [Short] numbers.
 */
@Intrinsic("kotlin.arrays.array") public fun shortArrayOf(vararg content : Short) : ShortArray       = content

/**
 * Returns an array containing the specified [Byte] numbers.
 */
@Intrinsic("kotlin.arrays.array") public fun byteArrayOf(vararg content : Byte) : ByteArray          = content

/**
 * Returns an array containing the specified boolean values.
 */
@Intrinsic("kotlin.arrays.array") public fun booleanArrayOf(vararg content : Boolean) : BooleanArray = content

// TODO: Move inputStream to kotlin.io
/**
 * Creates an input stream for reading data from this byte array.
 */
@Deprecated("Use inputStream() method instead.", ReplaceWith("this.inputStream()", "kotlin.io.inputStream"))
public val ByteArray.inputStream : ByteArrayInputStream
    get() = inputStream()

/**
 * Creates an input stream for reading data from this byte array.
 */
@Deprecated("Use inputStream() method from kotlin.io package instead.", ReplaceWith("this.inputStream()", "kotlin.io.inputStream"))
@HiddenDeclaration
public fun ByteArray.inputStream(): ByteArrayInputStream = ByteArrayInputStream(this)

/**
 * Creates an input stream for reading data from the specified portion of this byte array.
 * @param offset the start offset of the portion of the array to read.
 * @param length the length of the portion of the array to read.
 */
@Deprecated("Use inputStream() method from kotlin.io package instead.", ReplaceWith("this.inputStream(offset, length)", "kotlin.io.inputStream"))
@HiddenDeclaration
public fun ByteArray.inputStream(offset: Int, length: Int) : ByteArrayInputStream = ByteArrayInputStream(this, offset, length)

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
