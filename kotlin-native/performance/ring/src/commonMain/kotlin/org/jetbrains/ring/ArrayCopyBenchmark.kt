/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.ring

import org.jetbrains.benchmarksLauncher.Blackhole
import kotlin.random.Random

open class ArrayCopyBenchmark {
    class CustomArray<T>(capacity: Int = 0) {
        private var hashes: IntArray = IntArray(capacity)
        private var values: Array<T?> = arrayOfNulls<Any>(capacity) as Array<T?>
        private var _size: Int = 0

        fun add(index: Int, element: T): Boolean {
            val oldSize = _size

            // Grow the array if needed.
            if (oldSize == hashes.size) {
                val newSize = if (oldSize > 0) oldSize * 2 else 2
                hashes = hashes.copyOf(newSize)
                values = values.copyOf(newSize)
            }

            // Shift the array if needed.
            if (index < oldSize) {
                hashes.copyInto(
                        hashes,
                        destinationOffset = index + 1,
                        startIndex = index,
                        endIndex = oldSize
                )
                values.copyInto(
                        values,
                        destinationOffset = index + 1,
                        startIndex = index,
                        endIndex = oldSize
                )
            }

            hashes[index] = element.hashCode()
            values[index] = element

            _size++
            return true
        }
    }

    //Benchmark
    fun copyInSameArray(): CustomArray<Int> {
        val array = CustomArray<Int>()
        for (i in 0 until 2 * BENCHMARK_SIZE) {
            array.add(0, i)
        }
        return array
    }
}
