/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.jetbrains.analyzer
import org.jetbrains.report.BenchmarkResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

// Entity to describe avarage values which conssists of mean and variance values.
data class MeanVariance(val mean: Double, val variance: Double) {
    override fun toString(): String {
        val format = { number: Double -> number.format(2)}
        return "${format(mean)} ± ${format(variance)}"
    }
}

// Composite benchmark which descibes avarage result for several runs and contains mean and variance value.
data class MeanVarianceBenchmark(val meanBenchmark: BenchmarkResult, val varianceBenchmark: BenchmarkResult) {

    // Calculate difference in percentage compare to another.
    fun calcPercentageDiff(other: MeanVarianceBenchmark): MeanVariance {
        assert(other.meanBenchmark.score >= 0 &&
                other.varianceBenchmark.score >= 0 &&
                other.meanBenchmark.score - other.varianceBenchmark.score != 0.0,
                { "Mean and variance should be positive and not equal!" })
        val exactMean = (meanBenchmark.score - other.meanBenchmark.score) / other.meanBenchmark.score
        // Analyze intervals. Calculate difference between border points.
        val (bigValue, smallValue) = if (meanBenchmark.score > other.meanBenchmark.score) Pair(this, other) else Pair(other, this)
        val bigValueIntervalStart = bigValue.meanBenchmark.score - bigValue.varianceBenchmark.score
        val bigValueIntervalEnd = bigValue.meanBenchmark.score + bigValue.varianceBenchmark.score
        val smallValueIntervalStart = smallValue.meanBenchmark.score - smallValue.varianceBenchmark.score
        val smallValueIntervalEnd = smallValue.meanBenchmark.score + smallValue.varianceBenchmark.score
        if (smallValueIntervalEnd > bigValueIntervalStart) {
            // Interval intersect.
            return MeanVariance(0.0, 0.0)
        }
        val mean = ((smallValueIntervalEnd - bigValueIntervalStart) / bigValueIntervalStart) *
                (if (meanBenchmark.score > other.meanBenchmark.score) -1 else 1)

        val maxValueChange = ((bigValueIntervalEnd - smallValueIntervalEnd) / bigValueIntervalEnd)
        val minValueChange =  ((bigValueIntervalStart - smallValueIntervalStart) / bigValueIntervalStart)
        val variance = abs(abs(mean) - max(minValueChange, maxValueChange))
        return MeanVariance(mean * 100, variance * 100)
    }

    // Calculate ratio value compare to another.
    fun calcRatio(other: MeanVarianceBenchmark): MeanVariance {
        assert(other.meanBenchmark.score >= 0 &&
                other.varianceBenchmark.score >= 0 &&
                other.meanBenchmark.score - other.varianceBenchmark.score != 0.0,
                { "Mean and variance should be positive and not equal!" })
        val mean = meanBenchmark.score / other.meanBenchmark.score
        val minRatio = (meanBenchmark.score - varianceBenchmark.score) / (other.meanBenchmark.score + other.varianceBenchmark.score)
        val maxRatio = (meanBenchmark.score + varianceBenchmark.score) / (other.meanBenchmark.score - other.varianceBenchmark.score)
        val ratioConfInt = min(abs(minRatio - mean), abs(maxRatio - mean))
        return MeanVariance(mean, ratioConfInt)
    }

    override fun toString(): String =
        "${meanBenchmark.score.format()} ± ${varianceBenchmark.score.format()}"

}

fun geometricMean(values: Collection<Double>, totalNumber: Int = values.size) =
    with(values.asSequence().filter { it != 0.0 }) {
        if (count() == 0) {
            0.0
        } else {
            map { it.pow(1.0 / totalNumber) }.reduce { a, b -> a * b }
        }
    }

fun computeMeanVariance(samples: List<Double>): MeanVariance {
    val zStar = 1.67    // Critical point for 90% confidence of normal distribution.
    val mean = samples.sum() / samples.size
    val variance = samples.indices.sumByDouble { (samples[it] - mean) * (samples[it] - mean) } / samples.size
    val confidenceInterval = sqrt(variance / samples.size) * zStar
    return MeanVariance(mean, confidenceInterval)
}

// Calculate avarage results for bencmarks (each becnhmark can be run several times).
fun collectMeanResults(benchmarks: Map<String, List<BenchmarkResult>>): BenchmarksTable {
    return benchmarks.map {(name, resultsSet) ->
        val repeatedSequence = IntArray(resultsSet.size)
        var metric = BenchmarkResult.Metric.EXECUTION_TIME
        var currentStatus = BenchmarkResult.Status.PASSED
        var currentWarmup = -1

        // Collect common becnhmark values and check them.
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
        val meanBenchmark = BenchmarkResult(name, currentStatus, scoreMeanVariance.mean, metric,
                runtimeInUsMeanVariance.mean, repeatedSequence[resultsSet.size - 1],
                currentWarmup)
        val varianceBenchmark = BenchmarkResult(name, currentStatus, scoreMeanVariance.variance, metric,
                runtimeInUsMeanVariance.variance, repeatedSequence[resultsSet.size - 1],
                currentWarmup)
        name to MeanVarianceBenchmark(meanBenchmark, varianceBenchmark)
    }.toMap()
}

fun collectBenchmarksDurations(benchmarks: Map<String, List<BenchmarkResult>>): Map<String, Double> =
        benchmarks.map { (name, resultsSet) ->
            name to resultsSet.sumByDouble { it.runtimeInUs }
        }.toMap()