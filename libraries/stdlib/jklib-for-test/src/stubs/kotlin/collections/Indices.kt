package kotlin.collections

public val <T> Array<out T>.indices: kotlin.ranges.IntRange
    get() = kotlin.ranges.IntRange(0, size - 1)

@Suppress("UNCHECKED_CAST")
public fun <T> Array<out T>.asList(): List<T> {
    return ArraysUtilJVM.asList(this as Array<Any?>) as List<T>
}

public fun <T> List<T>.getOrNull(index: Int): T? {
    return if (index >= 0 && index < size) this[index] else null
}
