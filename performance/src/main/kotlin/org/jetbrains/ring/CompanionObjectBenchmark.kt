package org.jetbrains.ring

open class CompanionObjectBenchmark {
    //Benchmark
    fun invokeRegularFunction() {
        regularCompanionObjectFunction("")
    }

    //Benchmark
    fun invokeJvmStaticFunction() {
        staticCompanionObjectFunction("")
    }

    companion object {
        fun regularCompanionObjectFunction(o: Any): Any {
            return o
        }

        fun staticCompanionObjectFunction(o: Any): Any {
            return o
        }
    }
}