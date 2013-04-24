package kotlin

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.Arrays
import jet.runtime.Intrinsic

// Array "constructor"
[Intrinsic("kotlin.arrays.array")] public inline fun <T> array(vararg t : T) : Array<T> = t

// "constructors" for primitive types array
[Intrinsic("kotlin.arrays.array")] public inline fun doubleArray(vararg content : Double) : DoubleArray    = content

[Intrinsic("kotlin.arrays.array")] public inline fun floatArray(vararg content : Float) : FloatArray       = content

[Intrinsic("kotlin.arrays.array")] public inline fun longArray(vararg content : Long) : LongArray          = content

[Intrinsic("kotlin.arrays.array")] public inline fun intArray(vararg content : Int) : IntArray             = content

[Intrinsic("kotlin.arrays.array")] public inline fun charArray(vararg content : Char) : CharArray          = content

[Intrinsic("kotlin.arrays.array")] public inline fun shortArray(vararg content : Short) : ShortArray       = content

[Intrinsic("kotlin.arrays.array")] public inline fun byteArray(vararg content : Byte) : ByteArray          = content

[Intrinsic("kotlin.arrays.array")] public inline fun booleanArray(vararg content : Boolean) : BooleanArray = content

public inline fun   ByteArray.binarySearch(key: Byte) : Int   = Arrays.binarySearch(this, key)
public inline fun  ShortArray.binarySearch(key: Short) : Int  = Arrays.binarySearch(this, key)
public inline fun    IntArray.binarySearch(key: Int) : Int    = Arrays.binarySearch(this, key)
public inline fun   LongArray.binarySearch(key: Long) : Int   = Arrays.binarySearch(this, key)
public inline fun  FloatArray.binarySearch(key: Float) : Int  = Arrays.binarySearch(this, key)
public inline fun DoubleArray.binarySearch(key: Double) : Int = Arrays.binarySearch(this, key)
public inline fun   CharArray.binarySearch(key: Char) : Int   = Arrays.binarySearch(this, key)

public inline fun   ByteArray.binarySearch(fromIndex: Int, toIndex: Int, key: Byte) : Int   = Arrays.binarySearch(this, fromIndex, toIndex, key)
public inline fun  ShortArray.binarySearch(fromIndex: Int, toIndex: Int, key: Short) : Int  = Arrays.binarySearch(this, fromIndex, toIndex, key)
public inline fun    IntArray.binarySearch(fromIndex: Int, toIndex: Int, key: Int) : Int    = Arrays.binarySearch(this, fromIndex, toIndex, key)
public inline fun   LongArray.binarySearch(fromIndex: Int, toIndex: Int, key: Long) : Int   = Arrays.binarySearch(this, fromIndex, toIndex, key)
public inline fun  FloatArray.binarySearch(fromIndex: Int, toIndex: Int, key: Float) : Int  = Arrays.binarySearch(this, fromIndex, toIndex, key)
public inline fun DoubleArray.binarySearch(fromIndex: Int, toIndex: Int, key: Double) : Int = Arrays.binarySearch(this, fromIndex, toIndex, key)
public inline fun   CharArray.binarySearch(fromIndex: Int, toIndex: Int, key: Char) : Int   = Arrays.binarySearch(this, fromIndex, toIndex, key)

/*
public inline fun <T> Array<T>.binarySearch(key: T, comparator: public fun(T,T):Int) = Arrays.binarySearch(this, key, object: java.util.Comparator<T> {
    public override fun compare(a: T, b: T) = comparator(a, b)

    public override fun equals(obj: Any?) = obj.identityEquals(this)
})
*/
public inline fun   BooleanArray.fill(value: Boolean) : Unit = Arrays.fill(this, value)
public inline fun      ByteArray.fill(value: Byte) : Unit    = Arrays.fill(this, value)
public inline fun     ShortArray.fill(value: Short) : Unit   = Arrays.fill(this, value)
public inline fun       IntArray.fill(value: Int) : Unit     = Arrays.fill(this, value)
public inline fun      LongArray.fill(value: Long) : Unit    = Arrays.fill(this, value)
public inline fun     FloatArray.fill(value: Float) : Unit   = Arrays.fill(this, value)
public inline fun    DoubleArray.fill(value: Double) : Unit  = Arrays.fill(this, value)
public inline fun      CharArray.fill(value: Char) : Unit    = Arrays.fill(this, value)

public inline fun  <T: Any?>  Array<T>.fill(value: T) : Unit = Arrays.fill(this, value)

public inline fun   ByteArray.sort() : Unit = Arrays.sort(this)
public inline fun  ShortArray.sort() : Unit = Arrays.sort(this)
public inline fun    IntArray.sort() : Unit = Arrays.sort(this)
public inline fun   LongArray.sort() : Unit = Arrays.sort(this)
public inline fun  FloatArray.sort() : Unit = Arrays.sort(this)
public inline fun DoubleArray.sort() : Unit = Arrays.sort(this)
public inline fun   CharArray.sort() : Unit = Arrays.sort(this)

public inline fun   ByteArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)
public inline fun  ShortArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)
public inline fun    IntArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)
public inline fun   LongArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)
public inline fun  FloatArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)
public inline fun DoubleArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)
public inline fun   CharArray.sort(fromIndex: Int, toIndex: Int) : Unit = Arrays.sort(this, fromIndex, toIndex)

public inline fun BooleanArray.copyOf(newLength: Int = this.size) : BooleanArray = Arrays.copyOf(this, newLength)
public inline fun    ByteArray.copyOf(newLength: Int = this.size) : ByteArray    = Arrays.copyOf(this, newLength)
public inline fun   ShortArray.copyOf(newLength: Int = this.size) : ShortArray   = Arrays.copyOf(this, newLength)
public inline fun     IntArray.copyOf(newLength: Int = this.size) : IntArray     = Arrays.copyOf(this, newLength)
public inline fun    LongArray.copyOf(newLength: Int = this.size) : LongArray    = Arrays.copyOf(this, newLength)
public inline fun   FloatArray.copyOf(newLength: Int = this.size) : FloatArray   = Arrays.copyOf(this, newLength)
public inline fun  DoubleArray.copyOf(newLength: Int = this.size) : DoubleArray  = Arrays.copyOf(this, newLength)
public inline fun    CharArray.copyOf(newLength: Int = this.size) : CharArray    = Arrays.copyOf(this, newLength)

// TODO: resuling array may contain nulls even if T is non-nullable
public inline fun  <T> Array<T>.copyOf(newLength: Int = this.size) : Array<T> = Arrays.copyOf(this, newLength) as Array<T>

public inline fun BooleanArray.copyOfRange(from: Int, to: Int) : BooleanArray = Arrays.copyOfRange(this, from, to)
public inline fun    ByteArray.copyOfRange(from: Int, to: Int) : ByteArray    = Arrays.copyOfRange(this, from, to)
public inline fun   ShortArray.copyOfRange(from: Int, to: Int) : ShortArray   = Arrays.copyOfRange(this, from, to)
public inline fun     IntArray.copyOfRange(from: Int, to: Int) : IntArray     = Arrays.copyOfRange(this, from, to)
public inline fun    LongArray.copyOfRange(from: Int, to: Int) : LongArray    = Arrays.copyOfRange(this, from, to)
public inline fun   FloatArray.copyOfRange(from: Int, to: Int) : FloatArray   = Arrays.copyOfRange(this, from, to)
public inline fun  DoubleArray.copyOfRange(from: Int, to: Int) : DoubleArray  = Arrays.copyOfRange(this, from, to)
public inline fun    CharArray.copyOfRange(from: Int, to: Int) : CharArray    = Arrays.copyOfRange(this, from, to)

// TODO: resuling array may contain nulls even if T is non-nullable
public inline fun  <T> Array<T>.copyOfRange(from: Int, to: Int) : Array<T> = Arrays.copyOfRange(this, from, to) as Array<T>

public inline val ByteArray.inputStream : ByteArrayInputStream
    get() = ByteArrayInputStream(this)

public inline fun ByteArray.inputStream(offset: Int, length: Int) : ByteArrayInputStream = ByteArrayInputStream(this, offset, length)

public inline fun ByteArray.toString(encoding: String): String = String(this, encoding)
public inline fun ByteArray.toString(): String = String(this)

public inline fun ByteArray.toString(encoding: Charset) : String = String(this, encoding)