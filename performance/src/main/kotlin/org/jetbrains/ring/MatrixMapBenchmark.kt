package org.jetbrains.ring

import org.jetbrains.ring.BENCHMARK_SIZE

/**
 * This class emulates matrix behaviour using a hash map as its implementation
 */
class KMatrix internal constructor(val rows: Int, val columns: Int) {
    private val matrix: MutableMap<Pair<Int, Int>, Double> = HashMap();

    init {
        for (row in 0..rows-1) {
            for (col in 0..columns-1) {
                matrix.put(Pair(row, col), Random.nextDouble())
            }
        }
    }

    fun get(row: Int, col: Int): Double {
        return get(Pair(row, col))
    }

    fun get(pair: Pair<Int, Int>): Double {
        return matrix.getOrElse(pair, { 0.0 })
    }

    fun put(pair: Pair<Int, Int>, elem: Double) {
        matrix.put(pair, elem)
    }

    operator fun plusAssign(other: KMatrix) {
        for (entry in matrix.entries) {
            put(entry.key, entry.value + other.get(entry.key))
        }
    }
}

/**
 * This class tests hash map performance
 */
open class MatrixMapBenchmark {

    //Benchmark
    fun add(): KMatrix {
        var rows = BENCHMARK_SIZE
        var cols = 1
        while (rows > cols) {
            rows /= 2
            cols *= 2
        }
        val a = KMatrix(rows, cols)
        val b = KMatrix(rows, cols)
        a += b
        return a
    }

}