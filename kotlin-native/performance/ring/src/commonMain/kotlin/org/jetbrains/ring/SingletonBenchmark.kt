/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.ring

import kotlin.random.Random
import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val BENCHMARK_SIZE = 1000

private object A {
    // Use the same seed for reproducibility
    private val rnd = Random(109)

    val a = rnd.nextInt(100)
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Singleton {
    init {
        // Make sure A is initialized.
        A.a
    }

    @Benchmark
    fun access(bh: Blackhole) {
        var result = 0
        for (i in 0 until BENCHMARK_SIZE) {
            result += A.a
        }
        bh.consume(result)
    }
}
