package kotlin

public inline fun <reified T> Array(size: Int, init: (Int) -> T): Array<T> {
    val result = arrayOfNulls<T>(size)

    for (i in 0..size - 1) {
        result[i] = init(i)
    }

    return result as Array<T>
}

public val BooleanArray.lastIndex: Int
    get() = size() - 1

public val ByteArray.lastIndex: Int
    get() = size() - 1

public val ShortArray.lastIndex: Int
    get() = size() - 1

public val IntArray.lastIndex: Int
    get() = size() - 1

public val LongArray.lastIndex: Int
    get() = size() - 1

public val FloatArray.lastIndex: Int
    get() = size() - 1

public val DoubleArray.lastIndex: Int
    get() = size() - 1

public val CharArray.lastIndex: Int
    get() = size() - 1

public val Array<*>.lastIndex: Int
    get() = size() - 1


public val BooleanArray.indices: IntRange
    get() = IntRange(0, lastIndex)

public val ByteArray.indices: IntRange
    get() = IntRange(0, lastIndex)

public val ShortArray.indices: IntRange
    get() = IntRange(0, lastIndex)

public val IntArray.indices: IntRange
    get() = IntRange(0, lastIndex)

public val LongArray.indices: IntRange
    get() = IntRange(0, lastIndex)

public val FloatArray.indices: IntRange
    get() = IntRange(0, lastIndex)

public val DoubleArray.indices: IntRange
    get() = IntRange(0, lastIndex)

public val CharArray.indices: IntRange
    get() = IntRange(0, lastIndex)

public val Array<*>.indices: IntRange
    get() = IntRange(0, lastIndex)
