package org.jetbrains.ring

var globalAddendum = 0

open class LambdaBenchmark {
    private fun <T> runLambda(x: () -> T): T = x()
    private fun <T> runLambdaNoInline(x: () -> T): T = x()

    fun setup() {
        globalAddendum = Random.nextInt(20)
    }

    //Benchmark
    fun noncapturingLambda(): Int {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambda { globalAddendum }
        }
        return x
    }

    //Benchmark
    fun noncapturingLambdaNoInline(): Int {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambdaNoInline { globalAddendum }
        }
        return x
    }

    //Benchmark
    fun capturingLambda(): Int {
        val addendum = globalAddendum + 1
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambda { addendum }
        }
        return x
    }

    //Benchmark
    fun capturingLambdaNoInline(): Int {
        val addendum = globalAddendum + 1
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambdaNoInline { addendum }
        }
        return x
    }

    //Benchmark
    fun mutatingLambda(): Int {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            runLambda { x += globalAddendum }
        }
        return x
    }

    //Benchmark
    fun mutatingLambdaNoInline(): Int {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            runLambdaNoInline { x += globalAddendum }
        }
        return x
    }

    //Benchmark
    fun methodReference(): Int {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambda(::referenced)
        }
        return x
    }

    //Benchmark
    fun methodReferenceNoInline(): Int {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambdaNoInline(::referenced)
        }
        return x
    }
}

private fun referenced(): Int {
    return globalAddendum
}
