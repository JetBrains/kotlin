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

import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val BENCHMARK_SIZE = 10000

/**
 * This class tests linked list performance
 * using prime number calculation algorithms
 */
@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class PrimeList {
    private var primes: MutableList<Int> = mutableListOf()

    @Benchmark
    fun calcDirect(bh: Blackhole) {
        primes.clear()
        primes.add(2)
        var i = 3
        while (i <= BENCHMARK_SIZE) {
            var simple = true
            for (prime in primes) {
                if (prime * prime > i)
                    break
                if (i % prime == 0) {
                    simple = false
                    break
                }
            }
            if (simple)
                primes.add(i)
            i += 2
        }
        bh.consume(primes)
    }

    @Benchmark
    fun calcEratosthenes(bh: Blackhole) {
        primes.clear()
        primes.addAll(2..BENCHMARK_SIZE)
        var i = 0
        while (i < primes.size) {
            val divisor = primes[i]
            primes.removeAll { it -> it > divisor && it % divisor == 0 }
            i++
        }
        bh.consume(primes)
    }
}
