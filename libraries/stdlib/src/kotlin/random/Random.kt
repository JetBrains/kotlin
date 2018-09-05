/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

import kotlin.*
import kotlin.math.nextDown

/**
 * An abstract class that is implemented by random number generator algorithms.
 *
 * The companion object [Random.Companion] is the default instance of [Random].
 *
 * To get a seeded instance of random generator use [Random] function.
 *
 * @sample samples.random.Randoms.defaultRandom
 */
@SinceKotlin("1.3")
public abstract class Random {

    /**
     * Gets the next random [bitCount] number of bits.
     *
     * Generates an `Int` whose lower [bitCount] bits are filled with random values and the remaining upper bits are zero.
     *
     * @param bitCount number of bits to generate, must be in range 0..32, otherwise the behavior is unspecified.
     */
    public abstract fun nextBits(bitCount: Int): Int

    /**
     * Gets the next random `Int` from the random number generator.
     *
     * Generates an `Int` random value uniformly distributed between `Int.MIN_VALUE` and `Int.MAX_VALUE` (inclusive).
     */
    public open fun nextInt(): Int = nextBits(32)

    /**
     * Gets the next random non-negative `Int` from the random number generator not greater than the specified [bound].
     *
     * Generates an `Int` random value uniformly distributed between `0` (inclusive) and the specified [bound] (exclusive).
     *
     * @param bound must be positive.
     *
     * @throws IllegalArgumentException if [bound] is negative or zero.
     */
    public open fun nextInt(bound: Int): Int = nextInt(0, bound)

    /**
     * Gets the next random `Int` from the random number generator in the specified range.
     *
     * Generates an `Int` random value uniformly distributed between the specified [origin] (inclusive) and the specified [bound] (exclusive).
     *
     * @throws IllegalArgumentException if [origin] is greater than or equal to [bound].
     */
    public open fun nextInt(origin: Int, bound: Int): Int {
        checkRangeBounds(origin, bound)
        val n = bound - origin
        if (n > 0 || n == Int.MIN_VALUE) {
            val rnd = if (n and -n == n) {
                val bitCount = fastLog2(n)
                nextBits(bitCount)
            } else {
                var v: Int
                do {
                    val bits = nextInt().ushr(1)
                    v = bits % n
                } while (bits - v + (n - 1) < 0)
                v
            }
            return origin + rnd
        } else {
            while (true) {
                val rnd = nextInt()
                if (rnd in origin until bound) return rnd
            }
        }
    }

    /**
     * Gets the next random `Int` from the random number generator in the specified [range].
     *
     * Generates an `Int` random value uniformly distributed in the specified [range]:
     * from `range.start` inclusive to `range.endInclusive` inclusive.
     *
     * @throws IllegalArgumentException if [range] is empty.
     */
    public open fun nextInt(range: IntRange): Int = when {
        range.isEmpty() -> throw IllegalArgumentException("Cannot get random in empty range: $range")
        range.last < Int.MAX_VALUE -> nextInt(range.first, range.last + 1)
        range.first > Int.MIN_VALUE -> nextInt(range.first - 1, range.last) + 1
        else -> nextInt()
    }

    /**
     * Gets the next random `Long` from the random number generator.
     *
     * Generates a `Long` random value uniformly distributed between `Long.MIN_VALUE` and `Long.MAX_VALUE` (inclusive).
     */
    public open fun nextLong(): Long = nextInt().toLong().shl(32) + nextInt()

    /**
     * Gets the next random non-negative `Long` from the random number generator not greater than the specified [bound].
     *
     * Generates a `Long` random value uniformly distributed between `0` (inclusive) and the specified [bound] (exclusive).
     *
     * @param bound must be positive.
     *
     * @throws IllegalArgumentException if [bound] is negative or zero.
     */
    public open fun nextLong(bound: Long): Long = nextLong(0, bound)

    /**
     * Gets the next random `Long` from the random number generator in the specified range.
     *
     * Generates a `Long` random value uniformly distributed between the specified [origin] (inclusive) and the specified [bound] (exclusive).
     *
     * @throws IllegalArgumentException if [origin] is greater than or equal to [bound].
     */
    public open fun nextLong(origin: Long, bound: Long): Long {
        checkRangeBounds(origin, bound)
        val n = bound - origin
        if (n > 0) {
            val rnd: Long
            if (n and -n == n) {
                val nLow = n.toInt()
                val nHigh = (n ushr 32).toInt()
                rnd = when {
                    nLow != 0 -> {
                        val bitCount = fastLog2(nLow)
                        // toUInt().toLong()
                        nextBits(bitCount).toLong() and 0xFFFF_FFFF
                    }
                    nHigh == 1 ->
                        // toUInt().toLong()
                        nextInt().toLong() and 0xFFFF_FFFF
                    else -> {
                        val bitCount = fastLog2(nHigh)
                        nextBits(bitCount).toLong().shl(32) + nextInt()
                    }
                }
            } else {
                var v: Long
                do {
                    val bits = nextLong().ushr(1)
                    v = bits % n
                } while (bits - v + (n - 1) < 0)
                rnd = v
            }
            return origin + rnd
        } else {
            while (true) {
                val rnd = nextLong()
                if (rnd in origin until bound) return rnd
            }
        }
    }

    /**
     * Gets the next random `Long` from the random number generator in the specified [range].
     *
     * Generates a `Long` random value uniformly distributed in the specified [range]:
     * from `range.start` inclusive to `range.endInclusive` inclusive.
     *
     * @throws IllegalArgumentException if [range] is empty.
     */
    public open fun nextLong(range: LongRange): Long = when {
        range.isEmpty() -> throw IllegalArgumentException("Cannot get random in empty range: $range")
        range.last < Long.MAX_VALUE -> nextLong(range.start, range.endInclusive + 1)
        range.start > Long.MIN_VALUE -> nextLong(range.start - 1, range.endInclusive) + 1
        else -> nextLong()
    }

    /**
     * Gets the next random [Boolean] value.
     */
    public open fun nextBoolean(): Boolean = nextBits(1) != 0

    /**
     * Gets the next random [Double] value uniformly distributed between 0 (inclusive) and 1 (exclusive).
     */
    public open fun nextDouble(): Double = doubleFromParts(nextBits(26), nextBits(27))

    /**
     * Gets the next random non-negative `Double` from the random number generator not greater than the specified [bound].
     *
     * Generates a `Double` random value uniformly distributed between 0 (inclusive) and [bound] (exclusive).
     *
     * @throws IllegalArgumentException if [bound] is negative or zero.
     */
    public open fun nextDouble(bound: Double): Double = nextDouble(0.0, bound)

    /**
     * Gets the next random `Double` from the random number generator in the specified range.
     *
     * Generates a `Double` random value uniformly distributed between the specified [origin] (inclusive) and the specified [bound] (exclusive).
     *
     * [origin] and [bound] must be finite otherwise the behavior is unspecified.
     *
     * @throws IllegalArgumentException if [origin] is greater than or equal to [bound].
     */
    public open fun nextDouble(origin: Double, bound: Double): Double {
        checkRangeBounds(origin, bound)
        val size = bound - origin
        val r = if (size.isInfinite() && origin.isFinite() && bound.isFinite()) {
            val r1 = nextDouble() * (bound / 2 - origin / 2)
            origin + r1 + r1
        } else {
            origin + nextDouble() * size
        }
        return if (r >= bound) bound.nextDown() else r
    }

    /**
     * Gets the next random [Float] value uniformly distributed between 0 (inclusive) and 1 (exclusive).
     */
    public open fun nextFloat(): Float = nextBits(24) / (1 shl 24).toFloat()

    /**
     * Fills a subrange of the specified byte [array] starting from [fromIndex] inclusive and ending [toIndex] exclusive
     * with random bytes.
     *
     * @return [array] with the subrange filled with random bytes.
     */
    public open fun nextBytes(array: ByteArray, fromIndex: Int = 0, toIndex: Int = array.size): ByteArray {
        require(fromIndex in 0..array.size && toIndex in 0..array.size) { "fromIndex ($fromIndex) or toIndex ($toIndex) are out of range: 0..${array.size}." }
        require(fromIndex <= toIndex) { "fromIndex ($fromIndex) must be not greater than toIndex ($toIndex)." }

        val steps = (toIndex - fromIndex) / 4

        var position = fromIndex
        repeat(steps) {
            val v = nextInt()
            array[position] = v.toByte()
            array[position + 1] = v.ushr(8).toByte()
            array[position + 2] = v.ushr(16).toByte()
            array[position + 3] = v.ushr(24).toByte()
            position += 4
        }

        val remainder = toIndex - position
        val vr = nextBits(remainder * 8)
        for (i in 0 until remainder) {
            array[position + i] = vr.ushr(i * 8).toByte()
        }

        return array
    }

    /**
     * Fills the specified byte [array] with random bytes and returns it.
     *
     * @return [array] filled with random bytes.
     */
    public open fun nextBytes(array: ByteArray): ByteArray = nextBytes(array, 0, array.size)

    /**
     * Creates a byte array of the specified [size], filled with random bytes.
     */
    public open fun nextBytes(size: Int): ByteArray = nextBytes(ByteArray(size))


    /**
     * The default random number generator.
     *
     * On JVM this generator is thread-safe, its methods can be invoked from multiple threads.
     *
     * @sample samples.random.Randoms.defaultRandom
     */
    companion object : Random() {

        private val defaultRandom: Random = defaultPlatformRandom()

        override fun nextBits(bitCount: Int): Int = defaultRandom.nextBits(bitCount)
        override fun nextInt(): Int = defaultRandom.nextInt()
        override fun nextInt(bound: Int): Int = defaultRandom.nextInt(bound)
        override fun nextInt(origin: Int, bound: Int): Int = defaultRandom.nextInt(origin, bound)
        override fun nextInt(range: IntRange): Int = defaultRandom.nextInt(range)

        override fun nextLong(): Long = defaultRandom.nextLong()
        override fun nextLong(bound: Long): Long = defaultRandom.nextLong(bound)
        override fun nextLong(origin: Long, bound: Long): Long = defaultRandom.nextLong(origin, bound)
        override fun nextLong(range: LongRange): Long = defaultRandom.nextLong(range)

        override fun nextBoolean(): Boolean = defaultRandom.nextBoolean()

        override fun nextDouble(): Double = defaultRandom.nextDouble()
        override fun nextDouble(bound: Double): Double = defaultRandom.nextDouble(bound)
        override fun nextDouble(origin: Double, bound: Double): Double = defaultRandom.nextDouble(origin, bound)

        override fun nextFloat(): Float = defaultRandom.nextFloat()

        override fun nextBytes(array: ByteArray): ByteArray = defaultRandom.nextBytes(array)
        override fun nextBytes(size: Int): ByteArray = defaultRandom.nextBytes(size)
        override fun nextBytes(array: ByteArray, fromIndex: Int, toIndex: Int): ByteArray = defaultRandom.nextBytes(array, fromIndex, toIndex)


    }
}

/**
 * Returns a repeatable random number generator seeded with the given [seed] `Int` value.
 *
 * Two generators with the same seed produce the same sequence of values.
 *
 * @sample samples.random.Randoms.seededRandom
 */
@SinceKotlin("1.3")
public fun Random(seed: Int): Random = XorWowRandom(seed, seed.shr(31))

/**
 * Returns a repeatable random number generator seeded with the given [seed] `Long` value.
 *
 * Two generators with the same seed produce the same sequence of values.
 *
 * @sample samples.random.Randoms.seededRandom
 */
@SinceKotlin("1.3")
public fun Random(seed: Long): Random = XorWowRandom(seed.toInt(), seed.shr(32).toInt())



/**
 * Gets the next random [UInt] from the random number generator.
 *
 * Generates a [UInt] random value uniformly distributed between [UInt.MIN_VALUE] and [UInt.MAX_VALUE] (inclusive).
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextUInt(): UInt = nextInt().toUInt()

/**
 * Gets the next random [UInt] from the random number generator not greater than the specified [bound].
 *
 * Generates a [UInt] random value uniformly distributed between `0` (inclusive) and the specified [bound] (exclusive).
 *
 * @throws IllegalArgumentException if [bound] is zero.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextUInt(bound: UInt): UInt = nextUInt(0u, bound)

/**
 * Gets the next random [UInt] from the random number generator in the specified range.
 *
 * Generates a [UInt] random value uniformly distributed between the specified [origin] (inclusive) and the specified [bound] (exclusive).
 *
 * @throws IllegalArgumentException if [origin] is greater than or equal to [bound].
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextUInt(origin: UInt, bound: UInt): UInt {
    checkUIntRangeBounds(origin, bound)

    val originTransformedToInt = origin.toInt() xor Int.MIN_VALUE
    val boundTransformedToInt = bound.toInt() xor Int.MIN_VALUE

    val randomValueTransformedBack = nextInt(originTransformedToInt, boundTransformedToInt) xor Int.MIN_VALUE

    return randomValueTransformedBack.toUInt()
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
 * Gets the next random [ULong] from the random number generator not greater than the specified [bound].
 *
 * Generates a [ULong] random value uniformly distributed between `0` (inclusive) and the specified [bound] (exclusive).
 *
 * @throws IllegalArgumentException if [bound] is zero.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextULong(bound: ULong): ULong = nextULong(0uL, bound)

/**
 * Gets the next random [ULong] from the random number generator in the specified range.
 *
 * Generates a [ULong] random value uniformly distributed between the specified [origin] (inclusive) and the specified [bound] (exclusive).
 *
 * @throws IllegalArgumentException if [origin] is greater than or equal to [bound].
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Random.nextULong(origin: ULong, bound: ULong): ULong {
    checkULongRangeBounds(origin, bound)

    val originTransformedToLong = origin.toLong() xor Long.MIN_VALUE
    val boundTransformedToLong = bound.toLong() xor Long.MIN_VALUE

    val randomValueTransformedBack = nextLong(originTransformedToLong, boundTransformedToLong) xor Long.MIN_VALUE
    return randomValueTransformedBack.toULong()
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

internal expect fun defaultPlatformRandom(): Random
internal expect fun fastLog2(value: Int): Int //  31 - Integer.numberOfLeadingZeros(value)
internal expect fun doubleFromParts(hi26: Int, low27: Int): Double

/** Takes upper [bitCount] bits (0..32) from this number. */
internal fun Int.takeUpperBits(bitCount: Int): Int =
    this.ushr(32 - bitCount) and (-bitCount).shr(31)

internal fun checkRangeBounds(origin: Int, bound: Int) = require(bound > origin) { boundsErrorMessage(origin, bound) }
internal fun checkUIntRangeBounds(origin: UInt, bound: UInt) = require(bound > origin) { boundsErrorMessage(origin, bound) }
internal fun checkRangeBounds(origin: Long, bound: Long) = require(bound > origin) { boundsErrorMessage(origin, bound) }
internal fun checkULongRangeBounds(origin: ULong, bound: ULong) = require(bound > origin) { boundsErrorMessage(origin, bound) }
internal fun checkRangeBounds(origin: Double, bound: Double) = require(bound > origin) { boundsErrorMessage(origin, bound) }

private fun boundsErrorMessage(origin: Any, bound: Any) = "Random range is empty: [$origin, $bound)."
