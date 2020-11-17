/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

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
public actual abstract class Random {

    /**
     * Gets the next random [bitCount] number of bits.
     *
     * Generates an `Int` whose lower [bitCount] bits are filled with random values and the remaining upper bits are zero.
     *
     * @param bitCount number of bits to generate, must be in range 0..32, otherwise the behavior is unspecified.
     *
     * @sample samples.random.Randoms.nextBits
     */
    public actual abstract fun nextBits(bitCount: Int): Int

    /**
     * Gets the next random `Int` from the random number generator.
     *
     * Generates an `Int` random value uniformly distributed between `Int.MIN_VALUE` and `Int.MAX_VALUE` (inclusive).
     *
     * @sample samples.random.Randoms.nextInt
     */
    public actual open fun nextInt(): Int = nextBits(32)

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
    public actual open fun nextInt(until: Int): Int = nextInt(0, until)

    /**
     * Gets the next random `Int` from the random number generator in the specified range.
     *
     * Generates an `Int` random value uniformly distributed between the specified [from] (inclusive) and [until] (exclusive) bounds.
     *
     * @throws IllegalArgumentException if [from] is greater than or equal to [until].
     *
     * @sample samples.random.Randoms.nextIntFromUntil
     */
    public actual open fun nextInt(from: Int, until: Int): Int {
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
    public actual open fun nextLong(): Long = nextInt().toLong().shl(32) + nextInt()

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
    public actual open fun nextLong(until: Long): Long = nextLong(0, until)

    /**
     * Gets the next random `Long` from the random number generator in the specified range.
     *
     * Generates a `Long` random value uniformly distributed between the specified [from] (inclusive) and [until] (exclusive) bounds.
     *
     * @throws IllegalArgumentException if [from] is greater than or equal to [until].
     *
     * @sample samples.random.Randoms.nextLongFromUntil
     */
    public actual open fun nextLong(from: Long, until: Long): Long {
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
    public actual open fun nextBoolean(): Boolean = nextBits(1) != 0

    /**
     * Gets the next random [Double] value uniformly distributed between 0 (inclusive) and 1 (exclusive).
     *
     * @sample samples.random.Randoms.nextDouble
     */
    public actual open fun nextDouble(): Double = TODO("Wasm stdlib: Random")

    /**
     * Gets the next random non-negative `Double` from the random number generator less than the specified [until] bound.
     *
     * Generates a `Double` random value uniformly distributed between 0 (inclusive) and [until] (exclusive).
     *
     * @throws IllegalArgumentException if [until] is negative or zero.
     *
     * @sample samples.random.Randoms.nextDoubleFromUntil
     */
    public actual open fun nextDouble(until: Double): Double = nextDouble(0.0, until)

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
    public actual open fun nextDouble(from: Double, until: Double): Double {
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
    public actual open fun nextFloat(): Float = nextBits(24) / (1 shl 24).toFloat()

    /**
     * Fills a subrange of the specified byte [array] starting from [fromIndex] inclusive and ending [toIndex] exclusive
     * with random bytes.
     *
     * @return [array] with the subrange filled with random bytes.
     *
     * @sample samples.random.Randoms.nextBytes
     */
    public actual open fun nextBytes(array: ByteArray, fromIndex: Int, toIndex: Int): ByteArray {
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
    public actual open fun nextBytes(array: ByteArray): ByteArray = nextBytes(array, 0, array.size)

    /**
     * Creates a byte array of the specified [size], filled with random bytes.
     *
     * @sample samples.random.Randoms.nextBytes
     */
    public actual open fun nextBytes(size: Int): ByteArray = nextBytes(ByteArray(size))


    /**
     * The default random number generator.
     *
     * On JVM this generator is thread-safe, its methods can be invoked from multiple threads.
     *
     * @sample samples.random.Randoms.defaultRandom
     */
    actual companion object Default : Random() {
        private val defaultRandom: Random = TODO("Wasm stdlib: Random")

        actual override fun nextBits(bitCount: Int): Int = defaultRandom.nextBits(bitCount)
        actual override fun nextInt(): Int = defaultRandom.nextInt()
        actual override fun nextInt(until: Int): Int = defaultRandom.nextInt(until)
        actual override fun nextInt(from: Int, until: Int): Int = defaultRandom.nextInt(from, until)

        actual override fun nextLong(): Long = defaultRandom.nextLong()
        actual override fun nextLong(until: Long): Long = defaultRandom.nextLong(until)
        actual override fun nextLong(from: Long, until: Long): Long = defaultRandom.nextLong(from, until)

        actual override fun nextBoolean(): Boolean = defaultRandom.nextBoolean()

        actual override fun nextDouble(): Double = defaultRandom.nextDouble()
        actual override fun nextDouble(until: Double): Double = defaultRandom.nextDouble(until)
        actual override fun nextDouble(from: Double, until: Double): Double = defaultRandom.nextDouble(from, until)

        actual override fun nextFloat(): Float = defaultRandom.nextFloat()

        actual override fun nextBytes(array: ByteArray): ByteArray = defaultRandom.nextBytes(array)
        actual override fun nextBytes(size: Int): ByteArray = defaultRandom.nextBytes(size)
        actual override fun nextBytes(array: ByteArray, fromIndex: Int, toIndex: Int): ByteArray =
            defaultRandom.nextBytes(array, fromIndex, toIndex)
    }
}

/**
 * Random number generator, using Marsaglia's "xorwow" algorithm
 *
 * Cycles after 2^192 - 2^32 repetitions.
 *
 * For more details, see Marsaglia, George (July 2003). "Xorshift RNGs". Journal of Statistical Software. 8 (14). doi:10.18637/jss.v008.i14
 *
 * Available at https://www.jstatsoft.org/v08/i14/paper
 *
 */
internal actual class XorWowRandom internal actual constructor(
    private var x: Int,
    private var y: Int,
    private var z: Int,
    private var w: Int,
    private var v: Int,
    private var addend: Int,
) : Random() {

    internal actual constructor(seed1: Int, seed2: Int) :
            this(seed1, seed2, 0, 0, seed1.inv(), (seed1 shl 10) xor (seed2 ushr 4))

    init {
        require((x or y or z or w or v) != 0) { "Initial state must have at least one non-zero element." }

        // some trivial seeds can produce several values with zeroes in upper bits, so we discard first 64
        repeat(64) { nextInt() }
    }

    actual override fun nextInt(): Int {
        // Equivalent to the xorxow algorithm
        // From Marsaglia, G. 2003. Xorshift RNGs. J. Statis. Soft. 8, 14, p. 5
        var t = x
        t = t xor (t ushr 2)
        x = y
        y = z
        z = w
        val v0 = v
        w = v0
        t = (t xor (t shl 1)) xor v0 xor (v0 shl 4)
        v = t
        addend += 362437
        return t + addend
    }

    actual override fun nextBits(bitCount: Int): Int =
        nextInt().takeUpperBits(bitCount)
}
