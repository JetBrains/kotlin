/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

import kotlin.*

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
    public abstract fun nextBits(bitCount: Int): Int

    public open fun nextInt(): Int = nextBits(32)
    public open fun nextInt(bound: Int): Int = nextInt(0, bound)
    public open fun nextInt(origin: Int, bound: Int): Int {
        checkRangeBounds(origin.toLong(), bound.toLong())
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

    public open fun nextInt(range: IntRange): Int = when {
        range.last < Int.MAX_VALUE -> nextInt(range.first, range.last + 1)
        range.first > Int.MIN_VALUE -> nextInt(range.first - 1, range.last) + 1
        else -> nextInt()
    }


    public open fun nextLong(): Long = nextInt().toLong().shl(32) + nextInt()

    public open fun nextLong(bound: Long): Long = nextLong(0, bound)

    public open fun nextLong(origin: Long, bound: Long): Long {
        checkRangeBounds(origin, bound)
        val n = bound - origin
        if (n > 0) {
            val rnd: Long
            if (n and -n == n) {
                val bitCount = fastLog2((n ushr 32).toInt())
                rnd = when {
                    bitCount < 0 ->
                        // toUInt().toLong()
                        nextBits(fastLog2(n.toInt())).toLong() and 0xFFFFFFFF
                    bitCount == 0 ->
                        nextBits(32).toLong() and 0xFFFFFFFF
                    else ->
                        nextBits(bitCount).toLong().shl(32) + nextBits(32)
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

    public open fun nextLong(range: LongRange): Long = when {
//        range.isEmpty() -> throw IllegalArgumentException("Cannot get random in empty range: $range")
        range.last < Long.MAX_VALUE -> nextLong(range.start, range.endInclusive + 1)
        range.start > Long.MIN_VALUE -> nextLong(range.start - 1, range.endInclusive) + 1
        else -> nextLong()
    }

    public open fun nextBoolean(): Boolean = nextBits(1) != 0

    public open fun nextDouble(): Double = (nextBits(26).toLong().shl(27) + nextBits(27)) / (1L shl 53).toDouble()
    public open fun nextFloat(): Float = nextBits(24) / (1 shl 24).toFloat()

    public open fun nextBytes(array: ByteArray, startIndex: Int = 0, endIndex: Int = array.size): ByteArray {
        // TODO: check range

        var v: Int = 0
        var bits: Int = 0
        for (i in startIndex until endIndex) {
            if (bits == 0) {
                bits = if (endIndex - i >= 4) 32 else (endIndex - i) * 8
                v = nextBits(bits)
            }
            array[i] = v.toByte()
            v = v ushr 8
            bits -= 8
        }
        return array
    }

    public open fun nextBytes(array: ByteArray): ByteArray = nextBytes(array, 0, array.size)

    public open fun nextBytes(size: Int): ByteArray = nextBytes(ByteArray(size))

    // TODO: UInt, ULong, UByteArray

    /**
     * The default random number generator.
     *
     *
     *
     * @sample samples.random.Randoms.defaultRandom
     */
    companion object : Random() {

        private var _defaultRandom: Random? = null
        private val defaultRandom: Random
            get() = _defaultRandom ?: defaultPlatformRandom().also { _defaultRandom = it }


        override fun nextBits(bitCount: Int): Int = defaultRandom.nextBits(bitCount)

        // TODO: Override others

        internal fun checkRangeBounds(origin: Long, bound: Long) {
            if (origin >= bound) throw IllegalArgumentException("Random range is empty: [$origin, $bound)")
        }

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
public fun Random(seed: Int): Random = XorWowRandom(seed)

/**
 * Returns a repeatable random number generator seeded with the given [seed] `Long` value.
 *
 * Two generators with the same seed produce the same sequence of values.
 *
 * @sample samples.random.Randoms.seededRandom
 */
@SinceKotlin("1.3")
public fun Random(seed: Long): Random = XorWowRandom(seed)


internal expect fun defaultPlatformRandom(): Random
internal expect fun fastLog2(value: Int): Int //  31 - Integer.numberOfLeadingZeros(value)

