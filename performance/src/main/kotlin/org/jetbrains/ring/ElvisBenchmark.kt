package org.jetbrains.ring

open class ElvisBenchmark {

    class Value(var value: Int)

    var array : Array<Value?> = arrayOf()

    fun setup() {
        array = Array(BENCHMARK_SIZE) {
            if (Random.nextInt(BENCHMARK_SIZE) < BENCHMARK_SIZE / 10) null else Value(Random.nextInt())
        }
    }

    //Benchmark
    fun testElvis() {
        for (obj in array) {
            Blackhole.consume(obj?.value ?: 0)
        }
    }
}
