package org.jetbrains.ring

open class LoopBenchmark {
    lateinit var arrayList: List<Value>
    lateinit var array: Array<Value>

    fun setup() {
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        for (n in classValues(BENCHMARK_SIZE))
            list.add(n)
        arrayList = list
        array = list.toTypedArray()
    }

    //Benchmark 
    fun arrayLoop() {
        for (x in array) {
            Blackhole.consume(x)
        }
    }

    //Benchmark 
    fun arrayIndexLoop() {
        for (i in array.indices) {
            Blackhole.consume(array[i])
        }
    }

    //Benchmark 
    fun rangeLoop() {
        for (i in 0..BENCHMARK_SIZE) {
            Blackhole.consume(i)
        }
    }

    //Benchmark 
    fun arrayListLoop() {
        for (x in arrayList) {
            Blackhole.consume(x)
        }
    }

    //Benchmark 
    fun arrayWhileLoop() {
        var i = 0
        val s = array.size
        while (i < s) {
            Blackhole.consume(array[i])
            i++
        }
    }

    //Benchmark 
    fun arrayForeachLoop() {
        array.forEach { Blackhole.consume(it) }
    }

    //Benchmark 
    fun arrayListForeachLoop() {
        arrayList.forEach { Blackhole.consume(it) }
    }
}