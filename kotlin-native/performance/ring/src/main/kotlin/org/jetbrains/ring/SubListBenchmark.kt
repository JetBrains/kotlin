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

class SubListBenchmark {
    private var _data: List<Value>? = null

    fun getData(subList: Boolean): List<Value> {
        val data: List<Value> = _data!!
        // 1 to ensure that .subList can't return the list itself
        if (subList) return data.subList(1, data.size)
        else return data
    }

    init {
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        for (n in classValues(BENCHMARK_SIZE))
            list.add(n)
        _data = list
    }

    //Benchmark
    fun concatenate(): List<Value> {
        return getData(false) + getData(true)
    }

    //Benchmark
    fun concatenateManual(): List<Value> {
        val list = ArrayList<Value>(2 * BENCHMARK_SIZE)
        // outer loop to ensure single call site for list and sublist
        for (data in listOf(getData(false), getData(true))) {
            for (item in data) {
                list.add(item)
            }
        }
        return list
    }

    //Benchmark
    fun filterAndCount(): Int {
        var count = 0
        for (data in listOf(getData(false), getData(true))) {
            count += data.filter { filterLoad(it) }.count()
        }
        return count
    }

    //Benchmark
    fun filterAndCountWithLambda(): Int {
        var count = 0
        for (data in listOf(getData(false), getData(true))) {
            count += data.filter { it.value % 2 == 0 }.count()
        }
        return count
    }

    //Benchmark
    fun countWithLambda(): Int {
        var count = 0
        for (data in listOf(getData(false), getData(true))) {
            count += data.count { it.value % 2 == 0 }
        }
        return count
    }

    //Benchmark
    fun filterManual(): List<Value> {
        val list = ArrayList<Value>()
        for (data in listOf(getData(false), getData(true))) {
            for (it in data) {
                if (filterLoad(it))
                    list.add(it)
            }
        }
        return list
    }

    //Benchmark
    fun countFilteredManual(): Int {
        var count = 0
        for (data in listOf(getData(false), getData(true))) {
            for (it in data) {
                if (filterLoad(it))
                    count++
            }
        }
        return count
    }

    //Benchmark
    fun countFiltered(): Int {
        var count = 0
        for (data in listOf(getData(false), getData(true))) {
            count += data.count { filterLoad(it) }
        }
        return count
    }

    //Benchmark
    fun reduce(): Int {
        var res = 0
        for (data in listOf(getData(false), getData(true))) {
            res = data.fold(res) { acc, it -> if (filterLoad(it)) acc + 1 else acc }
        }
        return res
    }
}
