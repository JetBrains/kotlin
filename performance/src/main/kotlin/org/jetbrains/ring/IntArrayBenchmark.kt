package org.jetbrains.ring

open class IntArrayBenchmark {
    private var _data: IntArray? = null
    val data: IntArray
        get() = _data!!

    fun setup() {
        val list = IntArray(BENCHMARK_SIZE)
        var index = 0
        for (n in intValues(BENCHMARK_SIZE))
            list[index++] = n
        _data = list
    }

    //Benchmark
    fun copy(): List<Int> {
        return data.toList()
    }

    //Benchmark
    fun copyManual(): ArrayList<Int> {
        val list = ArrayList<Int>(data.size)
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
    fun filterSomeAndCount(): Int {
        return data.filter { filterSome(it) }.count()
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
    fun filter(): List<Int> {
        return data.filter { filterLoad(it) }
    }

    //Benchmark
    fun filterSome(): List<Int> {
        return data.filter { filterSome(it) }
    }

    //Benchmark
    fun filterPrime(): List<Int> {
        return data.filter { filterPrime(it) }
    }

    //Benchmark
    fun filterManual(): ArrayList<Int> {
        val list = ArrayList<Int>()
        for (it in data) {
            if (filterLoad(it))
                list.add(it)
        }
        return list
    }

    //Benchmark
    fun filterSomeManual(): ArrayList<Int> {
        val list = ArrayList<Int>()
        for (it in data) {
            if (filterSome(it))
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
    fun countFilteredSomeManual(): Int {
        var count = 0
        for (it in data) {
            if (filterSome(it))
                count++
        }
        return count
    }

    //Benchmark
    fun countFilteredPrimeManual(): Int {
        var count = 0
        for (it in data) {
            if (filterPrime(it))
                count++
        }
        return count
    }

    
    //Benchmark
    fun countFiltered(): Int {
        return data.count { filterLoad(it) }
    }

    //Benchmark
    fun countFilteredSome(): Int {
        return data.count { filterSome(it) }
    }

    //Benchmark
    fun countFilteredPrime(): Int {
        val res = data.count { filterPrime(it) }
        //println(res)
        return res
    }

    //Benchmark
    fun countFilteredLocal(): Int {
        return data.cnt { filterLoad(it) }
    }

    //Benchmark
    fun countFilteredSomeLocal(): Int {
        return data.cnt { filterSome(it) }
    }

    //Benchmark
    fun reduce(): Int {
        return data.fold(0) { acc, it -> if (filterLoad(it)) acc + 1 else acc }
    }
}

