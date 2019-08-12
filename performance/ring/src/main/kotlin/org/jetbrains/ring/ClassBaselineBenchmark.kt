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