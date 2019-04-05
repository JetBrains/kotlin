/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.benchmarksLauncher

import kotlin.math.sqrt
import org.jetbrains.report.BenchmarkResult
import org.jetbrains.kliopt.*

abstract class Launcher(val numWarmIterations: Int, val numberOfAttempts: Int, val prefix: String = "") {
    class Results(val mean: Double, val variance: Double)

    abstract val benchmarks: BenchmarksCollection

    protected val benchmarkResults = mutableListOf<BenchmarkResult>()

    fun launch(benchmarksToRun: Collection<String>? = null): List<BenchmarkResult> {
        val runningBenchmarks = benchmarksToRun ?: benchmarks.keys
        runningBenchmarks.forEach {
            val benchmark = benchmarks[it]
            benchmark ?: error("Benchmark $it wasn't found!")
            var i = numWarmIterations
            while (i-- > 0) benchmark()
            cleanup()
            var autoEvaluatedNumberOfMeasureIteration = 1
            while (true) {
                var j = autoEvaluatedNumberOfMeasureIteration
                val time = measureNanoTime {
                    while (j-- > 0) {
                        benchmark()
                    }
                    cleanup()
                }
                if (time >= 100L * 1_000_000) // 100ms
                    break
                autoEvaluatedNumberOfMeasureIteration *= 2
            }
            val samples = DoubleArray(numberOfAttempts)
            for (k in samples.indices) {
                i = autoEvaluatedNumberOfMeasureIteration
                val time = measureNanoTime {
                    while (i-- > 0) {
                        benchmark()
                    }
                    cleanup()
                }
                val scaledTime = time * 1.0 / autoEvaluatedNumberOfMeasureIteration
                samples[k] = scaledTime
                // Save benchmark object
                benchmarkResults.add(BenchmarkResult("$prefix$it", BenchmarkResult.Status.PASSED,
                        scaledTime / 1000, BenchmarkResult.Metric.EXECUTION_TIME, scaledTime / 1000,
                        k + 1, numWarmIterations))
            }
        }
        return benchmarkResults
    }
}

object BenchmarksRunner {
    fun parse(args: Array<String>): ArgParser {
        val options = listOf(
                OptionDescriptor(ArgType.Int(), "warmup", "w", "Number of warm up iterations", "20"),
                OptionDescriptor(ArgType.Int(), "repeat", "r", "Number of each becnhmark run", "60"),
                OptionDescriptor(ArgType.String(), "prefix", "p", "Prefix added to benchmark name", ""),
                OptionDescriptor(ArgType.String(), "output", "o", "Output file"),
                OptionDescriptor(ArgType.String(), "filter", "f", "Benchmark to run", isMultiple = true)
        )

        // Parse args.
        val argParser = ArgParser(options)
        argParser.parse(args)
        return argParser
    }

    fun collect(results: List<BenchmarkResult>, parser: ArgParser) {
        parser.get<String>("output")?.let {
            JsonReportCreator(results).printJsonReport(it)
        }
    }

    fun runBenchmarks(args: Array<String>,
                      run: (parser: ArgParser) -> List<BenchmarkResult>,
                      parseArgs: (args: Array<String>) -> ArgParser = this::parse,
                      collect: (results: List<BenchmarkResult>, parser: ArgParser) -> Unit = this::collect) {
        val parser = parseArgs(args)
        val results = run(parser)
        collect(results, parser)
    }
}