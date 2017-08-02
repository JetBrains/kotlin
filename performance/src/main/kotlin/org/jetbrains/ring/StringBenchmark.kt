package org.jetbrains.ring

open class StringBenchmark {
    private var _data: ArrayList<String>? = null
    val data: ArrayList<String>
        get() = _data!!
    var csv: String = ""

    fun setup() {
        val list = ArrayList<String>(BENCHMARK_SIZE)
        for (n in stringValues(BENCHMARK_SIZE))
            list.add(n)
        _data = list
        csv = ""
        for (i in 1..BENCHMARK_SIZE-1) {
            val elem = Random.nextDouble()
            csv += elem
            csv += ","
        }
        csv += 0.0
    }
    
    //Benchmark
    open fun stringConcat(): String? {
        var string: String = ""
        for (it in data) string += it
        return string
    }
    
    //Benchmark
    open fun stringConcatNullable(): String? {
        var string: String? = ""
        for (it in data) string += it
        return string
    }
    
    //Benchmark
    open fun stringBuilderConcat(): String {
        var string : StringBuilder = StringBuilder("")
        for (it in data) string.append(it)
        return string.toString()
    }
    
    //Benchmark
    open fun stringBuilderConcatNullable(): String {
        var string : StringBuilder? = StringBuilder("")
        for (it in data) string?.append(it)
        return string.toString()
    }
    
    //Benchmark
    open fun summarizeSplittedCsv(): Double {
        val fields = csv.split(",")
        var sum = 0.0
        for (field in fields) {
            sum += field.toDouble()
        }
        return sum
    }
}