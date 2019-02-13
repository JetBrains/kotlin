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

/**
 * Created by Mikhail.Glukhikh on 06/03/2015.
 *
 * A benchmark for a single abstract method based on a string comparison
 */

open class AbstractMethodBenchmark {

    private val arr: List<String> = zdf_win
    private val sequence = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"

    private val sequenceMap = HashMap<Char, Int>()

    init {
        var i = 0;
        for (ch in sequence) {
            sequenceMap[ch] = i++;
        }
    }

    //Benchmark
    fun sortStrings(): Set<String> {
        val res = arr.subList(0, if (BENCHMARK_SIZE < arr.size) BENCHMARK_SIZE else arr.size).toSet()
        return res
    }

    //Benchmark
    fun sortStringsWithComparator(): Set<String> {
        val res = mutableSetOf<String>()
        res.addAll(arr.subList(0, if (BENCHMARK_SIZE < arr.size) BENCHMARK_SIZE else arr.size))
        return res
    }
}

