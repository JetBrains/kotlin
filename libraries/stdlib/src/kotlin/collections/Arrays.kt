package kotlin

public val BooleanArray.lastIndex: Int
    get() = this.size - 1

public val ByteArray.lastIndex: Int
    get() = this.size - 1

public val ShortArray.lastIndex: Int
    get() = this.size - 1

public val IntArray.lastIndex: Int
    get() = this.size - 1

public val LongArray.lastIndex: Int
    get() = this.size - 1

public val FloatArray.lastIndex: Int
    get() = this.size - 1

public val DoubleArray.lastIndex: Int
    get() = this.size - 1

public val CharArray.lastIndex: Int
    get() = this.size - 1

public val Array<*>.lastIndex: Int
    get() = this.size - 1
