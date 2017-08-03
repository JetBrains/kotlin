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