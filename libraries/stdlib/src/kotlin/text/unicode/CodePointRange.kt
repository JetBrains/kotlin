/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.unicode

import kotlin.internal.getProgressionLastElement
import kotlin.random.Random

/**
 * A range of values of type [CodePoint].
 */
@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public class CodePointRange(start: CodePoint, endInclusive: CodePoint) : CodePointProgression(start, endInclusive, 1), ClosedRange<CodePoint>, OpenEndRange<CodePoint> {
    override val start: CodePoint get() = first
    override val endInclusive: CodePoint get() = last

    @Deprecated("Can throw an exception when it's impossible to represent the value with UInt type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
    override val endExclusive: CodePoint get() {
        if (last == CodePoint.MAX_VALUE) error("Cannot return the exclusive upper bound of a range that includes MAX_VALUE.")
        return last + 1
    }

    override fun contains(value: CodePoint): Boolean = first <= value && value <= last

    /**
     * Checks if the range is empty.

     * The range is empty if its start value is greater than the end value.
     */
    override fun isEmpty(): Boolean = first > last

    override fun equals(other: Any?): Boolean =
        other is CodePointRange && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * first.code + last.code)

    override fun toString(): String = "$first..$last"

    public companion object {
        /** An empty range of values of type UInt. */
        public val EMPTY: CodePointRange = CodePointRange(CodePoint.MAX_VALUE, CodePoint.MIN_VALUE)
    }
}


/**
 * A progression of values of type [CodePoint].
 */
@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public open class CodePointProgression
internal constructor(
    start: CodePoint,
    endInclusive: CodePoint,
    step: Int
) : Iterable<CodePoint> {
    init {
        if (step == 0.toInt()) throw kotlin.IllegalArgumentException("Step must be non-zero.")
        if (step == Int.MIN_VALUE) throw kotlin.IllegalArgumentException("Step must be greater than Int.MIN_VALUE to avoid overflow on negation.")
    }

    /**
     * The first element in the progression.
     */
    public val first: CodePoint = start

    /**
     * The last element in the progression.
     */
    public val last: CodePoint = getProgressionLastElement(start.code, endInclusive.code, step).toCodePoint()

    /**
     * The step of the progression.
     */
    public val step: Int = step

    final override fun iterator(): Iterator<CodePoint> = CodePointProgressionIterator(first, last, step)

    /**
     * Checks if the progression is empty.

     * Progression with a positive step is empty if its first element is greater than the last element.
     * Progression with a negative step is empty if its first element is less than the last element.
     */
    public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is CodePointProgression && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * first.code + last.code) + step)

    override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

    public companion object {
        /**
         * Creates UIntProgression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         *
         * [step] must be greater than `Int.MIN_VALUE` and not equal to zero.
         */
        public fun fromClosedRange(rangeStart: CodePoint, rangeEnd: CodePoint, step: Int): CodePointProgression = CodePointProgression(rangeStart, rangeEnd, step)
    }
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
private class CodePointProgressionIterator(first: CodePoint, last: CodePoint, step: Int) : Iterator<CodePoint> {
    private val finalElement = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
    private val step = step
    private var next = if (hasNext) first else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun next(): CodePoint {
        val value = next
        if (value == finalElement) {
            if (!hasNext) throw kotlin.NoSuchElementException()
            hasNext = false
        } else {
            next += step
        }
        return value
    }
}


/**
 * Returns a range from this value up to but excluding the specified [other] value.
 *
 * If the [other] value is less than or equal to `this` value, then the returned range is empty.
 */
@SinceKotlin("2.5")
@ExperimentalUnicodeApi
@kotlin.internal.InlineOnly
public inline infix fun CodePoint.until(to: CodePoint): CodePointRange = this..<to

/**
 * Returns a progression from this value down to and including the specified [to] value with the step -1.
 *
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public infix fun CodePoint.downTo(to: CodePoint): CodePointProgression {
    return CodePointProgression.fromClosedRange(this, to, -1)
}

/**
 * Returns a progression that goes over the same range with the given step.
 */
@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public infix fun CodePointProgression.step(step: Int): CodePointProgression {
    checkStepIsPositive(step > 0, step)
    return CodePointProgression.fromClosedRange(first, last, if (this.step > 0) step else -step)
}

/**
 * Checks if the specified [value] belongs to this range.
 */
@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public operator fun CodePointRange.contains(value: Char): Boolean {
    return value.code in first.code..last.code
}

/**
 * Returns `true` if this range contains the specified [element].
 *
 * Always returns `false` if the [element] is `null`.
 */
@SinceKotlin("2.5")
@ExperimentalUnicodeApi
@kotlin.internal.InlineOnly
public inline operator fun CodePointRange.contains(element: CodePoint?): Boolean {
    return element != null && contains(element)
}



/**
 * Returns a random element from this range.
 *
 * @throws NoSuchElementException if this range is empty.
 */
@SinceKotlin("2.5")
@ExperimentalUnicodeApi
@kotlin.internal.InlineOnly
public inline fun CodePointRange.random(): CodePoint {
    return random(Random)
}

/**
 * Returns a random element from this range using the specified source of randomness.
 *
 * @throws NoSuchElementException if this range is empty.
 */
@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public fun CodePointRange.random(random: Random): CodePoint {
    if (isEmpty()) throw NoSuchElementException("Range is empty")

    return random.nextInt(first.code, last.code + 1).toCodePoint()
}

/**
 * Returns a random element from this range, or `null` if this range is empty.
 */
@SinceKotlin("2.5")
@ExperimentalUnicodeApi
@kotlin.internal.InlineOnly
public inline fun CodePointRange.randomOrNull(): CodePoint? {
    return randomOrNull(Random)
}

/**
 * Returns a random element from this range using the specified source of randomness, or `null` if this range is empty.
 */
@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public fun CodePointRange.randomOrNull(random: Random): CodePoint? {
    if (isEmpty())
        return null
    return random(random)
}
