/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.random

import kotlin.native.concurrent.AtomicLong
import kotlin.system.getTimeNanos

public abstract class NativeRandom {
    /**
     * A default pseudo-random linear congruential generator.
     */
    companion object : Random() {
        private const val MULTIPLIER = 0x5deece66dL
        private val _seed = AtomicLong(mult(getTimeNanos()))

        /**
         * Random generator seed value.
         */
        var seed: Long
            get() = _seed.value
            set(value) = update(mult(value))

        private fun mult(value: Long) = (value xor MULTIPLIER) and ((1L shl 48) - 1)

        private fun update(seed: Long): Unit {
            _seed.value = seed
        }

        /**
         * Returns a pseudo-random Int number.
         */
        override fun nextInt(): Int {
            update((seed * MULTIPLIER + 0xbL) and ((1L shl 48) - 1));
            return (seed ushr 16).toInt();
        }

        /**
         * Returns a pseudo-random Int value between 0 and specified value (exclusive)
         */
        override fun nextInt(bound: Int): Int {
            if (bound <= 0) throw IllegalArgumentException("Incorrect bound: $bound")

            if (bound and (bound - 1) == 0) {
                return ((bound * (nextInt() ushr 1).toLong()) ushr 31).toInt();
            }

            var m: Int
            do {
                var r = nextInt() ushr 1
                m = r % bound
            } while (r - m + (bound - 1) < 0)
            return m
        }

        /**
         * Returns a pseudo-random Long number.
         */
        override fun nextLong(): Long = (nextInt().toLong() shl 32) + nextInt().toLong()

        override fun nextBits(bitCount: Int): Int = TODO("unimplemented")
    }
}


internal actual fun defaultPlatformRandom(): Random = NativeRandom
internal actual fun fastLog2(value: Int): Int  = TODO("unimplemented")
internal actual fun doubleFromParts(hi26: Int, low27: Int): Double  = TODO("unimplemented")

// TODO: common stdlib Random hasn't got seed, this is workaround for running K/N tests.
var Random.seed:Long
  get() = NativeRandom.seed
  set(value) { NativeRandom.seed = value }