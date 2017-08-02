package org.jetbrains.ring

open class WithIndiciesBenchmark {
    private var _data: ArrayList<Value>? = null
    val data: ArrayList<Value>
        get() = _data!!

    fun setup() {
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        for (n in classValues(BENCHMARK_SIZE))
            list.add(n)
        _data = list
    }

    //Benchmark
    fun withIndicies() {
        for ((index, value) in data.withIndex()) {
            if (filterLoad(value)) {
                Blackhole.consume(index)
                Blackhole.consume(value)
            }
        }
    }

    //Benchmark
    fun withIndiciesManual() {
        var index = 0
        for (value in data) {
            if (filterLoad(value)) {
                Blackhole.consume(index)
                Blackhole.consume(value)
            }
            index++
        }
    }
}
