package org.jetbrains.ring


open class IntBaselineBenchmark {

    //Benchmark
    fun consume() {
        for (item in 1..BENCHMARK_SIZE) {
            Blackhole.consume(item)
        }
    }

    //Benchmark
    fun allocateList(): List<Int> {
        val list = ArrayList<Int>(BENCHMARK_SIZE)
        return list
    }

    //Benchmark
    fun allocateArray(): IntArray {
        val list = IntArray(BENCHMARK_SIZE)
        return list
    }

    //Benchmark
    fun allocateListAndFill(): List<Int> {
        val list = ArrayList<Int>(BENCHMARK_SIZE)
        for (item in 1..BENCHMARK_SIZE) {
            list.add(item)
        }
        return list
    }

    //Benchmark
    fun allocateArrayAndFill(): IntArray {
        var index = 0
        val list = IntArray(BENCHMARK_SIZE)
        for (item in 1..BENCHMARK_SIZE) {
            list[index++] = item
        }
        return list
    }
}