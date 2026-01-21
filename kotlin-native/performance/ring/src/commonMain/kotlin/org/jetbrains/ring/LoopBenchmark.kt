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

import org.jetbrains.benchmarksLauncher.Blackhole

open class LoopBenchmark {
    lateinit var arrayList: List<Value>
    lateinit var array: Array<Value>

    init {
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