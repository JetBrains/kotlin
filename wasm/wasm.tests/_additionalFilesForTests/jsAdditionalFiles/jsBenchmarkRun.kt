/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.time.TimeSource

@JsFun("writeFile")
external fun jsWrite(filename: String, content: String)

@JsExport
fun runBenchmark(): String {
    val warmupIterations = 30
    val benchmarkIterations = 50

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

    val n = benchmarkIterations
    val trimCount = (n * 0.05).toInt().coerceAtLeast(1)
    val trimmed = sortedTimes.subList(trimCount, benchmarkIterations - trimCount)
    val tn = trimmed.size

    val min = sortedTimes.first()
    val max = sortedTimes.last()

    // Proper median (average of two middle values for even count)
    val median = if (tn % 2 == 1) {
        trimmed[tn / 2]
    } else {
        (trimmed[tn / 2 - 1] + trimmed[tn / 2]) / 2
    }

    val mean = trimmed.sum() / tn

    // Population stddev; use Double accumulation to avoid Long overflow
    val variance = trimmed.map { d -> val diff = (d - mean).toDouble(); diff * diff }.sum() / tn
    val stddev = kotlin.math.sqrt(variance).toLong()

    // p90: floor(0.90 * tn), clamped
    val p90Index = (0.90 * tn).toInt().coerceIn(0, tn - 1)
    val p90 = trimmed[p90Index]

    val cv = if (mean != 0L) stddev.toDouble() / mean.toDouble() * 100.0 else 0.0

    val statsContent = buildString {
        appendLine("Benchmark Results (nanoseconds):")
        appendLine("  Iterations: $benchmarkIterations")
        appendLine("  Min: $min")
        appendLine("  Max: $max")
        appendLine("  Trimmed Mean: $mean")
        appendLine("  Median: $median")
        appendLine("  Stddev: $stddev")
        appendLine("  P90: $p90")
        appendLine("  CV: $cv")
    }

    println(statsContent)

    jsWrite("result.txt", statsContent)

    return "OK"
}