package org.jetbrains.ring

open class ClassArrayBenchmark {
    private var _data: Array<Value>? = null
    val data: Array<Value>
        get() = _data!!

    fun setup() {
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        for (n in classValues(BENCHMARK_SIZE))
            list.add(n)
        _data = list.toTypedArray()
    }

    //Benchmark
    fun copy(): List<Value> {
        return data.toList()
    }

    //Benchmark
    fun copyManual(): List<Value> {
        val list = ArrayList<Value>(data.size)
        for (item in data) {
            list.add(item)
        }
        return list
    }

    //Benchmark
    fun filterAndCount(): Int {
        return data.filter { filterLoad(it) }.count()
    }

    //Benchmark
    fun filterAndMap(): List<String> {
        return data.filter { filterLoad(it) }.map { mapLoad(it) }
    }

    //Benchmark
    fun filterAndMapManual(): ArrayList<String> {
        val list = ArrayList<String>()
        for (it in data) {
            if (filterLoad(it)) {
                val value = mapLoad(it)
                list.add(value)
            }
        }
        return list
    }

    //Benchmark
    fun filter(): List<Value> {
        return data.filter { filterLoad(it) }
    }

    //Benchmark
    fun filterManual(): List<Value> {
        val list = ArrayList<Value>()
        for (it in data) {
            if (filterLoad(it))
                list.add(it)
        }
        return list
    }

    //Benchmark
    fun countFilteredManual(): Int {
        var count = 0
        for (it in data) {
            if (filterLoad(it))
                count++
        }
        return count
    }

    //Benchmark
    fun countFiltered(): Int {
        return data.count { filterLoad(it) }
    }

    //Benchmark
    fun countFilteredLocal(): Int {
        return data.cnt { filterLoad(it) }
    }

    //Benchmark
//    fun reduce(): Int {
//        return data.fold(0) { acc, it -> if (filterLoad(it)) acc + 1 else acc }
//    }
}
