package kotlin

/** Returns true if the array is not empty */
public inline fun <T> Array<T>.isNotEmpty() : Boolean = !this.isEmpty()

/** Returns true if the array is empty */
public inline fun <T> Array<T>.isEmpty() : Boolean = this.size == 0

/** Returns the array if its not null or else returns an empty array */
public inline fun <T> Array<out T>?.orEmpty() : Array<out T> = if (this != null) this else array<T>()

public inline val          BooleanArray.lastIndex : Int
    get() = this.size - 1

public inline val             ByteArray.lastIndex : Int
    get() = this.size - 1

public inline val            ShortArray.lastIndex : Int
    get() = this.size - 1

public inline val              IntArray.lastIndex : Int
    get() = this.size - 1

public inline val             LongArray.lastIndex : Int
    get() = this.size - 1

public inline val            FloatArray.lastIndex : Int
    get() = this.size - 1

public inline val           DoubleArray.lastIndex : Int
    get() = this.size - 1

public inline val             CharArray.lastIndex : Int
    get() = this.size - 1

public inline val <T: Any?> Array<T>.lastIndex : Int
    get() = this.size - 1
