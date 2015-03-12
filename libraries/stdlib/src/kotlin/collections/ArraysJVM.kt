package kotlin

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.Arrays
import kotlin.jvm.internal.Intrinsic

// Array "constructor"
/**
 * Returns an array containing the specified elements.
 */
[Intrinsic("kotlin.arrays.array")] public fun <reified T> array(vararg t : T) : Array<T> = t as Array<T>

// "constructors" for primitive types array
/**
 * Returns an array containing the specified [Double] numbers.
 */
[Intrinsic("kotlin.arrays.array")] public fun doubleArray(vararg content : Double) : DoubleArray    = content

/**
 * Returns an array containing the specified [Float] numbers.
 */
[Intrinsic("kotlin.arrays.array")] public fun floatArray(vararg content : Float) : FloatArray       = content

/**
 * Returns an array containing the specified [Long] numbers.
 */
[Intrinsic("kotlin.arrays.array")] public fun longArray(vararg content : Long) : LongArray          = content

/**
 * Returns an array containing the specified [Int] numbers.
 */
[Intrinsic("kotlin.arrays.array")] public fun intArray(vararg content : Int) : IntArray             = content

/**
 * Returns an array containing the specified characters.
 */
[Intrinsic("kotlin.arrays.array")] public fun charArray(vararg content : Char) : CharArray          = content

/**
 * Returns an array containing the specified [Short] numbers.
 */
[Intrinsic("kotlin.arrays.array")] public fun shortArray(vararg content : Short) : ShortArray       = content

/**
 * Returns an array containing the specified [Byte] numbers.
 */
[Intrinsic("kotlin.arrays.array")] public fun byteArray(vararg content : Byte) : ByteArray          = content

/**
 * Returns an array containing the specified boolean values.
 */
[Intrinsic("kotlin.arrays.array")] public fun booleanArray(vararg content : Boolean) : BooleanArray = content

/**
 * Creates an input stream for reading data from this byte array.
 */
public val ByteArray.inputStream : ByteArrayInputStream
    get() = ByteArrayInputStream(this)

/**
 * Creates an input stream for reading data from the specified portion of this byte array.
 * @param offset the start offset of the portion of the array to read.
 * @param length the length of the portion of the array to read.
 */
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
 * Returns an array containing the elements of this collection.
 */
[Intrinsic("kotlin.collections.copyToArray")] public fun <reified T> Collection<T>.copyToArray(): Array<T> =
        throw UnsupportedOperationException()

/** Returns the array if it's not null, or an empty array otherwise. */
public inline fun <reified T> Array<out T>?.orEmpty(): Array<out T> = this ?: array<T>()

