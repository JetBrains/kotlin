/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.ranges

/**
 * Represents a range of values (for example, numbers or characters).
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/ranges.html) for more information.
 */
public interface ClosedRange<T: Comparable<T>> {
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
     */
    public operator fun contains(value: T): Boolean = value >= start && value <= endInclusive

    /**
     * Checks whether the range is empty.

     * The range is empty if its start value is greater than the end value.
     */
    public fun isEmpty(): Boolean = start > endInclusive
}
