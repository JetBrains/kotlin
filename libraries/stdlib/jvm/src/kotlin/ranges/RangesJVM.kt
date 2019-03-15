/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("RangesKt")

package kotlin.ranges

/**
 * A closed range of values of type `Float`.
 *
 * Numbers are compared with the ends of this range according to IEEE-754.
 */
private class ClosedFloatRange(
    start: Float,
    endInclusive: Float
) : ClosedFloatingPointRange<Float> {
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
 * Creates a range from this [Float] value to the specified [that] value.
 *
 * Numbers are compared with the ends of this range according to IEEE-754.
 * @sample samples.ranges.Ranges.rangeFromFloat
 */
@SinceKotlin("1.1")
public operator fun Float.rangeTo(that: Float): ClosedFloatingPointRange<Float> = ClosedFloatRange(this, that)

