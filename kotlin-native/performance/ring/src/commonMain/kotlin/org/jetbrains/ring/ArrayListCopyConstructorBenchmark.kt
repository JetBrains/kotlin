/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.ring

import kotlinx.benchmark.*

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ArrayListCopyConstructorBenchmark {
    @Param(
            "ArrayList",
            "arrayListOf",
            // Name rationale: it implements to `List` but doesn't inherit `AbstractCollection` nor implements `RandomAccess`
            "ListWorstCase",
            "HashSet",
    )
    var sourceKind: String = ""

    @Param("0", "1", "2", "10", "100", "1000", "10000", "100000")
    var size: Int = -1

    private lateinit var calculation: () -> ArrayList<String>

    @Setup
    fun setUp() {
        val list = sizedArrayList(size)

        calculation = when (sourceKind) {
            "ArrayList" -> list.let { { ArrayList(it) } }
            "arrayListOf" -> list.toTypedArray().let { { arrayListOf(*it) } }
            "ListWorstCase" -> (object : List<String> by list {}).let { { ArrayList(it) } }
            "HashSet" -> HashSet(list).let { { ArrayList(it) } }
            else -> throw IllegalStateException("Unknown source kind: $sourceKind")
        }
    }

    @Benchmark
    fun benchmark(blackhole: Blackhole) {
        blackhole.consume(calculation())
    }

    private fun sizedArrayList(count: Int) = ArrayList<String>().apply {
        repeat(count) { index ->
            add(index.toString())
        }
    }
}
