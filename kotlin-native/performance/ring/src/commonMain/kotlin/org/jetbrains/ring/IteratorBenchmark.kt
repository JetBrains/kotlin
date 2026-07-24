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

import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val BENCHMARK_SIZE = 10000

fun makeIterable(): Iterable<Int> = (0..BENCHMARK_SIZE)

@Suppress("NOTHING_TO_INLINE")
internal inline fun sum(iterable: Iterable<Int>): Int {
    var sum = 0
    for (x in iterable) {
        sum += x
    }
    return sum
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Iterator : SkipWhenBaseOnly() {
    val iterable = makeIterable()

    @Benchmark
    fun baseline(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum = 0
        for (i in 0..BENCHMARK_SIZE) {
            sum += i
        }
        bh.consume(sum)
    }

    @Benchmark
    fun concreteIterable(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(sum((0..BENCHMARK_SIZE)))
    }

    @Benchmark
    fun abstractIterable(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(sum(iterable))
    }
}
