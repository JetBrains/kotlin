/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

import kotlin.internal.ReadObjectParameterType
import kotlin.internal.throwReadObjectNotSupported
import kotlin.math.nextDown

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
public abstract class Random {

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
    public open fun nextInt(): Int = nextBits(32)

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
    public open fun nextInt(until: Int): Int = nextInt(0, until)

    /**
     * Gets the next random `Int` from the random number generator in the specified range.
     *
     * Generates an `Int` random value uniformly distributed between the specified [from] (inclusive) and [until] (exclusive) bounds.
     *
     * @throws IllegalArgumentException if [from] is greater than or equal to [until].
     *
     * @sample samples.random.Randoms.nextIntFromUntil
     */
    public open fun nextInt(from: Int, until: Int): Int {
        checkRangeBounds(from, until)
        val n = until - from
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
            return from + rnd
        } else {
            while (true) {
                val rnd = nextInt()
                if (rnd in from until until) return rnd
            }
        }
    }

    /**
     * Gets the next random `Long` from the random number generator.
     *
     * Generates a `Long` random value uniformly distributed between `Long.MIN_VALUE` and `Long.MAX_VALUE` (inclusive).
     *
     * @sample samples.random.Randoms.nextLong
     */
    public open fun nextLong(): Long = nextInt().toLong().shl(32) + nextInt()

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
    public open fun nextLong(until: Long): Long = nextLong(0, until)

    /**
     * Gets the next random `Long` from the random number generator in the specified range.
     *
     * Generates a `Long` random value uniformly distributed between the specified [from] (inclusive) and [until] (exclusive) bounds.
     *
     * @throws IllegalArgumentException if [from] is greater than or equal to [until].
     *
     * @sample samples.random.Randoms.nextLongFromUntil
     */
    public open fun nextLong(from: Long, until: Long): Long {
        checkRangeBounds(from, until)
        val n = until - from
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
                        nextBits(bitCount).toLong().shl(32) + (nextInt().toLong() and 0xFFFF_FFFF)
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
            return from + rnd
        } else {
            while (true) {
                val rnd = nextLong()
                if (rnd in from until until) return rnd
            }
        }
    }

    /**
     * Gets the next random [Boolean] value.
     *
     * @sample samples.random.Randoms.nextBoolean
     */
    public open fun nextBoolean(): Boolean = nextBits(1) != 0

    /**
     * Gets the next random [Double] value uniformly distributed between 0 (inclusive) and 1 (exclusive).
     *
     * @sample samples.random.Randoms.nextDouble
     */
    public open fun nextDouble(): Double = doubleFromParts(nextBits(26), nextBits(27))

    /**
     * Gets the next random non-negative `Double` from the random number generator less than the specified [until] bound.
     *
     * Generates a `Double` random value uniformly distributed between 0 (inclusive) and [until] (exclusive).
     *
     * @throws IllegalArgumentException if [until] is negative or zero.
     *
     * @sample samples.random.Randoms.nextDoubleFromUntil
     */
    public open fun nextDouble(until: Double): Double = nextDouble(0.0, until)

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
    public open fun nextDouble(from: Double, until: Double): Double {
        checkRangeBounds(from, until)
        val size = until - from
        val r = if (size.isInfinite() && from.isFinite() && until.isFinite()) {
            val r1 = nextDouble() * (until / 2 - from / 2)
            from + r1 + r1
        } else {
            from + nextDouble() * size
        }
        return if (r >= until) until.nextDown() else r
    }

    /**
     * Gets the next random [Float] value uniformly distributed between 0 (inclusive) and 1 (exclusive).
     *
     * @sample samples.random.Randoms.nextFloat
     */
    public open fun nextFloat(): Float = nextBits(24) / (1 shl 24).toFloat()

    /**
     * Fills a subrange of the specified byte [array] starting from [fromIndex] inclusive and ending [toIndex] exclusive
     * with random bytes.
     *
     * @return [array] with the subrange filled with random bytes.
     *
     * @sample samples.random.Randoms.nextBytes
     */
    @IgnorableReturnValue
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
     *
     * @sample samples.random.Randoms.nextBytes
     */
    @IgnorableReturnValue
    public open fun nextBytes(array: ByteArray): ByteArray = nextBytes(array, 0, array.size)

    /**
     * Creates a byte array of the specified [size], filled with random bytes.
     *
     * @sample samples.random.Randoms.nextBytes
     */
    public open fun nextBytes(size: Int): ByteArray = nextBytes(ByteArray(size))


    /**
     * The default random number generator.
     *
     * On JVM this generator is thread-safe, its methods can be invoked from multiple threads.
     *
     * @sample samples.random.Randoms.defaultRandom
     */
    public companion object Default : Random(), Serializable {
        private val defaultRandom: Random = defaultPlatformRandom()

        private object Serialized : Serializable {
            private const val serialVersionUID = 0L

            private fun readResolve(): Any = Random
        }

        private fun writeReplace(): Any = Serialized

        private fun readObject(input: ReadObjectParameterType): Unit = throwReadObjectNotSupported()

        override fun nextBits(bitCount: Int): Int = defaultRandom.nextBits(bitCount)
        override fun nextInt(): Int = defaultRandom.nextInt()
        override fun nextInt(until: Int): Int = defaultRandom.nextInt(until)
        override fun nextInt(from: Int, until: Int): Int = defaultRandom.nextInt(from, until)

        override fun nextLong(): Long = defaultRandom.nextLong()
        override fun nextLong(until: Long): Long = defaultRandom.nextLong(until)
        override fun nextLong(from: Long, until: Long): Long = defaultRandom.nextLong(from, until)

        override fun nextBoolean(): Boolean = defaultRandom.nextBoolean()

        override fun nextDouble(): Double = defaultRandom.nextDouble()
        override fun nextDouble(until: Double): Double = defaultRandom.nextDouble(until)
        override fun nextDouble(from: Double, until: Double): Double = defaultRandom.nextDouble(from, until)

        override fun nextFloat(): Float = defaultRandom.nextFloat()

        override fun nextBytes(array: ByteArray): ByteArray = defaultRandom.nextBytes(array)
        override fun nextBytes(size: Int): ByteArray = defaultRandom.nextBytes(size)
        override fun nextBytes(array: ByteArray, fromIndex: Int, toIndex: Int): ByteArray =
            defaultRandom.nextBytes(array, fromIndex, toIndex)
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


internal expect fun defaultPlatformRandom(): Random
internal expect fun doubleFromParts(hi26: Int, low27: Int): Double

internal fun fastLog2(value: Int): Int = 31 - value.countLeadingZeroBits()

/** Takes upper [bitCount] bits (0..32) from this number. */
internal fun Int.takeUpperBits(bitCount: Int): Int =
    this.ushr(32 - bitCount) and (-bitCount).shr(31)

internal fun checkRangeBounds(from: Int, until: Int) = require(until > from) { boundsErrorMessage(from, until) }
internal fun checkRangeBounds(from: Long, until: Long) = require(until > from) { boundsErrorMessage(from, until) }
internal fun checkRangeBounds(from: Double, until: Double) = require(until > from) { boundsErrorMessage(from, until) }

internal fun boundsErrorMessage(from: Any, until: Any) = "Random range is empty: [$from, $until)."
