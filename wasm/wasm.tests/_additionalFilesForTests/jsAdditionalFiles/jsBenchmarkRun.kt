/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.time.TimeSource

@JsFun("writeFile")
external fun jsWrite(filename: String, content: String)

@JsExport
fun runBenchmark(): String {
    val warmupIterations = 20
    val benchmarkIterations = 30

    // Warmup phase
    for (i in 0 until warmupIterations) {
        box()
    }

    // Benchmark phase
    val times = mutableListOf<Long>()

    for (i in 0 until benchmarkIterations) {
        val startMark = TimeSource.Monotonic.markNow()
        val boxResult = box()
        val duration = startMark.elapsedNow()

        val isOk = boxResult == "OK"
        if (!isOk) {
            return boxResult
        }

        times.add(duration.inWholeNanoseconds)
    }

    // Calculate statistics
    val sortedTimes = times.sorted()
    val median = sortedTimes[sortedTimes.size / 2]
    val average = times.sum() / times.size
    val min = sortedTimes.first()
    val max = sortedTimes.last()

    val statsContent = buildString {
        appendLine("Benchmark Results (nanoseconds):")
        appendLine("  Iterations: $benchmarkIterations")
        appendLine("  Min: $min")
        appendLine("  Max: $max")
        appendLine("  Average: $average")
        appendLine("  Median: $median")
    }

    println(statsContent)

    jsWrite("result.txt", statsContent)

    return "OK"
}