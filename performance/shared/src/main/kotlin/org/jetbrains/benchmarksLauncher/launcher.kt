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
@file:UseExperimental(ExperimentalCli::class)
package org.jetbrains.benchmarksLauncher

import org.jetbrains.report.BenchmarkResult
import kotlinx.cli.*


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

    enum class LogLevel { DEBUG, OFF }

    class Logger(val level: LogLevel = LogLevel.OFF) {
         fun log(message: String, messageLevel: LogLevel = LogLevel.DEBUG, usePrefix: Boolean = true) {
            if (messageLevel == level) {
                if (usePrefix) {
                    printStderr("[$level][${currentTime()}] $message")
                } else {
                    printStderr("$message")
                }
            }
        }
    }

    fun launch(numWarmIterations: Int,
               numberOfAttempts: Int,
               prefix: String = "",
               filters: Collection<String>? = null,
               filterRegexes: Collection<String>? = null,
               verbose: Boolean): List<BenchmarkResult> {
        val logger = if (verbose) Logger(LogLevel.DEBUG) else Logger()
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
            logger.log("Warm up iterations for benchmark $name\n")
            runBenchmark(benchmarkInstance, benchmark, i)
            var autoEvaluatedNumberOfMeasureIteration = 1
            while (true) {
                var j = autoEvaluatedNumberOfMeasureIteration
                val time = runBenchmark(benchmarkInstance, benchmark, j)
                if (time >= 100L * 1_000_000) // 100ms
                    break
                autoEvaluatedNumberOfMeasureIteration *= 2
            }
            logger.log("Running benchmark $name ")
            val samples = DoubleArray(numberOfAttempts)
            for (k in samples.indices) {
                logger.log(".", usePrefix = false)
                i = autoEvaluatedNumberOfMeasureIteration
                val time = runBenchmark(benchmarkInstance, benchmark, i)
                val scaledTime = time * 1.0 / autoEvaluatedNumberOfMeasureIteration
                samples[k] = scaledTime
                // Save benchmark object
                benchmarkResults.add(BenchmarkResult("$prefix$name", BenchmarkResult.Status.PASSED,
                        scaledTime / 1000, BenchmarkResult.Metric.EXECUTION_TIME, scaledTime / 1000,
                        k + 1, numWarmIterations))
            }
            logger.log("\n", usePrefix = false)
        }
        return benchmarkResults
    }

    fun benchmarksListAction() {
        benchmarks.keys.forEach {
            println(it)
        }
    }
}

abstract class BenchmarkArguments(argParser: ArgParser)

class BaseBenchmarkArguments(argParser: ArgParser): BenchmarkArguments(argParser) {
    val warmup by argParser.option(ArgType.Int, shortName = "w", description = "Number of warm up iterations")
            .default(20)
    val repeat by argParser.option(ArgType.Int, shortName = "r", description = "Number of each benchmark run").
            default(60)
    val prefix by argParser.option(ArgType.String, shortName = "p", description = "Prefix added to benchmark name")
            .default("")
    val output by argParser.option(ArgType.String, shortName = "o", description = "Output file")
    val filter by argParser.option(ArgType.String, shortName = "f", description = "Benchmark to run").multiple()
    val filterRegex by argParser.option(ArgType.String, shortName = "fr",
            description = "Benchmark to run, described by a regular expression").multiple()
    val verbose by argParser.option(ArgType.Boolean, shortName = "v", description = "Verbose mode of running")
            .default(false)
}

object BenchmarksRunner {
    fun parse(args: Array<String>, benchmarksListAction: ()->Unit): BenchmarkArguments? {
        class List: Subcommand("list") {
            override fun execute() {
                benchmarksListAction()
            }
        }

        // Parse args.
        val argParser = ArgParser("benchmark")
        argParser.subcommands(List())
        val argumentsValues = BaseBenchmarkArguments(argParser)
        return if (argParser.parse(args).commandName == "benchmark") argumentsValues else null
    }

    fun collect(results: List<BenchmarkResult>, arguments: BenchmarkArguments) {
        if (arguments is BaseBenchmarkArguments) {
            JsonReportCreator(results).printJsonReport(arguments.output)
        }
    }

    fun runBenchmarks(args: Array<String>,
                      run: (parser: BenchmarkArguments) -> List<BenchmarkResult>,
                      parseArgs: (args: Array<String>, benchmarksListAction: ()->Unit) -> BenchmarkArguments? = this::parse,
                      collect: (results: List<BenchmarkResult>, arguments: BenchmarkArguments) -> Unit = this::collect,
                      benchmarksListAction: ()->Unit) {
        val arguments = parseArgs(args, benchmarksListAction)
        arguments?.let {
            val results = run(arguments)
            collect(results, arguments)
        }
    }
}
