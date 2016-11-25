@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("RangesKt")
package kotlin.ranges

public interface ClosedComparableRange<T: Comparable<T>> : ClosedRange<T> {
    override fun contains(value: T): Boolean = lessThanOrEquals(value, start) && lessThanOrEquals(value, endInclusive)
    override fun isEmpty(): Boolean = !lessThanOrEquals(start, endInclusive)

    fun lessThanOrEquals(a: T, b: T): Boolean
}

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

/**
 * A closed range of values of type `Double`.
 *
 * Numbers are compared with the ends of this range according to IEEE-754.
 */
private class ClosedDoubleRange (
        start: Double,
        endInclusive: Double
) : ClosedComparableRange<Double> {
    private val _start = start
    private val _endInclusive = endInclusive
    override val start: Double get() = _start
    override val endInclusive: Double get() = _endInclusive

    override fun lessThanOrEquals(a: Double, b: Double): Boolean = a <= b

    override fun contains(value: Double): Boolean = value >= _start && value <= _endInclusive
    override fun isEmpty(): Boolean = !(_start <= _endInclusive)

    override fun equals(other: Any?): Boolean {
        return other is ClosedDoubleRange && (isEmpty() && other.isEmpty() ||
                _start == other._start && _endInclusive == other._endInclusive)
    }

    override fun hashCode(): Int {
        return if (isEmpty()) -1 else 31 * _start.hashCode() + _endInclusive.hashCode()
    }
    override fun toString(): String = "$_start..$_endInclusive"
}

/**
 * A closed range of values of type `Float`.
 *
 * Numbers are compared with the ends of this range according to IEEE-754.

 */
@JvmVersion
private class ClosedFloatRange (
        start: Float,
        endInclusive: Float
): ClosedComparableRange<Float> {
    private val _start = start
    private val _endInclusive = endInclusive
    override val start: Float get() = _start
    override val endInclusive: Float get() = _endInclusive

    override fun lessThanOrEquals(a: Float, b: Float): Boolean = a <= b

    override fun contains(value: Float): Boolean = value >= _start && value <= _endInclusive
    override fun isEmpty(): Boolean = !(_start <= _endInclusive)

    override fun equals(other: Any?): Boolean {
        return other is ClosedFloatRange && (isEmpty() && other.isEmpty() ||
                _start == other._start && _endInclusive == other._endInclusive)
    }

    override fun hashCode(): Int {
        return if (isEmpty()) -1 else 31 * _start.hashCode() + _endInclusive.hashCode()
    }
    override fun toString(): String = "$_start..$_endInclusive"
}

/**
 * Creates a range from this [Comparable] value to the specified [that] value.
 *
 * This value needs to be smaller than [that] value, otherwise the returned range will be empty.
 */
public operator fun <T: Comparable<T>> T.rangeTo(that: T): ClosedRange<T> = ComparableRange(this, that)

/**
 * Creates a range from this [Double] value to the specified [other] value.
 */
@SinceKotlin("1.1")
public operator fun Double.rangeTo(that: Double): ClosedComparableRange<Double> = ClosedDoubleRange(this, that)

/**
 * Creates a range from this [Float] value to the specified [other] value.
 */
@JvmVersion
@SinceKotlin("1.1")
public operator fun Float.rangeTo(that: Float): ClosedComparableRange<Float> = ClosedFloatRange(this, that)


internal fun checkStepIsPositive(isPositive: Boolean, step: Number) {
    if (!isPositive) throw IllegalArgumentException("Step must be positive, was: $step.")
}
