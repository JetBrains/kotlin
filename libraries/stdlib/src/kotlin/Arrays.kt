package kotlin

/** Returns true if the array is not empty */
public fun <T> Array<out T>.isNotEmpty() : Boolean = !this.isEmpty()

/** Returns true if the array is empty */
public fun <T> Array<out T>.isEmpty() : Boolean = this.size == 0

public val          BooleanArray.lastIndex : Int
    get() = this.size - 1

public val             ByteArray.lastIndex : Int
    get() = this.size - 1

public val            ShortArray.lastIndex : Int
    get() = this.size - 1

public val              IntArray.lastIndex : Int
    get() = this.size - 1

public val             LongArray.lastIndex : Int
    get() = this.size - 1

public val            FloatArray.lastIndex : Int
    get() = this.size - 1

public val           DoubleArray.lastIndex : Int
    get() = this.size - 1

public val             CharArray.lastIndex : Int
    get() = this.size - 1

public val <T: Any?> Array<out T>.lastIndex : Int
    get() = this.size - 1

// "Iterable-getters"
public fun <T> Array<T>.slice(indexes: Iterable<Int>): List<T>{
    val result = listBuilder<T>()
    for(i in indexes){
        result.add(get(i))
    }
    return result.build()
}
public fun    ByteArray.slice(indexes: Iterable<Int>): List<Byte>{
    val result = listBuilder<Byte>()
    for(i in indexes){
        result.add(get(i))
    }
    return result.build()
}
public fun   ShortArray.slice(indexes: Iterable<Int>): List<Short>{
    val result = listBuilder<Short>()
    for(i in indexes){
        result.add(get(i))
    }
    return result.build()
}
public fun     IntArray.slice(indexes: Iterable<Int>): List<Int>{
    val result = listBuilder<Int>()
    for(i in indexes){
        result.add(get(i))
    }
    return result.build()
}
public fun    LongArray.slice(indexes: Iterable<Int>): List<Long>{
    val result = listBuilder<Long>()
    for(i in indexes){
        result.add(get(i))
    }
    return result.build()
}
public fun   FloatArray.slice(indexes: Iterable<Int>): List<Float>{
    val result = listBuilder<Float>()
    for(i in indexes){
        result.add(get(i))
    }
    return result.build()
}
public fun  DoubleArray.slice(indexes: Iterable<Int>): List<Double>{
    val result = listBuilder<Double>()
    for(i in indexes){
        result.add(get(i))
    }
    return result.build()
}
public fun BooleanArray.slice(indexes: Iterable<Int>): List<Boolean>{
    val result = listBuilder<Boolean>()
    for(i in indexes){
        result.add(get(i))
    }
    return result.build()
}

