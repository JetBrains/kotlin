package kotlin

public class ComparableRange<T: Comparable<T>> (
        override val start: T,
        override val end: T
): Range<T> {
    override fun contains(item: T): Boolean {
        return start <= item && item <= end
    }

    override fun equals(other: Any?): Boolean {
        return other is ComparableRange<*> && (isEmpty() && other.isEmpty() ||
                start == other.start && end == other.end)
    }

    override fun hashCode(): Int {
        return if (isEmpty()) -1 else 31 * start.hashCode() + end.hashCode()
    }
}

public fun <T: Comparable<T>> T.rangeTo(that: T): ComparableRange<T> {
    return ComparableRange(this, that)
}


private fun checkStepIsPositive(isPositive: Boolean, step: Number) {
    if (!isPositive) throw IllegalArgumentException("Step must be positive, was: $step")
}
