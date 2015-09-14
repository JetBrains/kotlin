package kotlin

// deprecated to be removed after M12

@Deprecated("Use arrayOf() instead.", ReplaceWith("arrayOf(*t)"))
inline public fun <reified T> array(vararg t : T) : Array<T> = arrayOf(*t)

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use doubleArrayOf() instead.", ReplaceWith("doubleArrayOf(*content)"))
inline public fun doubleArray(vararg content : Double) : DoubleArray    = doubleArrayOf(*content)

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use floatArrayOf() instead.", ReplaceWith("floatArrayOf(*content)"))
inline public fun floatArray(vararg content : Float) : FloatArray       = floatArrayOf(*content)

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use longArrayOf() instead.", ReplaceWith("longArrayOf(*content)"))
inline public fun longArray(vararg content : Long) : LongArray          = longArrayOf(*content)

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use intArrayOf() instead.", ReplaceWith("intArrayOf(*content)"))
inline public fun intArray(vararg content : Int) : IntArray             = intArrayOf(*content)

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use charArrayOf() instead.", ReplaceWith("charArrayOf(*content)"))
inline public fun charArray(vararg content : Char) : CharArray          = charArrayOf(*content)

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use shortArrayOf() instead.", ReplaceWith("shortArrayOf(*content)"))
inline public fun shortArray(vararg content : Short) : ShortArray       = shortArrayOf(*content)

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use byteArrayOf() instead.", ReplaceWith("byteArrayOf(*content)"))
inline public fun byteArray(vararg content : Byte) : ByteArray          = byteArrayOf(*content)

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use booleanArrayOf() instead.", ReplaceWith("booleanArrayOf(*content)"))
inline public fun booleanArray(vararg content : Boolean) : BooleanArray = booleanArrayOf(*content)

@Deprecated("Use toTypedArray() instead.", ReplaceWith("toTypedArray()"))
inline public fun <reified T> Collection<T>.copyToArray(): Array<T> = toTypedArray()