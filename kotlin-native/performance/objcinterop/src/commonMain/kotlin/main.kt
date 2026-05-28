/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly
import org.jetbrains.complexNumbers.*

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ComplexNumbersBenchmarkHideName : SkipWhenBaseOnly() {
    private val instance = ComplexNumbersBenchmark()

    @Benchmark
    fun sumComplex(bh: Blackhole) {
        instance.sumComplex(bh)
    }

    @Benchmark
    fun stringToObjC(bh: Blackhole) {
        instance.stringToObjC(bh)
    }

    @Benchmark
    fun stringFromObjC(bh: Blackhole) {
        instance.stringFromObjC(bh)
    }

    @Benchmark
    fun fft(bh: Blackhole) {
        instance.fft(bh)
    }

    @Benchmark
    fun generateNumbersSequence(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.generateNumbersSequence(bh)
    }

    @Benchmark
    fun subComplex(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.subComplex(bh)
    }

    @Benchmark
    fun classInheritance(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.classInheritance(bh)
    }

    @Benchmark
    fun categoryMethods(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.categoryMethods(bh)
    }

    @Benchmark
    fun invertFft(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.invertFft(bh)
    }
}
