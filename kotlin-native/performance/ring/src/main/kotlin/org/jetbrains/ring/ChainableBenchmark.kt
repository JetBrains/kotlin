/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.ring

import org.jetbrains.benchmarksLauncher.Blackhole
import org.jetbrains.benchmarksLauncher.Random

// Benchmark from KT-46482.
open class ChainableBenchmark {

    class IntArrayList(capacity: Int = 7) {
        private var data: IntArray = IntArray(capacity)
        var size: Int = 0
            private set
        val capacity get() = data.size
        private fun grow(minSize: Int) {
            data = data.copyOf(kotlin.math.max(minSize, capacity * 3))
        }
        private fun ensure(count: Int) {
            if (size + count >= capacity) {
                grow(size + count)
            }
        }

        fun add(value: Int) {
            ensure(1)
            data[size++] = value
        }

        fun addChainable(value: Int): IntArrayList {
            add(value)
            return this
        }

        operator fun get(index: Int): Int = data[index]
        operator fun set(index: Int, value: Int) {
            data[index] = value
        }
    }
    val size = BENCHMARK_SIZE * 100

    //Benchmark
    fun testChainable() {
        val list = IntArrayList()
        for (i in 0..size) {
            list.addChainable(i)
        }
        for (i in 0..size) {
            list[i] = i * 2
        }
        var sum = 0
        for (i in 0..size) {
            sum += list[i]
        }
    }
}
