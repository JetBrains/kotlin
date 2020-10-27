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
 * This test checks work with long numbers using Fibonacci sequence
 *
 * NB: all three tests here work CRITICALLY (x4...x6) slower than their Java equivalents
 * The reason is iteration on a progression formed as max downTo min or min..max step s.
 * In case of a range min..max primitive types are used by the Kotlin compiler,
 * but when we have a progression it's used directly with its iterator and so.
 */

open class FibonacciBenchmark {

    //Benchmark
    fun calcClassic(): Long {
        var a = 1L
        var b = 2L
        val size = BENCHMARK_SIZE
        for (i in 0..size-1) {
            val next = a + b
            a = b
            b = next
        }
        return b
    }

    //Benchmark
    fun calc(): Long {
        // This test works CRITICALLY slower compared with java equivalent (05.03.2015)
        var a = 1L
        var b = 2L
        // Probably for with downTo is the reason of slowness
        for (i in BENCHMARK_SIZE downTo 1) {
            val next = a + b
            a = b
            b = next
        }
        return b
    }

    //Benchmark
    fun calcWithProgression(): Long {
        // This test works CRITICALLY slower compared with java equivalent (05.03.2015)
        var a = 1L
        var b = 2L
        // Probably for with step is the reason of slowness
        for (i in 1..2*BENCHMARK_SIZE-1 step 2) {
            val next = a + b
            a = b
            b = next
        }
        return b
    }

    //Benchmark
    fun calcSquare(): Long {
        // This test works CRITICALLY slower compared with java equivalent (05.03.2015)
        var a = 1L
        var b = 2L
        val s = BENCHMARK_SIZE.toLong()
        val limit = s*s
        // Probably for with downTo is the reason of slowness
        for (i in limit downTo 1) {
            val next = a + b
            a = b
            b = next
        }
        return b
    }
}