/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.random


/**
 * Gets the next random [UInt] from the random number generator.
 *
 * Generates a [UInt] random value uniformly distributed between [UInt.MIN_VALUE] and [UInt.MAX_VALUE] (inclusive).
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextUInt(): UInt = nextInt().toUInt()

/**
 * Gets the next random [UInt] from the random number generator less than the specified [until] bound.
 *
 * Generates a [UInt] random value uniformly distributed between `0` (inclusive) and the specified [until] bound (exclusive).
 *
 * @throws IllegalArgumentException if [until] is zero.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextUInt(until: UInt): UInt = nextUInt(0u, until)

/**
 * Gets the next random [UInt] from the random number generator in the specified range.
 *
 * Generates a [UInt] random value uniformly distributed between the specified [from] (inclusive) and [until] (exclusive) bounds.
 *
 * @throws IllegalArgumentException if [from] is greater than or equal to [until].
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextUInt(from: UInt, until: UInt): UInt {
    checkUIntRangeBounds(from, until)

    val signedFrom = from.toInt() xor Int.MIN_VALUE
    val signedUntil = until.toInt() xor Int.MIN_VALUE

    val signedResult = nextInt(signedFrom, signedUntil) xor Int.MIN_VALUE
    return signedResult.toUInt()
}

/**
 * Gets the next random [UInt] from the random number generator in the specified [range].
 *
 * Generates a [UInt] random value uniformly distributed in the specified [range]:
 * from `range.start` inclusive to `range.endInclusive` inclusive.
 *
 * @throws IllegalArgumentException if [range] is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextUInt(range: UIntRange): UInt = when {
    range.isEmpty() -> throw IllegalArgumentException("Cannot get random in empty range: $range")
    range.last < UInt.MAX_VALUE -> nextUInt(range.first, range.last + 1u)
    range.first > UInt.MIN_VALUE -> nextUInt(range.first - 1u, range.last) + 1u
    else -> nextUInt()
}

/**
 * Gets the next random [ULong] from the random number generator.
 *
 * Generates a [ULong] random value uniformly distributed between [ULong.MIN_VALUE] and [ULong.MAX_VALUE] (inclusive).
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextULong(): ULong = nextLong().toULong()

/**
 * Gets the next random [ULong] from the random number generator less than the specified [until] bound.
 *
 * Generates a [ULong] random value uniformly distributed between `0` (inclusive) and the specified [until] bound (exclusive).
 *
 * @throws IllegalArgumentException if [until] is zero.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextULong(until: ULong): ULong = nextULong(0uL, until)

/**
 * Gets the next random [ULong] from the random number generator in the specified range.
 *
 * Generates a [ULong] random value uniformly distributed between the specified [from] (inclusive) and [until] (exclusive) bounds.
 *
 * @throws IllegalArgumentException if [from] is greater than or equal to [until].
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextULong(from: ULong, until: ULong): ULong {
    checkULongRangeBounds(from, until)

    val signedFrom = from.toLong() xor Long.MIN_VALUE
    val signedUntil = until.toLong() xor Long.MIN_VALUE

    val signedResult = nextLong(signedFrom, signedUntil) xor Long.MIN_VALUE
    return signedResult.toULong()
}

/**
 * Gets the next random [ULong] from the random number generator in the specified [range].
 *
 * Generates a [ULong] random value uniformly distributed in the specified [range]:
 * from `range.start` inclusive to `range.endInclusive` inclusive.
 *
 * @throws IllegalArgumentException if [range] is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextULong(range: ULongRange): ULong = when {
    range.isEmpty() -> throw IllegalArgumentException("Cannot get random in empty range: $range")
    range.last < ULong.MAX_VALUE -> nextULong(range.first, range.last + 1u)
    range.first > ULong.MIN_VALUE -> nextULong(range.first - 1u, range.last) + 1u
    else -> nextULong()
}

/**
 * Fills the specified unsigned byte [array] with random bytes and returns it.
 *
 * @return [array] filled with random bytes.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextUBytes(array: UByteArray): UByteArray {
    nextBytes(array.asByteArray())
    return array
}

/**
 * Creates an unsigned byte array of the specified [size], filled with random bytes.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextUBytes(size: Int): UByteArray = nextBytes(size).asUByteArray()

/**
 * Fills a subrange of the specified `UByte` [array] starting from [fromIndex] inclusive and ending [toIndex] exclusive with random UBytes.
 *
 * @return [array] with the subrange filled with random bytes.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextUBytes(array: UByteArray, fromIndex: Int = 0, toIndex: Int = array.size): UByteArray {
    nextBytes(array.asByteArray(), fromIndex, toIndex)
    return array
}


@ExperimentalUnsignedTypes
internal fun checkUIntRangeBounds(from: UInt, until: UInt) = require(until > from) { boundsErrorMessage(from, until) }
@ExperimentalUnsignedTypes
internal fun checkULongRangeBounds(from: ULong, until: ULong) = require(until > from) { boundsErrorMessage(from, until) }
