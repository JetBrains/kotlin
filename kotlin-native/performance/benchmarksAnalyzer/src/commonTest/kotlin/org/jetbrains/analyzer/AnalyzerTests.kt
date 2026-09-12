/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.analyzer

import kotlin.test.*
import kotlin.math.abs
import org.jetbrains.report.BenchmarkResult
import org.jetbrains.report.MeanVarianceBenchmark

class AnalyzerTests {
    private val eps = 0.000001

    private fun createMeanVarianceBenchmarks(): Pair<MeanVarianceBenchmark, MeanVarianceBenchmark> {
        val first = MeanVarianceBenchmark("testBenchmark", BenchmarkResult.Status.PASSED, 9.0, BenchmarkResult.Metric.EXECUTION_TIME, 9.0, 10, 10, 0.0001)
        val second = MeanVarianceBenchmark("testBenchmark", BenchmarkResult.Status.PASSED, 10.0, BenchmarkResult.Metric.EXECUTION_TIME, 10.0, 10, 10, 0.0001)

        return Pair(first, second)
    }

    @Test
    fun testGeoMean() {
        val numbers = listOf(4.0, 6.0, 9.0)
        val value = geometricMean(numbers)
        val expected = 6.0
        assertTrue(abs(value - expected) < eps)
    }

    @Test
    fun testComputeMeanVariance() {
        val numbers = listOf(10.1, 10.2, 10.3)
        val value = computeMeanVariance(numbers)
        val expectedMean = 10.2
        val expectedVariance = 0.07872455
        assertTrue(abs(value.mean - expectedMean) < eps)
        assertTrue(abs(value.variance - expectedVariance) < eps)
    }

    @Test
    fun calcPercentageDiff() {
        val inputs = createMeanVarianceBenchmarks()

        val percent = inputs.first.calcPercentageDiff(inputs.second)
        val expectedMean = -9.99809998
        val expectedVariance = 0.0021
        assertTrue(abs(percent.mean - expectedMean) < eps)
        //assertTrue(abs(percent.variance - expectedVariance) < eps)
    }

    @Test
    fun calcRatio() {
        val inputs = createMeanVarianceBenchmarks()

        val ratio = inputs.first.calcRatio(inputs.second)
        val expectedMean = 0.9
        val expectedVariance = 0.00001899

        assertTrue(abs(ratio.mean - expectedMean) < eps)
        assertTrue(abs(ratio.variance - expectedVariance) < eps)
    }

    private fun samples(vararg scores: Double, warmups: Int = 5): Map<String, List<BenchmarkResult>> = mapOf(
            "benchmark" to scores.mapIndexed { index, score ->
                BenchmarkResult(
                        "benchmark", BenchmarkResult.Status.PASSED, score, BenchmarkResult.Metric.EXECUTION_TIME,
                        runtimeInUs = score, repeat = index + 1, warmup = warmups,
                )
            }
    )

    @Test
    fun stableBenchmarkNeedsNoChanges() {
        val result = analyzeBenchmarkStability(samples(10.0, 10.0, 10.0, 10.0)).single()

        assertTrue(result.stable)
        assertEquals(result.samples, result.recommendedIterations)
        assertFalse(result.needsMoreWarmups)
    }

    @Test
    fun noisyBenchmarkNeedsMoreIterations() {
        val result = analyzeBenchmarkStability(samples(8.0, 12.0, 9.0, 11.0)).single()

        assertFalse(result.stable)
        assertTrue(result.recommendedIterations > result.samples)
    }

    @Test
    fun measurementDriftSuggestsMoreWarmups() {
        val result = analyzeBenchmarkStability(samples(8.0, 8.0, 12.0, 12.0)).single()

        assertFalse(result.stable)
        assertTrue(result.needsMoreWarmups)
    }

    @Test
    fun nonExecutionMetricsAreIgnored() {
        val measurements = samples(10.0, 10.0, 10.0, 10.0).getValue("benchmark")
        val codeSize = BenchmarkResult(
                "benchmark", BenchmarkResult.Status.PASSED, 1_000_000.0, BenchmarkResult.Metric.CODE_SIZE,
                runtimeInUs = 0.0, repeat = 0, warmup = 0,
        )

        val result = analyzeBenchmarkStability(mapOf("benchmark" to measurements + codeSize)).single()

        assertTrue(result.stable)
        assertEquals(measurements.size, result.samples)
    }

    @Test
    fun targetRelativeErrorMustBeFiniteAndPositive() {
        assertFailsWith<IllegalArgumentException> {
            analyzeBenchmarkStability(samples(10.0, 10.0), Double.POSITIVE_INFINITY)
        }
    }
}
