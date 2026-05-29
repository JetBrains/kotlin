/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.ring

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference
import kotlin.random.Random
import kotlinx.benchmark.*
import kotlinx.cinterop.StableRef
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val BENCHMARK_SIZE = 10000

private const val REPEAT_COUNT = BENCHMARK_SIZE
private const val REFERENCES_COUNT = 3

private class Data(var x: Int)

private class ReferenceWrapper private constructor(
    data: Data
) {
    @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
    private val weak = WeakReference(data)
    private val strong = StableRef.create(data)

    val value: Int
        get() {
            @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
            val ref: Data? = weak.value
            if (ref == null) {
                return 0
            }
            return ref.x
        }

    fun dispose() {
        strong.dispose()
    }

    companion object {
        fun create(rnd: Random) = ReferenceWrapper(Data(rnd.nextInt(1000) + 1))
    }
}

private fun ReferenceWrapper.stress(): Int {
    var sum = 0
    for (i in 1..REPEAT_COUNT) {
        sum += this.value
    }
    return sum
}

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class WeakRefBenchmark : SkipWhenBaseOnly() {
    // Use the same seed for reproducibility
    private val rnd = Random(85140)

    private val weight = Array(BENCHMARK_SIZE) { ReferenceWrapper.create(rnd) }

    private val aliveRef = ReferenceWrapper.create(rnd)
    private val deadRef = ReferenceWrapper.create(rnd).apply {
        dispose()
        GC.collect()
    }

    // Access alive reference.
    @Benchmark
    fun aliveReference(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(aliveRef.stress())
    }

    // Access dead reference.
    @Benchmark
    fun deadReference(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(deadRef.stress())
    }

    // Access reference that is nulled out in the middle.
    @Benchmark
    fun dyingReference(bh: Blackhole) {
        skipWhenBaseOnly()
        val ref = ReferenceWrapper.create(rnd)

        ref.dispose()
        GC.schedule()

        bh.consume(ref.stress())
    }

    @TearDown
    fun clean() {
        weight.forEach { it.dispose() }
    }
}
