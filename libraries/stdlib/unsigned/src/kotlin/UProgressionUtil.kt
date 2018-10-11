/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:UseExperimental(ExperimentalUnsignedTypes::class)
package kotlin.internal

// (a - b) mod c
private fun differenceModulo(a: UInt, b: UInt, c: UInt): UInt {
    val ac = a % c
    val bc = b % c
    return if (ac >= bc) ac - bc else ac - bc + c
}

private fun differenceModulo(a: ULong, b: ULong, c: ULong): ULong {
    val ac = a % c
    val bc = b % c
    return if (ac >= bc) ac - bc else ac - bc + c
}

/**
 * Calculates the final element of a bounded arithmetic progression, i.e. the last element of the progression which is in the range
 * from [start] to [end] in case of a positive [step], or from [end] to [start] in case of a negative
 * [step].
 *
 * No validation on passed parameters is performed. The given parameters should satisfy the condition:
 *
 * - either `step > 0` and `start <= end`,
 * - or `step < 0` and `start >= end`.
 *
 * @param start first element of the progression
 * @param end ending bound for the progression
 * @param step increment, or difference of successive elements in the progression
 * @return the final element of the progression
 * @suppress
 */
@PublishedApi
@SinceKotlin("1.3")
internal fun getProgressionLastElement(start: UInt, end: UInt, step: Int): UInt = when {
    step > 0 -> if (start >= end) end else end - differenceModulo(end, start, step.toUInt())
    step < 0 -> if (start <= end) end else end + differenceModulo(start, end, (-step).toUInt())
    else -> throw kotlin.IllegalArgumentException("Step is zero.")
}

/**
 * Calculates the final element of a bounded arithmetic progression, i.e. the last element of the progression which is in the range
 * from [start] to [end] in case of a positive [step], or from [end] to [start] in case of a negative
 * [step].
 *
 * No validation on passed parameters is performed. The given parameters should satisfy the condition:
 *
 * - either `step > 0` and `start <= end`,
 * - or `step < 0` and `start >= end`.
 *
 * @param start first element of the progression
 * @param end ending bound for the progression
 * @param step increment, or difference of successive elements in the progression
 * @return the final element of the progression
 * @suppress
 */
@PublishedApi
@SinceKotlin("1.3")
internal fun getProgressionLastElement(start: ULong, end: ULong, step: Long): ULong = when {
    step > 0 -> if (start >= end) end else end - differenceModulo(end, start, step.toULong())
    step < 0 -> if (start <= end) end else end + differenceModulo(start, end, (-step).toULong())
    else -> throw kotlin.IllegalArgumentException("Step is zero.")
}
