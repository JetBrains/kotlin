package kotlin

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.Arrays
import kotlin.jvm.internal.Intrinsic

// Array "constructor"
[Intrinsic("kotlin.arrays.array")] public fun <reified T> array(vararg t : T) : Array<T> = t as Array<T>

// "constructors" for primitive types array
[Intrinsic("kotlin.arrays.array")] public fun doubleArray(vararg content : Double) : DoubleArray    = content

[Intrinsic("kotlin.arrays.array")] public fun floatArray(vararg content : Float) : FloatArray       = content

[Intrinsic("kotlin.arrays.array")] public fun longArray(vararg content : Long) : LongArray          = content

[Intrinsic("kotlin.arrays.array")] public fun intArray(vararg content : Int) : IntArray             = content

[Intrinsic("kotlin.arrays.array")] public fun charArray(vararg content : Char) : CharArray          = content

[Intrinsic("kotlin.arrays.array")] public fun shortArray(vararg content : Short) : ShortArray       = content

[Intrinsic("kotlin.arrays.array")] public fun byteArray(vararg content : Byte) : ByteArray          = content

[Intrinsic("kotlin.arrays.array")] public fun booleanArray(vararg content : Boolean) : BooleanArray = content

public val ByteArray.inputStream : ByteArrayInputStream
    get() = ByteArrayInputStream(this)

public fun ByteArray.inputStream(offset: Int, length: Int) : ByteArrayInputStream = ByteArrayInputStream(this, offset, length)

public fun ByteArray.toString(charset: String): String = String(this, charset)
public fun ByteArray.toString(charset: Charset): String = String(this, charset)

[Intrinsic("kotlin.collections.copyToArray")] public fun <reified T> Collection<T>.copyToArray(): Array<T> =
        throw UnsupportedOperationException()

/** Returns the List if its not null otherwise returns the empty list */
public inline fun <reified T> Array<out T>?.orEmpty(): Array<out T> = this ?: array<T>()
