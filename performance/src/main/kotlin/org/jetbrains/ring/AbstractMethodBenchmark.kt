package org.jetbrains.ring

/**
 * Created by Mikhail.Glukhikh on 06/03/2015.
 *
 * A benchmark for a single abstract method based on a string comparison
 */

open class AbstractMethodBenchmark {

    private val arr: List<String> = zdf_win
    private val sequence = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"

    private val sequenceMap = HashMap<Char, Int>()

    init {
        var i = 0;
        for (ch in sequence) {
            sequenceMap[ch] = i++;
        }
    }

    //Benchmark
    fun sortStrings(): Set<String> {
        val res = arr.subList(0, if (BENCHMARK_SIZE < arr.size) BENCHMARK_SIZE else arr.size).toSet()
        return res
    }

    //Benchmark
    fun sortStringsWithComparator(): Set<String> {
        val res = mutableSetOf<String>()
        res.addAll(arr.subList(0, if (BENCHMARK_SIZE < arr.size) BENCHMARK_SIZE else arr.size))
        return res
    }
}

