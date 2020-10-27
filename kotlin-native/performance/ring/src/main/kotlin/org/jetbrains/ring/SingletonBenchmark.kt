/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.ring

import org.jetbrains.benchmarksLauncher.Blackhole
import org.jetbrains.benchmarksLauncher.Random

private object A {
    val a = Random.nextInt(100)
}

open class SingletonBenchmark {
    init {
        // Make sure A is initialized.
        Blackhole.consume(A.a)
    }

    // Benchmark
    fun access() {
        for (i in 0 until BENCHMARK_SIZE) {
            Blackhole.consume(A.a)
        }
    }
}
