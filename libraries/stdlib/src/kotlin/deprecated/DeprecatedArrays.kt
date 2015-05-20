package kotlin

// deprecated to be removed after M12

deprecated("Use arrayOf() instead.", ReplaceWith("arrayOf(*t)"))
inline public fun <reified T> array(vararg t : T) : Array<T> = arrayOf(*t)

suppress("NOTHING_TO_INLINE")
deprecated("Use doubleArrayOf() instead.", ReplaceWith("doubleArrayOf(*content)"))
inline public fun doubleArray(vararg content : Double) : DoubleArray    = doubleArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use floatArrayOf() instead.", ReplaceWith("floatArrayOf(*content)"))
inline public fun floatArray(vararg content : Float) : FloatArray       = floatArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use longArrayOf() instead.", ReplaceWith("longArrayOf(*content)"))
inline public fun longArray(vararg content : Long) : LongArray          = longArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use intArrayOf() instead.", ReplaceWith("intArrayOf(*content)"))
inline public fun intArray(vararg content : Int) : IntArray             = intArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use charArrayOf() instead.", ReplaceWith("charArrayOf(*content)"))
inline public fun charArray(vararg content : Char) : CharArray          = charArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use shortArrayOf() instead.", ReplaceWith("shortArrayOf(*content)"))
inline public fun shortArray(vararg content : Short) : ShortArray       = shortArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use byteArrayOf() instead.", ReplaceWith("byteArrayOf(*content)"))
inline public fun byteArray(vararg content : Byte) : ByteArray          = byteArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use booleanArrayOf() instead.", ReplaceWith("booleanArrayOf(*content)"))
inline public fun booleanArray(vararg content : Boolean) : BooleanArray = booleanArrayOf(*content)

deprecated("Use toTypedArray() instead.", ReplaceWith("toTypedArray()"))
inline public fun <reified T> Collection<T>.copyToArray(): Array<T> = toTypedArray()