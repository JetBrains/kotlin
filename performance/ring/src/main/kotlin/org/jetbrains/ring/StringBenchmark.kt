/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.ring

open class StringBenchmark {
    private var _data: ArrayList<String>? = null
    val data: ArrayList<String>
        get() = _data!!
    var csv: String = ""

    init {
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