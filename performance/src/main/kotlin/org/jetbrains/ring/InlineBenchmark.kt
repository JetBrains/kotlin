package org.jetbrains.ring

fun load(value: Int, size: Int): Int {
    var acc = 0
    for (i in 0..size) {
        acc = acc xor value.hashCode()
    }
    return acc
}

inline fun loadInline(value: Int, size: Int): Int {
    var acc = 0
    for (i in 0..size) {
        acc = acc xor value.hashCode()
    }
    return acc
}

fun <T: Any> loadGeneric(value: T, size: Int): Int {
    var acc = 0
    for (i in 0..size) {
        acc = acc xor value.hashCode()
    }
    return acc
}

inline fun <T: Any> loadGenericInline(value: T, size: Int): Int {
    var acc = 0
    for (i in 0..size) {
        acc = acc xor value.hashCode()
    }
    return acc
}

open class InlineBenchmark {
    private var value = 2138476523

    //Benchmark
    fun calculate(): Int {
        return load(value, BENCHMARK_SIZE)
    }

    //Benchmark
    fun calculateInline(): Int {
        return loadInline(value, BENCHMARK_SIZE)
    }

    //Benchmark
    fun calculateGeneric(): Int {
        return loadGeneric(value, BENCHMARK_SIZE)
    }

    //Benchmark
    fun calculateGenericInline(): Int {
        return loadGenericInline(value, BENCHMARK_SIZE)
    }
}