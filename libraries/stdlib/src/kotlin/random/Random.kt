/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

/**
 * An abstract class that is implemented by random number generator algorithms.
 *
 * The companion object [Random.Default] is the default instance of [Random].
 *
 * To get a seeded instance of random generator use [Random] function.
 *
 * @sample samples.random.Randoms.defaultRandom
 */
@SinceKotlin("1.3")
public expect abstract class Random constructor() {

    /**
     * Gets the next random [bitCount] number of bits.
     *
     * Generates an `Int` whose lower [bitCount] bits are filled with random values and the remaining upper bits are zero.
     *
     * @param bitCount number of bits to generate, must be in range 0..32, otherwise the behavior is unspecified.
     *
     * @sample samples.random.Randoms.nextBits
     */
    public abstract fun nextBits(bitCount: Int): Int

    /**
     * Gets the next random `Int` from the random number generator.
     *
     * Generates an `Int` random value uniformly distributed between `Int.MIN_VALUE` and `Int.MAX_VALUE` (inclusive).
     *
     * @sample samples.random.Randoms.nextInt
     */
    public open fun nextInt(): Int

    /**
     * Gets the next random non-negative `Int` from the random number generator less than the specified [until] bound.
     *
     * Generates an `Int` random value uniformly distributed between `0` (inclusive) and the specified [until] bound (exclusive).
     *
     * @param until must be positive.
     *
     * @throws IllegalArgumentException if [until] is negative or zero.
     *
     * @sample samples.random.Randoms.nextIntFromUntil
     */
    public open fun nextInt(until: Int): Int

    /**
     * Gets the next random `Int` from the random number generator in the specified range.
     *
     * Generates an `Int` random value uniformly distributed between the specified [from] (inclusive) and [until] (exclusive) bounds.
     *
     * @throws IllegalArgumentException if [from] is greater than or equal to [until].
     *
     * @sample samples.random.Randoms.nextIntFromUntil
     */
    public open fun nextInt(from: Int, until: Int): Int

    /**
     * Gets the next random `Long` from the random number generator.
     *
     * Generates a `Long` random value uniformly distributed between `Long.MIN_VALUE` and `Long.MAX_VALUE` (inclusive).
     *
     * @sample samples.random.Randoms.nextLong
     */
    public open fun nextLong(): Long

    /**
     * Gets the next random non-negative `Long` from the random number generator less than the specified [until] bound.
     *
     * Generates a `Long` random value uniformly distributed between `0` (inclusive) and the specified [until] bound (exclusive).
     *
     * @param until must be positive.
     *
     * @throws IllegalArgumentException if [until] is negative or zero.
     *
     * @sample samples.random.Randoms.nextLongFromUntil
     */
    public open fun nextLong(until: Long): Long

    /**
     * Gets the next random `Long` from the random number generator in the specified range.
     *
     * Generates a `Long` random value uniformly distributed between the specified [from] (inclusive) and [until] (exclusive) bounds.
     *
     * @throws IllegalArgumentException if [from] is greater than or equal to [until].
     *
     * @sample samples.random.Randoms.nextLongFromUntil
     */
    public open fun nextLong(from: Long, until: Long): Long

    /**
     * Gets the next random [Boolean] value.
     *
     * @sample samples.random.Randoms.nextBoolean
     */
    public open fun nextBoolean(): Boolean

    /**
     * Gets the next random [Double] value uniformly distributed between 0 (inclusive) and 1 (exclusive).
     *
     * @sample samples.random.Randoms.nextDouble
     */
    public open fun nextDouble(): Double

    /**
     * Gets the next random non-negative `Double` from the random number generator less than the specified [until] bound.
     *
     * Generates a `Double` random value uniformly distributed between 0 (inclusive) and [until] (exclusive).
     *
     * @throws IllegalArgumentException if [until] is negative or zero.
     *
     * @sample samples.random.Randoms.nextDoubleFromUntil
     */
    public open fun nextDouble(until: Double): Double

    /**
     * Gets the next random `Double` from the random number generator in the specified range.
     *
     * Generates a `Double` random value uniformly distributed between the specified [from] (inclusive) and [until] (exclusive) bounds.
     *
     * [from] and [until] must be finite otherwise the behavior is unspecified.
     *
     * @throws IllegalArgumentException if [from] is greater than or equal to [until].
     *
     * @sample samples.random.Randoms.nextDoubleFromUntil
     */
    public open fun nextDouble(from: Double, until: Double): Double

    /**
     * Gets the next random [Float] value uniformly distributed between 0 (inclusive) and 1 (exclusive).
     *
     * @sample samples.random.Randoms.nextFloat
     */
    public open fun nextFloat(): Float

    /**
     * Fills a subrange of the specified byte [array] starting from [fromIndex] inclusive and ending [toIndex] exclusive
     * with random bytes.
     *
     * @return [array] with the subrange filled with random bytes.
     *
     * @sample samples.random.Randoms.nextBytes
     */
    public open fun nextBytes(array: ByteArray, fromIndex: Int = 0, toIndex: Int = array.size): ByteArray

    /**
     * Fills the specified byte [array] with random bytes and returns it.
     *
     * @return [array] filled with random bytes.
     *
     * @sample samples.random.Randoms.nextBytes
     */
    public open fun nextBytes(array: ByteArray): ByteArray

    /**
     * Creates a byte array of the specified [size], filled with random bytes.
     *
     * @sample samples.random.Randoms.nextBytes
     */
    public open fun nextBytes(size: Int): ByteArray


    /**
     * The default random number generator.
     *
     * On JVM this generator is thread-safe, its methods can be invoked from multiple threads.
     *
     * @sample samples.random.Randoms.defaultRandom
     */
    companion object Default : Random {
        override fun nextBits(bitCount: Int): Int
        override fun nextInt(): Int
        override fun nextInt(until: Int): Int
        override fun nextInt(from: Int, until: Int): Int

        override fun nextLong(): Long
        override fun nextLong(until: Long): Long
        override fun nextLong(from: Long, until: Long): Long

        override fun nextBoolean(): Boolean

        override fun nextDouble(): Double
        override fun nextDouble(until: Double): Double
        override fun nextDouble(from: Double, until: Double): Double

        override fun nextFloat(): Float

        override fun nextBytes(array: ByteArray): ByteArray
        override fun nextBytes(size: Int): ByteArray
        override fun nextBytes(array: ByteArray, fromIndex: Int, toIndex: Int): ByteArray
    }
}

/**
 * Returns a repeatable random number generator seeded with the given [seed] `Int` value.
 *
 * Two generators with the same seed produce the same sequence of values within the same version of Kotlin runtime.
 *
 * *Note:* Future versions of Kotlin may change the algorithm of this seeded number generator so that it will return
 * a sequence of values different from the current one for a given seed.
 *
 * On JVM the returned generator is NOT thread-safe. Do not invoke it from multiple threads without proper synchronization.
 *
 * @sample samples.random.Randoms.seededRandom
 */
@SinceKotlin("1.3")
public fun Random(seed: Int): Random = XorWowRandom(seed, seed.shr(31))

/**
 * Returns a repeatable random number generator seeded with the given [seed] `Long` value.
 *
 * Two generators with the same seed produce the same sequence of values within the same version of Kotlin runtime.
 *
 * *Note:* Future versions of Kotlin may change the algorithm of this seeded number generator so that it will return
 * a sequence of values different from the current one for a given seed.
 *
 * On JVM the returned generator is NOT thread-safe. Do not invoke it from multiple threads without proper synchronization.
 *
 * @sample samples.random.Randoms.seededRandom
 */
@SinceKotlin("1.3")
public fun Random(seed: Long): Random = XorWowRandom(seed.toInt(), seed.shr(32).toInt())


/**
 * Gets the next random `Int` from the random number generator in the specified [range].
 *
 * Generates an `Int` random value uniformly distributed in the specified [range]:
 * from `range.start` inclusive to `range.endInclusive` inclusive.
 *
 * @throws IllegalArgumentException if [range] is empty.
 */
@SinceKotlin("1.3")
public fun Random.nextInt(range: IntRange): Int = when {
    range.isEmpty() -> throw IllegalArgumentException("Cannot get random in empty range: $range")
    range.last < Int.MAX_VALUE -> nextInt(range.first, range.last + 1)
    range.first > Int.MIN_VALUE -> nextInt(range.first - 1, range.last) + 1
    else -> nextInt()
}

/**
 * Gets the next random `Long` from the random number generator in the specified [range].
 *
 * Generates a `Long` random value uniformly distributed in the specified [range]:
 * from `range.start` inclusive to `range.endInclusive` inclusive.
 *
 * @throws IllegalArgumentException if [range] is empty.
 */
@SinceKotlin("1.3")
public fun Random.nextLong(range: LongRange): Long = when {
    range.isEmpty() -> throw IllegalArgumentException("Cannot get random in empty range: $range")
    range.last < Long.MAX_VALUE -> nextLong(range.first, range.last + 1)
    range.first > Long.MIN_VALUE -> nextLong(range.first - 1, range.last) + 1
    else -> nextLong()
}


@OptIn(ExperimentalStdlibApi::class)
internal fun fastLog2(value: Int): Int = 31 - value.countLeadingZeroBits()

/** Takes upper [bitCount] bits (0..32) from this number. */
internal fun Int.takeUpperBits(bitCount: Int): Int =
    this.ushr(32 - bitCount) and (-bitCount).shr(31)

internal fun checkRangeBounds(from: Int, until: Int) = require(until > from) { boundsErrorMessage(from, until) }
internal fun checkRangeBounds(from: Long, until: Long) = require(until > from) { boundsErrorMessage(from, until) }
internal fun checkRangeBounds(from: Double, until: Double) = require(until > from) { boundsErrorMessage(from, until) }

internal fun boundsErrorMessage(from: Any, until: Any) = "Random range is empty: [$from, $until)."
