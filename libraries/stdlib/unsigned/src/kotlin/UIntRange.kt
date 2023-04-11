/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin.ranges



import kotlin.internal.*

/**
 * A range of values of type `UInt`.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
public class UIntRange(start: UInt, endInclusive: UInt) : UIntProgression(start, endInclusive, 1), ClosedRange<UInt>, OpenEndRange<UInt> {
    override val start: UInt get() = first
    override val endInclusive: UInt get() = last
    
    @Deprecated("Can throw an exception when it's impossible to represent the value with UInt type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
    @SinceKotlin("1.9")
    @WasExperimental(ExperimentalStdlibApi::class)
    override val endExclusive: UInt get() {
        if (last == UInt.MAX_VALUE) error("Cannot return the exclusive upper bound of a range that includes MAX_VALUE.")
        return last + 1u
    }

    override fun contains(value: UInt): Boolean = first <= value && value <= last

    /** 
     * Checks if the range is empty.
     
     * The range is empty if its start value is greater than the end value.
     */
    override fun isEmpty(): Boolean = first > last

    override fun equals(other: Any?): Boolean =
        other is UIntRange && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * first.toInt() + last.toInt())

    override fun toString(): String = "$first..$last"

    companion object {
        /** An empty range of values of type UInt. */
        public val EMPTY: UIntRange = UIntRange(UInt.MAX_VALUE, UInt.MIN_VALUE)
    }
}

/**
 * A progression of values of type `UInt`.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
public open class UIntProgression
internal constructor(
    start: UInt,
    endInclusive: UInt,
    step: Int
) : Iterable<UInt> {
    init {
        if (step == 0.toInt()) throw kotlin.IllegalArgumentException("Step must be non-zero.")
        if (step == Int.MIN_VALUE) throw kotlin.IllegalArgumentException("Step must be greater than Int.MIN_VALUE to avoid overflow on negation.")
    }

    /**
     * The first element in the progression.
     */
    public val first: UInt = start

    /**
     * The last element in the progression.
     */
    public val last: UInt = getProgressionLastElement(start, endInclusive, step)

    /**
     * The step of the progression.
     */
    public val step: Int = step

    final override fun iterator(): Iterator<UInt> = UIntProgressionIterator(first, last, step)

    /** 
     * Checks if the progression is empty.
     
     * Progression with a positive step is empty if its first element is greater than the last element.
     * Progression with a negative step is empty if its first element is less than the last element.
     */
    public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is UIntProgression && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * first.toInt() + last.toInt()) + step.toInt())

    override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

    companion object {
        /**
         * Creates UIntProgression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         *
         * [step] must be greater than `Int.MIN_VALUE` and not equal to zero.
         */
        public fun fromClosedRange(rangeStart: UInt, rangeEnd: UInt, step: Int): UIntProgression = UIntProgression(rangeStart, rangeEnd, step)
    }
}


/**
 * An iterator over a progression of values of type `UInt`.
 * @property step the number by which the value is incremented on each step.
 */
@SinceKotlin("1.3")
private class UIntProgressionIterator(first: UInt, last: UInt, step: Int) : Iterator<UInt> {
    private val finalElement = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
    private val step = step.toUInt() // use 2-complement math for negative steps
    private var next = if (hasNext) first else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun next(): UInt {
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

