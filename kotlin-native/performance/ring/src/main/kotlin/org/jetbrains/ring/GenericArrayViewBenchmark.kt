/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.benchmarksLauncher.Blackhole
import org.jetbrains.benchmarksLauncher.Random

// Benchmark is inspired by multik library.

interface MemoryView<T> where T : Number {
    fun get(index: Int): T
}

class MemoryViewIntArray(val data: IntArray) : MemoryView<Int> {
    override fun get(index: Int): Int = data[index]
}

class MemoryViewLongArray(val data: LongArray) : MemoryView<Long> {
    override fun get(index: Int): Long = data[index]
}

class MemoryViewDoubleArray(val data: DoubleArray) : MemoryView<Double> {
    override fun get(index: Int): Double = data[index]
}

class Array2D<T>(val data: MemoryView<T>, val width: Int) where T : Number{
    fun getGeneric(ind1: Int, ind2: Int): Int {
        return data.get(width * ind1 + ind2).toInt()
    }

    inline fun getGenericInlined(ind1: Int, ind2: Int): Int {
        return data.get(width * ind1 + ind2).toInt()
    }

    inline fun getSpecializedInlined(ind1: Int, ind2: Int): Int {
        return (data as MemoryViewIntArray).get(width * ind1 + ind2)
    }
}

open class GenericArrayViewBenchmark {
    private val N = 2000

    private val intArr = Array2D(MemoryViewIntArray(IntArray(N * N) { Random.nextInt() }), N)
    // To confuse devirtualizer:
    private val longArr = Array2D(MemoryViewLongArray(LongArray(N * N) { Random.nextInt().toLong() }), N)
    private val doubleArr = Array2D(MemoryViewDoubleArray(DoubleArray(N * N) { Random.nextDouble() }), N)

    init {
        bench(longArr) { a, i, j -> a.getGeneric(i, j) }
        bench(doubleArr) { a, i, j -> a.getGenericInlined(i, j) }
        try { bench(longArr) { a, i, j -> a.getSpecializedInlined(i, j) } } catch (t: ClassCastException) {}
    }

    private inline fun <T> bench(arr: Array2D<T>, getter: (Array2D<T>, Int, Int) -> Int) where T : Number {
        var sum = 0

        for (i in 0 until N) {
            for (j in 0 until N) {
                sum += getter(arr, i, j)
            }
        }

        Blackhole.consume(sum)
    }

    // Bench cases:

    fun origin() { bench(intArr) { a, i, j -> a.getGeneric(i, j) } }
    fun inlined() { bench(intArr) { a, i, j -> a.getGenericInlined(i, j) } }
    fun specialized() { bench(intArr) { a, i, j -> a.getSpecializedInlined(i, j) } }
    fun manual() { bench(intArr) { a, i, j -> a.width * i + j } }
}
