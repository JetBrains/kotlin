/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.ranges

// FIXME: Use stdlib _Ranges.kt instead

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Int.downTo(to: Byte): IntProgression {
    return IntProgression.fromClosedRange(this, to.toInt(), -1)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Long.downTo(to: Byte): LongProgression {
    return LongProgression.fromClosedRange(this, to.toLong(), -1L)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Byte.downTo(to: Byte): IntProgression {
    return IntProgression.fromClosedRange(this.toInt(), to.toInt(), -1)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Short.downTo(to: Byte): IntProgression {
    return IntProgression.fromClosedRange(this.toInt(), to.toInt(), -1)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Char.downTo(to: Char): CharProgression {
    return CharProgression.fromClosedRange(this, to, -1)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Int.downTo(to: Int): IntProgression {
    return IntProgression.fromClosedRange(this, to, -1)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Long.downTo(to: Int): LongProgression {
    return LongProgression.fromClosedRange(this, to.toLong(), -1L)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Byte.downTo(to: Int): IntProgression {
    return IntProgression.fromClosedRange(this.toInt(), to, -1)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Short.downTo(to: Int): IntProgression {
    return IntProgression.fromClosedRange(this.toInt(), to, -1)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Int.downTo(to: Long): LongProgression {
    return LongProgression.fromClosedRange(this.toLong(), to, -1L)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Long.downTo(to: Long): LongProgression {
    return LongProgression.fromClosedRange(this, to, -1L)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Byte.downTo(to: Long): LongProgression {
    return LongProgression.fromClosedRange(this.toLong(), to, -1L)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Short.downTo(to: Long): LongProgression {
    return LongProgression.fromClosedRange(this.toLong(), to, -1L)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Int.downTo(to: Short): IntProgression {
    return IntProgression.fromClosedRange(this, to.toInt(), -1)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Long.downTo(to: Short): LongProgression {
    return LongProgression.fromClosedRange(this, to.toLong(), -1L)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Byte.downTo(to: Short): IntProgression {
    return IntProgression.fromClosedRange(this.toInt(), to.toInt(), -1)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
public infix fun Short.downTo(to: Short): IntProgression {
    return IntProgression.fromClosedRange(this.toInt(), to.toInt(), -1)
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 */
public infix fun Int.until(to: Byte): IntRange {
    return this .. (to.toInt() - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 */
public infix fun Long.until(to: Byte): LongRange {
    return this .. (to.toLong() - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 */
public infix fun Byte.until(to: Byte): IntRange {
    return this.toInt() .. (to.toInt() - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 */
public infix fun Short.until(to: Byte): IntRange {
    return this.toInt() .. (to.toInt() - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 * 
 * If the [to] value is less than or equal to `'\u0000'` the returned range is empty.
 */
public infix fun Char.until(to: Char): CharRange {
    if (to <= '\u0000') return CharRange.EMPTY
    return this .. (to - 1).toChar()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 * 
 * If the [to] value is less than or equal to [Int.MIN_VALUE] the returned range is empty.
 */
public infix fun Int.until(to: Int): IntRange {
    if (to <= Int.MIN_VALUE) return IntRange.EMPTY
    return this .. (to - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 */
public infix fun Long.until(to: Int): LongRange {
    return this .. (to.toLong() - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 * 
 * If the [to] value is less than or equal to [Int.MIN_VALUE] the returned range is empty.
 */
public infix fun Byte.until(to: Int): IntRange {
    if (to <= Int.MIN_VALUE) return IntRange.EMPTY
    return this.toInt() .. (to - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 * 
 * If the [to] value is less than or equal to [Int.MIN_VALUE] the returned range is empty.
 */
public infix fun Short.until(to: Int): IntRange {
    if (to <= Int.MIN_VALUE) return IntRange.EMPTY
    return this.toInt() .. (to - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 * 
 * If the [to] value is less than or equal to [Long.MIN_VALUE] the returned range is empty.
 */
public infix fun Int.until(to: Long): LongRange {
    if (to <= Long.MIN_VALUE) return LongRange.EMPTY
    return this.toLong() .. (to - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 * 
 * If the [to] value is less than or equal to [Long.MIN_VALUE] the returned range is empty.
 */
public infix fun Long.until(to: Long): LongRange {
    if (to <= Long.MIN_VALUE) return LongRange.EMPTY
    return this .. (to - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 * 
 * If the [to] value is less than or equal to [Long.MIN_VALUE] the returned range is empty.
 */
public infix fun Byte.until(to: Long): LongRange {
    if (to <= Long.MIN_VALUE) return LongRange.EMPTY
    return this.toLong() .. (to - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 * 
 * If the [to] value is less than or equal to [Long.MIN_VALUE] the returned range is empty.
 */
public infix fun Short.until(to: Long): LongRange {
    if (to <= Long.MIN_VALUE) return LongRange.EMPTY
    return this.toLong() .. (to - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 */
public infix fun Int.until(to: Short): IntRange {
    return this .. (to.toInt() - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 */
public infix fun Long.until(to: Short): LongRange {
    return this .. (to.toLong() - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 */
public infix fun Byte.until(to: Short): IntRange {
    return this.toInt() .. (to.toInt() - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 */
public infix fun Short.until(to: Short): IntRange {
    return this.toInt() .. (to.toInt() - 1).toInt()
}
