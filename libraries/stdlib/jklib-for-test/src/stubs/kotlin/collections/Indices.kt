package kotlin.collections

public val <T> Array<out T>.indices: kotlin.ranges.IntRange
    get() = kotlin.ranges.IntRange(0, size - 1)
