package org.jetbrains.ring

open class ClassBaselineBenchmark {

    //Benchmark 
    fun consume() {
        for (item in 1..BENCHMARK_SIZE) {
            Blackhole.consume(Value(item))
        }
    }

    //Benchmark 
    fun consumeField() {
        val value = Value(0)
        for (item in 1..BENCHMARK_SIZE) {
            value.value = item
            Blackhole.consume(value)
        }
    }

    //Benchmark 
    fun allocateList(): List<Value> {
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        return list
    }

    //Benchmark 
    fun allocateArray(): Array<Value?> {
        val list = arrayOfNulls<Value>(BENCHMARK_SIZE)
        return list
    }

    //Benchmark 
    fun allocateListAndFill(): List<Value> {
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        for (item in 1..BENCHMARK_SIZE) {
            list.add(Value(item))
        }
        return list
    }

    //Benchmark 
    fun allocateListAndWrite(): List<Value> {
        val value = Value(0)
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        for (item in 1..BENCHMARK_SIZE) {
            list.add(value)
        }
        return list
    }

    //Benchmark 
    fun allocateArrayAndFill(): Array<Value?> {
        val list = arrayOfNulls<Value>(BENCHMARK_SIZE)
        var index = 0
        for (item in 1..BENCHMARK_SIZE) {
            list[index++] = Value(item)
        }
        return list
    }
}