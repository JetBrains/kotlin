package kotlin

// deprecated to be removed after M12

deprecated("Use arrayOf() instead.")
inline public fun <reified T> array(vararg t : T) : Array<T> = arrayOf(*t)

suppress("NOTHING_TO_INLINE")
deprecated("Use doubleArrayOf() instead.")
inline public fun doubleArray(vararg content : Double) : DoubleArray    = doubleArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use floatArrayOf() instead.")
inline public fun floatArray(vararg content : Float) : FloatArray       = floatArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use longArrayOf() instead.")
inline public fun longArray(vararg content : Long) : LongArray          = longArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use intArrayOf() instead.")
inline public fun intArray(vararg content : Int) : IntArray             = intArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use charArrayOf() instead.")
inline public fun charArray(vararg content : Char) : CharArray          = charArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use shortArrayOf() instead.")
inline public fun shortArray(vararg content : Short) : ShortArray       = shortArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use byteArrayOf() instead.")
inline public fun byteArray(vararg content : Byte) : ByteArray          = byteArrayOf(*content)

suppress("NOTHING_TO_INLINE")
deprecated("Use booleanArrayOf() instead.")
inline public fun booleanArray(vararg content : Boolean) : BooleanArray = booleanArrayOf(*content)

deprecated("Use toTypedArray() instead.")
inline public fun <reified T> Collection<T>.copyToArray(): Array<T> = toTypedArray()