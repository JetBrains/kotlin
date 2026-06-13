/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the licenses/LICENSE.txt file.
 */

import kotlinx.benchmark.*

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class NumericalHideName {
    @Benchmark
    fun BellardPi(bh: Blackhole) {
        var result = 0
        for (n in 1 .. 1000 step 9) {
            result += pi_nth_digit(n)
        }
        bh.consume(result)
    }

    @Benchmark
    fun BellardPiCinterop(bh: Blackhole) {
        var result = 0
        for (n in 1 .. 1000 step 9) {
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            result += cinterop.pi_nth_digit(n)
        }
        bh.consume(result)
    }
}
