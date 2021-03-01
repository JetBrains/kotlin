/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.analyzer

import org.jetbrains.report.BenchmarkResult
import org.jetbrains.report.MeanVariance
import org.jetbrains.report.MeanVarianceBenchmark
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

val MeanVariance.description: String
    get() {
        val format = { number: Double -> number.format(2) }
        return "${format(mean)} ± ${format(variance)}"
    }

val MeanVarianceBenchmark.description: String
    get() = "${score.format()} ± ${variance.format()}"

// Calculate difference in percentage compare to another.
fun MeanVarianceBenchmark.calcPercentageDiff(other: MeanVarianceBenchmark): MeanVariance {
    if (score == 0.0 && variance == 0.0 && other.score == 0.0 && other.variance == 0.0)
        return MeanVariance(score, variance)
    assert(other.score >= 0 &&
            other.variance >= 0 &&
            (other.score - other.variance != 0.0 || other.score == 0.0),
            { "Mean and variance should be positive and not equal!" })

    // Analyze intervals. Calculate difference between border points.
    val (bigValue, smallValue) = if (score > other.score) Pair(this, other) else Pair(other, this)
    val bigValueIntervalStart = bigValue.score - bigValue.variance
    val bigValueIntervalEnd = bigValue.score + bigValue.variance
    val smallValueIntervalStart = smallValue.score - smallValue.variance
    val smallValueIntervalEnd = smallValue.score + smallValue.variance
    if (smallValueIntervalEnd > bigValueIntervalStart) {
        // Interval intersect.
        return MeanVariance(0.0, 0.0)
    }
    val mean = ((smallValueIntervalEnd - bigValueIntervalStart) / bigValueIntervalStart) *
            (if (score > other.score) -1 else 1)

    val maxValueChange = ((bigValueIntervalEnd - smallValueIntervalEnd) / bigValueIntervalEnd)
    val minValueChange = ((bigValueIntervalStart - smallValueIntervalStart) / bigValueIntervalStart)
    val variance = abs(abs(mean) - max(minValueChange, maxValueChange))
    return MeanVariance(mean * 100, variance * 100)
}

// Calculate ratio value compare to another.
fun MeanVarianceBenchmark.calcRatio(other: MeanVarianceBenchmark): MeanVariance {
    if (other.score == 0.0 && other.variance == 0.0)
        return MeanVariance(1.0, 0.0)
    assert(other.score >= 0 &&
            other.variance >= 0 &&
            (other.score - other.variance != 0.0 || other.score == 0.0),
            { "Mean and variance should be positive and not equal!" })
    val mean = if (other.score != 0.0) (score / other.score) else 0.0
    val minRatio = (score - variance) / (other.score + other.variance)
    val maxRatio = (score + variance) / (other.score - other.variance)
    val ratioConfInt = min(abs(minRatio - mean), abs(maxRatio - mean))
    return MeanVariance(mean, ratioConfInt)
}

fun geometricMean(values: Collection<Double>, totalNumber: Int = values.size) =
        with(values.asSequence().filter { it != 0.0 }) {
            if (count() == 0 || totalNumber == 0) {
                0.0
            } else {
                map { it.pow(1.0 / totalNumber) }.reduce { a, b -> a * b }
            }
        }

fun computeMeanVariance(samples: List<Double>): MeanVariance {
    val removedBroadSamples = 0.2
    val zStar = 1.67    // Critical point for 90% confidence of normal distribution.
    // Skip several minimal and maximum values.
    val filteredSamples = if (samples.size >= 1/removedBroadSamples) {
         samples.sorted().subList((samples.size * removedBroadSamples).toInt(),
                samples.size - (samples.size * removedBroadSamples).toInt())
    } else {
        samples
    }

    val mean = filteredSamples.sum() / filteredSamples.size
    val variance = samples.indices.sumByDouble {
        (samples[it] - mean) * (samples[it] - mean)
    } / samples.size
    val confidenceInterval = sqrt(variance / samples.size) * zStar
    return MeanVariance(mean, confidenceInterval)
}

// Calculate average results for benchmarks (each benchmark can be run several times).
fun collectMeanResults(benchmarks: Map<String, List<BenchmarkResult>>): BenchmarksTable {
    return benchmarks.map { (name, resultsSet) ->
        val repeatedSequence = IntArray(resultsSet.size)
        var metric = BenchmarkResult.Metric.EXECUTION_TIME
        var currentStatus = BenchmarkResult.Status.PASSED
        var currentWarmup = -1

        // Results can be already processed.
        if (resultsSet[0] is MeanVarianceBenchmark) {
            assert(resultsSet.size == 1) { "Several MeanVarianceBenchmark instances." }
            name to resultsSet[0] as MeanVarianceBenchmark
        } else {
            // Collect common benchmark values and check them.
            resultsSet.forEachIndexed { index, result ->
                // If there was at least one failure, summary is marked as failure.
                if (result.status == BenchmarkResult.Status.FAILED) {
                    currentStatus = result.status
                }
                repeatedSequence[index] = result.repeat
                if (currentWarmup != -1)
                    if (result.warmup != currentWarmup)
                        println("Check data consistency. Warmup value for benchmark '${result.name}' differs.")
                currentWarmup = result.warmup
                metric = result.metric
            }

            repeatedSequence.sort()
            // Check if there are missed loop during running benchmarks.
            repeatedSequence.forEachIndexed { index, element ->
                if (index != 0)
                    if ((element - repeatedSequence[index - 1]) != 1)
                        println("Check data consistency. For benchmark '$name' there is no run" +
                                " between ${repeatedSequence[index - 1]} and $element.")
            }

            // Create mean and variance benchmarks result.
            val scoreMeanVariance = computeMeanVariance(resultsSet.map { it.score })
            val runtimeInUsMeanVariance = computeMeanVariance(resultsSet.map { it.runtimeInUs })
            val meanBenchmark = MeanVarianceBenchmark(name, currentStatus, scoreMeanVariance.mean, metric,
                    runtimeInUsMeanVariance.mean, repeatedSequence[resultsSet.size - 1],
                    currentWarmup, scoreMeanVariance.variance)
            name to meanBenchmark
        }
    }.toMap()
}

fun collectBenchmarksDurations(benchmarks: Map<String, List<BenchmarkResult>>): Map<String, Double> =
        benchmarks.map { (name, resultsSet) ->
            name to resultsSet.sumByDouble { it.runtimeInUs }
        }.toMap()