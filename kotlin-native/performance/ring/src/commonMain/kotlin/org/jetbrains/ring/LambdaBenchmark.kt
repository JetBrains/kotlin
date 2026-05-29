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

var globalAddendum = 0

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Lambda : SkipWhenBaseOnly() {
    private inline fun <T> runLambda(x: () -> T): T = x()
    private fun <T> runLambdaNoInline(x: () -> T): T = x()

    init {
        // Use the same seed for reproducibility
        val rnd = Random(0)
        globalAddendum = rnd.nextInt(20)
    }

    @Benchmark
    fun noncapturingLambda(bh: Blackhole) {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambda { globalAddendum }
        }
        bh.consume(x)
    }

    @Benchmark
    fun noncapturingLambdaNoInline(bh: Blackhole) {
        skipWhenBaseOnly()
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambdaNoInline { globalAddendum }
        }
        bh.consume(x)
    }

    @Benchmark
    fun capturingLambda(bh: Blackhole) {
        val addendum = globalAddendum + 1
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambda { addendum }
        }
        bh.consume(x)
    }

    @Benchmark
    fun capturingLambdaNoInline(bh: Blackhole) {
        skipWhenBaseOnly()
        val addendum = globalAddendum + 1
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambdaNoInline { addendum }
        }
        bh.consume(x)
    }

    @Benchmark
    fun mutatingLambda(bh: Blackhole) {
        skipWhenBaseOnly()
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            runLambda { x += globalAddendum }
        }
        bh.consume(x)
    }

    @Benchmark
    fun mutatingLambdaNoInline(bh: Blackhole) {
        skipWhenBaseOnly()
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            runLambdaNoInline { x += globalAddendum }
        }
        bh.consume(x)
    }

    @Benchmark
    fun methodReference(bh: Blackhole) {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambda(::referenced)
        }
        bh.consume(x)
    }

    @Benchmark
    fun methodReferenceNoInline(bh: Blackhole) {
        skipWhenBaseOnly()
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambdaNoInline(::referenced)
        }
        bh.consume(x)
    }
}

private fun referenced(): Int {
    return globalAddendum
}
