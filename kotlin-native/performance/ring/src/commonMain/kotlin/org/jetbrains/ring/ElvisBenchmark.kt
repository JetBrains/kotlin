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

import kotlin.random.Random
import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val BENCHMARK_SIZE = 10000

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Elvis : SkipWhenBaseOnly() {
    // Use the same seed for reproducibility
    private val rnd = Random(785)

    class Value(var value: Int)

    var array : Array<Value?> = arrayOf()

    init {
        array = Array(BENCHMARK_SIZE) {
            if (rnd.nextInt(BENCHMARK_SIZE) < BENCHMARK_SIZE / 10) null else Value(rnd.nextInt(100))
        }
    }

    @Benchmark
    fun testElvis(bh: Blackhole) {
        var result = 0
        for (obj in array) {
            result += obj?.value ?: 0
        }
        bh.consume(result)
    }

    class Composite(val x : Int, val y : Composite?)

    private val composites = Array(BENCHMARK_SIZE) {
        Composite(rnd.nextInt(100), Composite(rnd.nextInt(100), null))
    }

    fun check(a : Composite?) : Int {
        return a?.y?.x ?: (a?.x ?: 3)
    }

    @Benchmark
    fun testCompositeElvis(bh: Blackhole) {
        skipWhenBaseOnly()
        var result = 0
        for (composite in composites) {
            result += check(composite)
        }
        bh.consume(result)
    }
}
