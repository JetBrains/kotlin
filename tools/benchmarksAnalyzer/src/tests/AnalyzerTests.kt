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

import kotlin.test.*
import kotlin.math.abs
import org.jetbrains.report.BenchmarkResult

class AnalyzerTests {
    private val eps = 0.000001

    private fun createMeanVarianceBenchmarks(): Pair<MeanVarianceBenchmark, MeanVarianceBenchmark> {
        val firstMean = BenchmarkResult("testBenchmark", BenchmarkResult.Status.PASSED, 9.0, BenchmarkResult.Metric.EXECUTION_TIME, 9.0, 10, 10)
        val firstVariance = BenchmarkResult("testBenchmark", BenchmarkResult.Status.PASSED, 0.0001, BenchmarkResult.Metric.EXECUTION_TIME, 0.0001, 10, 10)
        val first = MeanVarianceBenchmark(firstMean, firstVariance)

        val secondMean = BenchmarkResult("testBenchmark", BenchmarkResult.Status.PASSED, 10.0, BenchmarkResult.Metric.EXECUTION_TIME, 10.0, 10, 10)
        val secondVariance = BenchmarkResult("testBenchmark", BenchmarkResult.Status.PASSED, 0.0001, BenchmarkResult.Metric.EXECUTION_TIME, 0.0001, 10, 10)
        val second = MeanVarianceBenchmark(secondMean, secondVariance)

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
}
