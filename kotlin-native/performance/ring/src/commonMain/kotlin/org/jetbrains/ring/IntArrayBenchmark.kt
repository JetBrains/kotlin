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

import kotlinx.benchmark.Blackhole

private const val BENCHMARK_SIZE = 10000

open class IntArrayBenchmark {
    private var _data: IntArray? = null
    val data: IntArray
        get() = _data!!

    init {
        val list = IntArray(BENCHMARK_SIZE)
        var index = 0
        for (n in intValues(BENCHMARK_SIZE))
            list[index++] = n
        _data = list
    }

    //Benchmark
    fun copy(bh: Blackhole) {
        bh.consume(data.toList())
    }

    //Benchmark
    fun copyManual(bh: Blackhole) {
        val list = ArrayList<Int>(data.size)
        for (item in data) {
            list.add(item)
        }
        bh.consume(list)
    }

    //Benchmark
    fun filterAndCount(bh: Blackhole) {
        bh.consume(data.filter { filterLoad(it) }.count())
    }

    //Benchmark
    fun filterSomeAndCount(bh: Blackhole) {
        bh.consume(data.filter { filterSome(it) }.count())
    }

    //Benchmark
    fun filterAndMap(bh: Blackhole) {
        bh.consume(data.filter { filterLoad(it) }.map { mapLoad(it) })
    }

    //Benchmark
    fun filterAndMapManual(bh: Blackhole) {
        val list = ArrayList<String>()
        for (it in data) {
            if (filterLoad(it)) {
                val value = mapLoad(it)
                list.add(value)
            }
        }
        bh.consume(list)
    }

    //Benchmark
    fun filter(bh: Blackhole) {
        bh.consume(data.filter { filterLoad(it) })
    }

    //Benchmark
    fun filterSome(bh: Blackhole) {
        bh.consume(data.filter { filterSome(it) })
    }

    //Benchmark
    fun filterPrime(bh: Blackhole) {
        bh.consume(data.filter { filterPrime(it) })
    }

    //Benchmark
    fun filterManual(bh: Blackhole) {
        val list = ArrayList<Int>()
        for (it in data) {
            if (filterLoad(it))
                list.add(it)
        }
        bh.consume(list)
    }

    //Benchmark
    fun filterSomeManual(bh: Blackhole) {
        val list = ArrayList<Int>()
        for (it in data) {
            if (filterSome(it))
                list.add(it)
        }
        bh.consume(list)
    }

    //Benchmark
    fun countFilteredManual(bh: Blackhole) {
        var count = 0
        for (it in data) {
            if (filterLoad(it))
                count++
        }
        bh.consume(count)
    }

    //Benchmark
    fun countFilteredSomeManual(bh: Blackhole) {
        var count = 0
        for (it in data) {
            if (filterSome(it))
                count++
        }
        bh.consume(count)
    }

    //Benchmark
    fun countFilteredPrimeManual(bh: Blackhole) {
        var count = 0
        for (it in data) {
            if (filterPrime(it))
                count++
        }
        bh.consume(count)
    }

    
    //Benchmark
    fun countFiltered(bh: Blackhole) {
        bh.consume(data.count { filterLoad(it) })
    }

    //Benchmark
    fun countFilteredSome(bh: Blackhole) {
        bh.consume(data.count { filterSome(it) })
    }

    //Benchmark
    fun countFilteredPrime(bh: Blackhole) {
        val res = data.count { filterPrime(it) }
        bh.consume(res)
    }

    //Benchmark
    fun countFilteredLocal(bh: Blackhole) {
        bh.consume(data.cnt { filterLoad(it) })
    }

    //Benchmark
    fun countFilteredSomeLocal(bh: Blackhole) {
        bh.consume(data.cnt { filterSome(it) })
    }

    //Benchmark
    fun reduce(bh: Blackhole) {
        bh.consume(data.fold(0) { acc, it -> if (filterLoad(it)) acc + 1 else acc })
    }
}

