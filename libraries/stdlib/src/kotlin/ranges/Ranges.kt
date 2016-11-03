@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("RangesKt")
package kotlin.ranges

/**
 * Represents a range of [Comparable] values.
 */
private open class ComparableRange<T: Comparable<T>> (
        override val start: T,
        override val endInclusive: T
): ClosedRange<T> {

    override fun equals(other: Any?): Boolean {
        return other is ComparableRange<*> && (isEmpty() && other.isEmpty() ||
                start == other.start && endInclusive == other.endInclusive)
    }

    override fun hashCode(): Int {
        return if (isEmpty()) -1 else 31 * start.hashCode() + endInclusive.hashCode()
    }

    override fun toString(): String = "$start..$endInclusive"
}

private class DoubleRange (
        start: Double,
        endInclusive: Double
): ComparableRange<Double>(start, endInclusive) {
    override fun contains(value: Double): Boolean = value >= start && value <= endInclusive
    override fun isEmpty(): Boolean = start > endInclusive
}

private class FloatRange (
        start: Float,
        endInclusive: Float
): ComparableRange<Float>(start, endInclusive) {
    override fun contains(value: Float): Boolean = value >= start && value <= endInclusive
    override fun isEmpty(): Boolean = start > endInclusive
}

/**
 * Creates a range from this [Comparable] value to the specified [that] value. This value
 * needs to be smaller than [that] value, otherwise the returned range will be empty.
 */
public operator fun <T: Comparable<T>> T.rangeTo(that: T): ClosedRange<T> =
        @Suppress("UNCHECKED_CAST")
        when {
            this is Double && that is Double -> DoubleRange(this, that) as ClosedRange<T>
            this is Float && that is Float -> FloatRange(this, that) as ClosedRange<T>
            else -> ComparableRange(this, that)
        }


internal fun checkStepIsPositive(isPositive: Boolean, step: Number) {
    if (!isPositive) throw IllegalArgumentException("Step must be positive, was: $step.")
}
