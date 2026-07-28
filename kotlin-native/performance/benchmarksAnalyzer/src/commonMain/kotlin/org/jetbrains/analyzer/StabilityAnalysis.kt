/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.analyzer

import org.jetbrains.report.BenchmarkResult
import org.jetbrains.report.MeanVarianceBenchmark
import kotlin.math.abs
import kotlin.math.sqrt

data class BenchmarkStability(
        val name: String,
        val samples: Int,
        val warmups: Int,
        val coefficientOfVariation: Double,
        val confidenceInterval: Double,
        val measurementDrift: Double,
        val recommendedIterations: Int,
        val needsMoreWarmups: Boolean,
        val stable: Boolean,
)

private fun tCritical95(sampleCount: Int): Double {
    val values = doubleArrayOf(
            12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228,
            2.201, 2.179, 2.160, 2.145, 2.131, 2.120, 2.110, 2.101, 2.093, 2.086,
            2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048, 2.045,
    )
    return values.getOrElse(sampleCount - 2) { 1.96 }
}

private fun relativePercent(value: Double, mean: Double): Double =
        if (mean == 0.0) Double.POSITIVE_INFINITY else abs(value / mean) * 100.0

private fun recommendedIterations(coefficientOfVariation: Double, targetRelativeError: Double): Int {
    if (!coefficientOfVariation.isFinite()) return Int.MAX_VALUE
    for (samples in 2..10_000) {
        val error = tCritical95(samples) * coefficientOfVariation / sqrt(samples.toDouble())
        if (error <= targetRelativeError) return samples
    }
    return Int.MAX_VALUE
}

fun analyzeBenchmarkStability(
        benchmarks: Map<String, List<BenchmarkResult>>,
        targetRelativeError: Double = 1.0,
): List<BenchmarkStability> {
    require(targetRelativeError.isFinite() && targetRelativeError > 0.0) {
        "Target relative error must be finite and positive"
    }
    return benchmarks.mapNotNull { (name, results) ->
        val samples = results
                .filter {
                    it.metric == BenchmarkResult.Metric.EXECUTION_TIME &&
                            it.status == BenchmarkResult.Status.PASSED &&
                            it !is MeanVarianceBenchmark
                }
                .sortedBy { it.repeat }
        if (samples.size < 2) return@mapNotNull null

        val scores = samples.map { it.score }
        val mean = scores.average()
        val standardDeviation = sqrt(scores.sumOf { (it - mean) * (it - mean) } / (scores.size - 1))
        val coefficientOfVariation = relativePercent(standardDeviation, mean)
        val confidenceInterval = tCritical95(scores.size) * coefficientOfVariation / sqrt(scores.size.toDouble())

        val middle = scores.size / 2
        val firstHalf = scores.take(middle).average()
        val secondHalf = scores.takeLast(middle).average()
        val measurementDrift = relativePercent(secondHalf - firstHalf, mean)
        val recommendedIterations = maxOf(
                scores.size,
                recommendedIterations(coefficientOfVariation, targetRelativeError),
        )

        BenchmarkStability(
                name = name,
                samples = scores.size,
                warmups = samples.first().warmup,
                coefficientOfVariation = coefficientOfVariation,
                confidenceInterval = confidenceInterval,
                measurementDrift = measurementDrift,
                recommendedIterations = recommendedIterations,
                needsMoreWarmups = measurementDrift > maxOf(targetRelativeError, confidenceInterval),
                stable = confidenceInterval <= targetRelativeError && measurementDrift <= targetRelativeError,
        )
    }.sortedWith(compareBy<BenchmarkStability> { it.stable }.thenByDescending { it.confidenceInterval }.thenBy { it.name })
}

fun renderStabilityReport(results: List<BenchmarkStability>, targetRelativeError: Double): String = buildString {
    appendLine("Benchmark stability (95% confidence, target ±${targetRelativeError.format(2)}%)")
    appendLine("================================================================================")
    if (results.isEmpty()) {
        appendLine("No raw execution-time measurements with at least two samples were found.")
    }
    results.forEach { result ->
        val recommendation = when {
            result.stable -> "stable"
            result.needsMoreWarmups && result.recommendedIterations > result.samples ->
                "increase warmups and use ${result.recommendedIterations.renderCount()} measurement iterations"

            result.needsMoreWarmups -> "increase warmups"
            else -> "use ${result.recommendedIterations.renderCount()} measurement iterations"
        }
        appendLine(result.name)
        appendLine(
                "  warmups=${result.warmups}, samples=${result.samples}, CV=${result.coefficientOfVariation.format(2)}%, " +
                        "CI=±${result.confidenceInterval.format(2)}%, drift=${result.measurementDrift.format(2)}%: $recommendation"
        )
    }
}

private fun Int.renderCount(): String = if (this == Int.MAX_VALUE) "more than 10000" else toString()
