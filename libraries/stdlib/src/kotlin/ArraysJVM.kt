package kotlin

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.Arrays
import kotlin.jvm.internal.Intrinsic

// Array "constructor"
[Intrinsic("kotlin.arrays.array")] public fun <reified T> array(vararg t : T) : Array<T> = t

// "constructors" for primitive types array
[Intrinsic("kotlin.arrays.array")] public fun doubleArray(vararg content : Double) : DoubleArray    = content

[Intrinsic("kotlin.arrays.array")] public fun floatArray(vararg content : Float) : FloatArray       = content

[Intrinsic("kotlin.arrays.array")] public fun longArray(vararg content : Long) : LongArray          = content

[Intrinsic("kotlin.arrays.array")] public fun intArray(vararg content : Int) : IntArray             = content

[Intrinsic("kotlin.arrays.array")] public fun charArray(vararg content : Char) : CharArray          = content

[Intrinsic("kotlin.arrays.array")] public fun shortArray(vararg content : Short) : ShortArray       = content

[Intrinsic("kotlin.arrays.array")] public fun byteArray(vararg content : Byte) : ByteArray          = content

[Intrinsic("kotlin.arrays.array")] public fun booleanArray(vararg content : Boolean) : BooleanArray = content

public fun   ByteArray.binarySearch(key: Byte) : Int   = Arrays.binarySearch(this, key)
public fun  ShortArray.binarySearch(key: Short) : Int  = Arrays.binarySearch(this, key)
public fun    IntArray.binarySearch(key: Int) : Int    = Arrays.binarySearch(this, key)
public fun   LongArray.binarySearch(key: Long) : Int   = Arrays.binarySearch(this, key)
public fun  FloatArray.binarySearch(key: Float) : Int  = Arrays.binarySearch(this, key)
public fun DoubleArray.binarySearch(key: Double) : Int = Arrays.binarySearch(this, key)
public fun   CharArray.binarySearch(key: Char) : Int   = Arrays.binarySearch(this, key)

public fun   ByteArray.binarySearch(fromIndex: Int, toIndex: Int, key: Byte) : Int   = Arrays.binarySearch(this, fromIndex, toIndex, key)
public fun  ShortArray.binarySearch(fromIndex: Int, toIndex: Int, key: Short) : Int  = Arrays.binarySearch(this, fromIndex, toIndex, key)
public fun    IntArray.binarySearch(fromIndex: Int, toIndex: Int, key: Int) : Int    = Arrays.binarySearch(this, fromIndex, toIndex, key)
public fun   LongArray.binarySearch(fromIndex: Int, toIndex: Int, key: Long) : Int   = Arrays.binarySearch(this, fromIndex, toIndex, key)
public fun  FloatArray.binarySearch(fromIndex: Int, toIndex: Int, key: Float) : Int  = Arrays.binarySearch(this, fromIndex, toIndex, key)
public fun DoubleArray.binarySearch(fromIndex: Int, toIndex: Int, key: Double) : Int = Arrays.binarySearch(this, fromIndex, toIndex, key)
public fun   CharArray.binarySearch(fromIndex: Int, toIndex: Int, key: Char) : Int   = Arrays.binarySearch(this, fromIndex, toIndex, key)

/*
public inline fun <T> Array<T>.binarySearch(key: T, comparator: public fun(T,T):Int) = Arrays.binarySearch(this, key, object: java.util.Comparator<T> {
    public override fun compare(a: T, b: T) = comparator(a, b)

    public override fun equals(obj: Any?) = obj.identityEquals(this)
})
*/
public fun   BooleanArray.fill(value: Boolean) : Unit = Arrays.fill(this, value)
public fun      ByteArray.fill(value: Byte) : Unit    = Arrays.fill(this, value)
public fun     ShortArray.fill(value: Short) : Unit   = Arrays.fill(this, value)
public fun       IntArray.fill(value: Int) : Unit     = Arrays.fill(this, value)
public fun      LongArray.fill(value: Long) : Unit    = Arrays.fill(this, value)
public fun     FloatArray.fill(value: Float) : Unit   = Arrays.fill(this, value)
public fun    DoubleArray.fill(value: Double) : Unit  = Arrays.fill(this, value)
public fun      CharArray.fill(value: Char) : Unit    = Arrays.fill(this, value)

public fun  <T: Any?>  Array<T>.fill(value: T) : Unit = Arrays.fill(this, value)

public fun   ByteArray.sort() : Unit = Arrays.sort(this)
public fun  ShortArray.sort() : Unit = Arrays.sort(this)
public fun    IntArray.sort() : Unit = Arrays.sort(this)
public fun   LongArray.sort() : Unit = Arrays.sort(this)
public fun  FloatArray.sort() : Unit = Arrays.sort(this)
public fun DoubleArray.sort() : Unit = Arrays.sort(this)
public fun   CharArray.sort() : Unit = Arrays.sort(this)

public fun   ByteArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)
public fun  ShortArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)
public fun    IntArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)
public fun   LongArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)
public fun  FloatArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)
public fun DoubleArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)
public fun   CharArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)

public fun BooleanArray.copyOf(newLength: Int = this.size) : BooleanArray = Arrays.copyOf(this, newLength)
public fun    ByteArray.copyOf(newLength: Int = this.size) : ByteArray    = Arrays.copyOf(this, newLength)
public fun   ShortArray.copyOf(newLength: Int = this.size) : ShortArray   = Arrays.copyOf(this, newLength)
public fun     IntArray.copyOf(newLength: Int = this.size) : IntArray     = Arrays.copyOf(this, newLength)
public fun    LongArray.copyOf(newLength: Int = this.size) : LongArray    = Arrays.copyOf(this, newLength)
public fun   FloatArray.copyOf(newLength: Int = this.size) : FloatArray   = Arrays.copyOf(this, newLength)
public fun  DoubleArray.copyOf(newLength: Int = this.size) : DoubleArray  = Arrays.copyOf(this, newLength)
public fun    CharArray.copyOf(newLength: Int = this.size) : CharArray    = Arrays.copyOf(this, newLength)

// TODO: resuling array may contain nulls even if T is non-nullable
public fun  <T> Array<T>.copyOf(newLength: Int = this.size) : Array<T> = Arrays.copyOf(this, newLength) as Array<T>

public fun BooleanArray.copyOfRange(from: Int, to: Int) : BooleanArray = Arrays.copyOfRange(this, from, to)
public fun    ByteArray.copyOfRange(from: Int, to: Int) : ByteArray    = Arrays.copyOfRange(this, from, to)
public fun   ShortArray.copyOfRange(from: Int, to: Int) : ShortArray   = Arrays.copyOfRange(this, from, to)
public fun     IntArray.copyOfRange(from: Int, to: Int) : IntArray     = Arrays.copyOfRange(this, from, to)
public fun    LongArray.copyOfRange(from: Int, to: Int) : LongArray    = Arrays.copyOfRange(this, from, to)
public fun   FloatArray.copyOfRange(from: Int, to: Int) : FloatArray   = Arrays.copyOfRange(this, from, to)
public fun  DoubleArray.copyOfRange(from: Int, to: Int) : DoubleArray  = Arrays.copyOfRange(this, from, to)
public fun    CharArray.copyOfRange(from: Int, to: Int) : CharArray    = Arrays.copyOfRange(this, from, to)

// TODO: resuling array may contain nulls even if T is non-nullable
public fun  <T> Array<T>.copyOfRange(from: Int, to: Int) : Array<T> = Arrays.copyOfRange(this, from, to) as Array<T>

public val ByteArray.inputStream : ByteArrayInputStream
    get() = ByteArrayInputStream(this)

public fun ByteArray.inputStream(offset: Int, length: Int) : ByteArrayInputStream = ByteArrayInputStream(this, offset, length)

public fun ByteArray.toString(encoding: String): String = String(this, encoding)
public fun ByteArray.toString(): String = String(this)

public fun ByteArray.toString(encoding: Charset) : String = String(this, encoding)

[Intrinsic("kotlin.collections.copyToArray")] public fun <reified T> Collection<T>.copyToArray(): Array<T> =
        throw UnsupportedOperationException()
