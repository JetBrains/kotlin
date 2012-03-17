package kotlin

import java.io.ByteArrayInputStream

import java.util.Arrays
import java.nio.charset.Charset

// Array "constructor"
inline fun <T> array(vararg t : T) : Array<T> = t

// "constructors" for primitive types array
inline fun doubleArray(vararg content : Double)    = content

inline fun floatArray(vararg content : Float)      = content

inline fun longArray(vararg content : Long)        = content

inline fun intArray(vararg content : Int)          = content

inline fun charArray(vararg content : Char)        = content

inline fun shortArray(vararg content : Short)      = content

inline fun byteArray(vararg content : Byte)        = content

inline fun booleanArray(vararg content : Boolean)  = content

inline fun   ByteArray.binarySearch(key: Byte)   = Arrays.binarySearch(this, key)
inline fun  ShortArray.binarySearch(key: Short)  = Arrays.binarySearch(this, key)
inline fun    IntArray.binarySearch(key: Int)    = Arrays.binarySearch(this, key)
inline fun   LongArray.binarySearch(key: Long)   = Arrays.binarySearch(this, key)
inline fun  FloatArray.binarySearch(key: Float)  = Arrays.binarySearch(this, key)
inline fun DoubleArray.binarySearch(key: Double) = Arrays.binarySearch(this, key)
inline fun   CharArray.binarySearch(key: Char)   = Arrays.binarySearch(this, key)

inline fun   ByteArray.binarySearch(fromIndex: Int, toIndex: Int, key: Byte)   = Arrays.binarySearch(this, fromIndex, toIndex, key)
inline fun  ShortArray.binarySearch(fromIndex: Int, toIndex: Int, key: Short)  = Arrays.binarySearch(this, fromIndex, toIndex, key)
inline fun    IntArray.binarySearch(fromIndex: Int, toIndex: Int, key: Int)    = Arrays.binarySearch(this, fromIndex, toIndex, key)
inline fun   LongArray.binarySearch(fromIndex: Int, toIndex: Int, key: Long)   = Arrays.binarySearch(this, fromIndex, toIndex, key)
inline fun  FloatArray.binarySearch(fromIndex: Int, toIndex: Int, key: Float)  = Arrays.binarySearch(this, fromIndex, toIndex, key)
inline fun DoubleArray.binarySearch(fromIndex: Int, toIndex: Int, key: Double) = Arrays.binarySearch(this, fromIndex, toIndex, key)
inline fun   CharArray.binarySearch(fromIndex: Int, toIndex: Int, key: Char)   = Arrays.binarySearch(this, fromIndex, toIndex, key)

/*
inline fun <T> Array<T>.binarySearch(key: T, comparator: fun(T,T):Int) = Arrays.binarySearch(this, key, object: java.util.Comparator<T> {
    override fun compare(a: T, b: T) = comparator(a, b)

    override fun equals(obj: Any?) = obj.identityEquals(this)
})
*/
inline fun   BooleanArray.fill(value: Boolean) = Arrays.fill(this, value)
inline fun      ByteArray.fill(value: Byte)    = Arrays.fill(this, value)
inline fun     ShortArray.fill(value: Short)   = Arrays.fill(this, value)
inline fun       IntArray.fill(value: Int)     = Arrays.fill(this, value)
inline fun      LongArray.fill(value: Long)    = Arrays.fill(this, value)
inline fun     FloatArray.fill(value: Float)   = Arrays.fill(this, value)
inline fun    DoubleArray.fill(value: Double)  = Arrays.fill(this, value)
inline fun      CharArray.fill(value: Char)    = Arrays.fill(this, value)

inline fun  <in T: Any?>  Array<T>.fill(value: T) = Arrays.fill(this as Array<Any?>, value)

inline fun   ByteArray.sort() = Arrays.sort(this)
inline fun  ShortArray.sort() = Arrays.sort(this)
inline fun    IntArray.sort() = Arrays.sort(this)
inline fun   LongArray.sort() = Arrays.sort(this)
inline fun  FloatArray.sort() = Arrays.sort(this)
inline fun DoubleArray.sort() = Arrays.sort(this)
inline fun   CharArray.sort() = Arrays.sort(this)

inline fun   ByteArray.sort(fromIndex: Int, toIndex: Int) = Arrays.sort(this, fromIndex, toIndex)
inline fun  ShortArray.sort(fromIndex: Int, toIndex: Int) = Arrays.sort(this, fromIndex, toIndex)
inline fun    IntArray.sort(fromIndex: Int, toIndex: Int) = Arrays.sort(this, fromIndex, toIndex)
inline fun   LongArray.sort(fromIndex: Int, toIndex: Int) = Arrays.sort(this, fromIndex, toIndex)
inline fun  FloatArray.sort(fromIndex: Int, toIndex: Int) = Arrays.sort(this, fromIndex, toIndex)
inline fun DoubleArray.sort(fromIndex: Int, toIndex: Int) = Arrays.sort(this, fromIndex, toIndex)
inline fun   CharArray.sort(fromIndex: Int, toIndex: Int) = Arrays.sort(this, fromIndex, toIndex)

inline fun BooleanArray.copyOf(newLength: Int = this.size) = Arrays.copyOf(this, newLength).sure()
inline fun    ByteArray.copyOf(newLength: Int = this.size) = Arrays.copyOf(this, newLength).sure()
inline fun   ShortArray.copyOf(newLength: Int = this.size) = Arrays.copyOf(this, newLength).sure()
inline fun     IntArray.copyOf(newLength: Int = this.size) = Arrays.copyOf(this, newLength).sure()
inline fun    LongArray.copyOf(newLength: Int = this.size) = Arrays.copyOf(this, newLength).sure()
inline fun   FloatArray.copyOf(newLength: Int = this.size) = Arrays.copyOf(this, newLength).sure()
inline fun  DoubleArray.copyOf(newLength: Int = this.size) = Arrays.copyOf(this, newLength).sure()
inline fun    CharArray.copyOf(newLength: Int = this.size) = Arrays.copyOf(this, newLength).sure()

inline fun  <T> Array<T>.copyOf(newLength: Int = this.size) : Array<T> = Arrays.copyOf(this, newLength).sure()

inline fun BooleanArray.copyOfRange(from: Int, to: Int) = Arrays.copyOfRange(this, from, to).sure()
inline fun    ByteArray.copyOfRange(from: Int, to: Int) = Arrays.copyOfRange(this, from, to).sure()
inline fun   ShortArray.copyOfRange(from: Int, to: Int) = Arrays.copyOfRange(this, from, to).sure()
inline fun     IntArray.copyOfRange(from: Int, to: Int) = Arrays.copyOfRange(this, from, to).sure()
inline fun    LongArray.copyOfRange(from: Int, to: Int) = Arrays.copyOfRange(this, from, to).sure()
inline fun   FloatArray.copyOfRange(from: Int, to: Int) = Arrays.copyOfRange(this, from, to).sure()
inline fun  DoubleArray.copyOfRange(from: Int, to: Int) = Arrays.copyOfRange(this, from, to).sure()
inline fun    CharArray.copyOfRange(from: Int, to: Int) = Arrays.copyOfRange(this, from, to).sure()

inline fun  <T> Array<T>.copyOfRange(from: Int, to: Int) : Array<T> = Arrays.copyOfRange(this, from, to).sure()

inline val ByteArray.inputStream : ByteArrayInputStream
    get() = ByteArrayInputStream(this)

inline fun ByteArray.inputStream(offset: Int, length: Int) = ByteArrayInputStream(this, offset, length)

inline fun ByteArray.toString(encoding: String?): String {
    if (encoding != null) {
        return String(this, encoding)
    } else {
        return String(this)
    }
}

inline fun ByteArray.toString(encoding: Charset) = String(this, encoding)

/** Returns true if the array is not empty */
inline fun <T> Array<T>.notEmpty() : Boolean = !this.isEmpty()

/** Returns true if the array is empty */
inline fun <T> Array<T>.isEmpty() : Boolean = this.size == 0

/** Returns the array if its not null or else returns an empty array */
inline fun <T> Array<T>?.orEmpty() : Array<T> = if (this != null) this else array<T>()

