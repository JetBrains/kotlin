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

import org.jetbrains.report.BenchmarkResult
import org.jetbrains.kliopt.*


abstract class Launcher {
    abstract val benchmarks: BenchmarksCollection

    fun add(name: String, benchmark: AbstractBenchmarkEntry) {
        benchmarks[name] = benchmark
    }

    fun runBenchmark(benchmarkInstance: Any?, benchmark: AbstractBenchmarkEntry, repeatNumber: Int): Long {
        var i = repeatNumber
        return if (benchmark is BenchmarkEntryWithInit) {
            cleanup()
            measureNanoTime {
                while (i-- > 0) benchmark.lambda(benchmarkInstance!!)
                cleanup()
            }
        } else {
            cleanup()
            measureNanoTime {
                if (benchmark is BenchmarkEntry) {
                    while (i-- > 0) benchmark.lambda()
                    cleanup()
                }
            }
        }
    }

    fun launch(numWarmIterations: Int,
               numberOfAttempts: Int,
               prefix: String = "",
               filters: Collection<String>? = null,
               filterRegexes: Collection<String>? = null): List<BenchmarkResult> {
        val regexes = filterRegexes?.map { it.toRegex() } ?: listOf()
        val filterSet = filters?.toHashSet() ?: hashSetOf()
        // Filter benchmarks using given filters, or run all benchmarks if none were given.
        val runningBenchmarks = if (filterSet.isNotEmpty() || regexes.isNotEmpty()) {
            benchmarks.filterKeys { benchmark -> benchmark in filterSet || regexes.any { it.matches(benchmark) } }
        } else benchmarks
        if (runningBenchmarks.isEmpty())
            error("No matching benchmarks found")
        val benchmarkResults = mutableListOf<BenchmarkResult>()
        for ((name, benchmark) in runningBenchmarks) {
            val benchmarkInstance = (benchmark as? BenchmarkEntryWithInit)?.ctor?.invoke()
            var i = numWarmIterations
            runBenchmark(benchmarkInstance, benchmark, i)
            var autoEvaluatedNumberOfMeasureIteration = 1
            while (true) {
                var j = autoEvaluatedNumberOfMeasureIteration
                val time = runBenchmark(benchmarkInstance, benchmark, j)
                if (time >= 100L * 1_000_000) // 100ms
                    break
                autoEvaluatedNumberOfMeasureIteration *= 2
            }
            val samples = DoubleArray(numberOfAttempts)
            for (k in samples.indices) {
                i = autoEvaluatedNumberOfMeasureIteration
                val time = runBenchmark(benchmarkInstance, benchmark, i)
                val scaledTime = time * 1.0 / autoEvaluatedNumberOfMeasureIteration
                samples[k] = scaledTime
                // Save benchmark object
                benchmarkResults.add(BenchmarkResult("$prefix$name", BenchmarkResult.Status.PASSED,
                        scaledTime / 1000, BenchmarkResult.Metric.EXECUTION_TIME, scaledTime / 1000,
                        k + 1, numWarmIterations))
            }
        }
        return benchmarkResults
    }

    fun benchmarksListAction(argParser: ArgParser) {
        benchmarks.keys.forEach {
            println(it)
        }
    }
}

object BenchmarksRunner {
    fun parse(args: Array<String>, benchmarksListAction: (ArgParser)->Unit): ArgParser? {
        val actions = mapOf("list" to Action(
                benchmarksListAction,
                ArgParser(listOf<OptionDescriptor>()))
        )
        val options = listOf(
                OptionDescriptor(ArgType.Int(), "warmup", "w", "Number of warm up iterations", "20"),
                OptionDescriptor(ArgType.Int(), "repeat", "r", "Number of each benchmark run", "60"),
                OptionDescriptor(ArgType.String(), "prefix", "p", "Prefix added to benchmark name", ""),
                OptionDescriptor(ArgType.String(), "output", "o", "Output file"),
                OptionDescriptor(ArgType.String(), "filter", "f", "Benchmark to run", isMultiple = true),
                OptionDescriptor(ArgType.String(), "filterRegex", "fr", "Benchmark to run, described by a regular expression", isMultiple = true)
        )

        // Parse args.
        val argParser = ArgParser(options, actions = actions)
        return if (argParser.parse(args)) argParser else null
    }

    fun collect(results: List<BenchmarkResult>, parser: ArgParser) {
        JsonReportCreator(results).printJsonReport(parser.get<String>("output"))
    }

    fun runBenchmarks(args: Array<String>,
                      run: (parser: ArgParser) -> List<BenchmarkResult>,
                      parseArgs: (args: Array<String>, benchmarksListAction: (ArgParser)->Unit) -> ArgParser? = this::parse,
                      collect: (results: List<BenchmarkResult>, parser: ArgParser) -> Unit = this::collect,
                      benchmarksListAction: (ArgParser)->Unit) {
        val parser = parseArgs(args, benchmarksListAction)
        parser?.let {
            val results = run(parser)
            collect(results, parser)
        }
    }
}
