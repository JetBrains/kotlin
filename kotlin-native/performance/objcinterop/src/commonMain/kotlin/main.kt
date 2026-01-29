/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly
import org.jetbrains.complexNumbers.*

@State(Scope.Benchmark)
class ComplexNumbersBenchmarkHideName : SkipWhenBaseOnly() {
    private val instance = ComplexNumbersBenchmark()

    @Benchmark
    fun sumComplex() {
        instance.sumComplex()
    }

    @Benchmark
    fun stringToObjC() {
        instance.stringToObjC()
    }

    @Benchmark
    fun stringFromObjC() {
        instance.stringFromObjC()
    }

    @Benchmark
    fun fft() {
        instance.fft()
    }

    @Benchmark
    fun generateNumbersSequence() {
        skipWhenBaseOnly()
        instance.generateNumbersSequence()
    }

    @Benchmark
    fun subComplex() {
        skipWhenBaseOnly()
        instance.subComplex()
    }

    @Benchmark
    fun classInheritance() {
        skipWhenBaseOnly()
        instance.classInheritance()
    }

    @Benchmark
    fun categoryMethods() {
        skipWhenBaseOnly()
        instance.categoryMethods()
    }

    @Benchmark
    fun invertFft() {
        skipWhenBaseOnly()
        instance.invertFft()
    }
}