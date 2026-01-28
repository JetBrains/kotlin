/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the licenses/LICENSE.txt file.
 */

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.benchmarksLauncher.*

@State(Scope.Benchmark)
class NumericalHideName {
    @Benchmark
    fun BellardPi() {
        konanBellardPi()
    }

    @Benchmark
    fun BellardPiCinterop() {
        clangBellardPi()
    }
}

fun konanBellardPi() {
    for (n in 1 .. 1000 step 9) {
        val result = pi_nth_digit(n)
        Blackhole.consume(result)
    }
}

fun clangBellardPi() {
    for (n in 1 .. 1000 step 9) {
        @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
        val result = cinterop.pi_nth_digit(n)
        Blackhole.consume(result)
    }
}
