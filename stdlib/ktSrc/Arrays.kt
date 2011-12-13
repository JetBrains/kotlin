namespace std

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
