/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.ranges

/**
 * Represents a range of values (for example, numbers or characters) where both the lower and upper bounds are included in the range.
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/ranges.html) for more information.
 */
public interface ClosedRange<T : Comparable<T>> {
    /**
     * The minimum value in the range.
     */
    public val start: T

    /**
     * The maximum value in the range (inclusive).
     */
    public val endInclusive: T

    /**
     * Checks whether the specified [value] belongs to the range.
     *
     * A value belongs to the closed range if it is greater than or equal to the [start] bound and less than or equal to the [endInclusive] bound.
     */
    public operator fun contains(value: T): Boolean = value >= start && value <= endInclusive

    /**
     * Checks whether the range is empty.
     *
     * The range is empty if its start value is greater than the end value.
     */
    public fun isEmpty(): Boolean = start > endInclusive
}

/**
 * Represents a range of values (for example, numbers or characters) where the upper bound is not included in the range.
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/ranges.html) for more information.
 */
@SinceKotlin("1.9")
@WasExperimental(ExperimentalStdlibApi::class)
public interface OpenEndRange<T : Comparable<T>> {
    /**
     * The minimum value in the range.
     */
    public val start: T

    /**
     * The maximum value in the range (exclusive).
     *
     * @throws IllegalStateException can be thrown if the exclusive end bound cannot be represented
     * with a value of type [T].
     */
    public val endExclusive: T

    /**
     * Checks whether the specified [value] belongs to the range.
     *
     * A value belongs to the open-ended range if it is greater than or equal to the [start] bound and strictly less than the [endExclusive] bound.
     */
    public operator fun contains(value: T): Boolean = value >= start && value < endExclusive

    /**
     * Checks whether the range is empty.
     *
     * The open-ended range is empty if its start value is greater than or equal to the end value.
     */
    public fun isEmpty(): Boolean = start >= endExclusive
}