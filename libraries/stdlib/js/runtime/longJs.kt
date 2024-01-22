/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright 2009 The Closure Library Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

package kotlin

import kotlin.internal.InlineOnly

internal external fun BigInt(value: dynamic): BigInt

private external fun Number(value: BigInt): Int

internal external interface BigInt

@InlineOnly
@Suppress("UNUSED_PARAMETER")
internal inline fun shiftLeft(a: BigInt, b: BigInt): BigInt = js("a << b")

@InlineOnly
@Suppress("UNUSED_PARAMETER")
internal inline fun shiftRight(a: BigInt, b: BigInt): BigInt = js("a >> b")

@InlineOnly
@Suppress("UNUSED_PARAMETER")
internal inline fun minus(a: BigInt, b: BigInt): BigInt = js("a - b")

@InlineOnly
@Suppress("UNUSED_PARAMETER")
internal inline fun and(a: BigInt, b: BigInt): BigInt = js("a & b")

@InlineOnly
@Suppress("UNUSED_PARAMETER")
internal inline fun or(a: BigInt, b: BigInt): BigInt = js("a | b")

@InlineOnly
@Suppress("UNUSED_PARAMETER")
internal inline fun xor(a: BigInt, b: BigInt): BigInt = js("a ^ b")

@InlineOnly
@Suppress("UNUSED_PARAMETER")
internal inline fun inv(value: BigInt): BigInt = js("~value")

@InlineOnly
internal inline fun Long.toNumber(): dynamic = Number(value)

@InlineOnly
internal inline fun hashCode(l: Long): Int =
    low(l) xor high(l)

@InlineOnly
internal inline fun low(l: Long): Int = Number(and(l.value, TWO_PWR_32_MINUS_1))

@InlineOnly
internal inline fun high(l: Long): Int = Number(and(shiftRight(l.value, BigInt(32)), TWO_PWR_32_MINUS_1))

@InlineOnly
internal inline fun Long.toStringImpl(radix: Int): String = value.toString(radix)

@InlineOnly
internal inline fun Long.equalsLong(other: Long) = value === other.value

@InlineOnly
internal inline fun Long.compare(other: Long): Int = when {
    value === other.value -> 0
    value > other.value -> 1
    else -> -1
}

@InlineOnly
internal inline fun Long.add(other: Long): Long = Long(value + other.value)

@InlineOnly
internal inline fun Long.subtract(other: Long): Long = Long(value - other.value)

@InlineOnly
internal inline fun Long.multiply(other: Long): Long = Long(value * other.value)

@InlineOnly
internal inline fun Long.divide(other: Long): Long = when {
    other.value === BigInt(0) -> throw Exception("division by zero")
    else -> Long(value / other.value)
}

@InlineOnly
internal inline fun Long.modulo(other: Long) = Long(value % other.value)

@InlineOnly
internal inline fun Long.shiftLeft(numBits: Int): Long = Long(shiftLeft(value, BigInt(numBits)))

@InlineOnly
internal inline fun Long.shiftRight(numBits: Int): Long = Long(shiftRight(value, BigInt(numBits)))

@InlineOnly
internal inline fun Long.shiftRightUnsigned(numBits: Int): Long {
    val mask = TWO_PWR_64_MINUS_1
    val bigIntValue = and(value, mask)
    return Long(and(shiftRight(bigIntValue, BigInt(numBits)), mask))
}

/**
 * Returns a Long representing the given (32-bit) integer value.
 * @param {number} value The 32-bit integer in question.
 * @return {!Kotlin.Long} The corresponding Long value.
 */
// TODO: cache
@InlineOnly
internal inline fun fromInt(value: Int) = Long(BigInt(value))

/**
 * Converts this [Double] value to [Long].
 * The fractional part, if any, is rounded down towards zero.
 * Returns zero if this `Double` value is `NaN`, [Long.MIN_VALUE] if it's less than `Long.MIN_VALUE`,
 * [Long.MAX_VALUE] if it's bigger than `Long.MAX_VALUE`.
 */
@InlineOnly
internal inline fun fromNumber(value: Double): Long = Long(BigInt(value))

private val TWO_PWR_32_MINUS_1 = BigInt(0xFFFFFFFF)
private val TWO_PWR_64_MINUS_1 = minus(shiftLeft(BigInt(1), BigInt(64)), BigInt(1))