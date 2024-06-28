/*
 * Copyright 2010-2024 JetBrains s.r.o.
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

fun makeIterable(): Iterable<Int> = (0..BENCHMARK_SIZE)

internal inline fun sum(iterable: Iterable<Int>): Int {
    var sum = 0
    for (x in iterable) {
        sum += x
    }
    return sum
}

open class IteratorBenchmark {
    val iterable = makeIterable()

    fun baseline() {
        var sum = 0
        for (i in 0..BENCHMARK_SIZE) {
            sum += i
        }
        Blackhole.consume(sum)
    }

    fun concreteIterable() {
        Blackhole.consume(sum((0..BENCHMARK_SIZE)))
    }

    fun abstractIterable() {
        Blackhole.consume(sum(iterable))
    }
}