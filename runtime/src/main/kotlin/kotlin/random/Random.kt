/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlin.random

import konan.worker.AtomicLong
import kotlin.system.getTimeNanos

abstract class Random {
    abstract fun nextInt(): Int
    abstract fun nextInt(bound: Int): Int

    abstract fun nextLong(): Long

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
            get() = _seed.get()
            set(value) = update(mult(value))

        private fun mult(value: Long) = (value xor MULTIPLIER) and ((1L shl 48) - 1)

        private fun update(seed: Long) {
            _seed.compareAndSwap(_seed.get(), seed)
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
    }
}