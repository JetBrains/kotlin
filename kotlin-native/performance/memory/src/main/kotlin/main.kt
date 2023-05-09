/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.benchmarksLauncher.*
import kotlinx.cli.*
import kotlin.native.runtime.GC
import kotlin.native.runtime.GCInfo
import kotlin.native.ref.*
import org.jetbrains.benchmarksLauncher.Blackhole
import org.jetbrains.membench.*
import org.jetbrains.report.BenchmarkResult


fun benchMark(provider: WorkloadProvider<*>): List<Double> {
    val workload = provider.allocate()

    // TODO warmup
    GC.collect()
    GC.collect()
    val pauses = GCStat.withStats { gcstat ->
        var sum: Long = 0
        while (gcstat.recorded.size < 20) {
            GC.collect()
            sum += fact(1_000_000)
        }
        Blackhole.consume(sum)

        gcstat.pauseTimes().map { (it / 1000).toDouble() }
        //gcstat.markedCount()
        //gcstat.pausePerObj()
    }
    Blackhole.consume(workload)
    return pauses
}


class MemoryBenchmarkLauncher : Launcher() {
    val workloads = listOf(ArrayWorkload, LinkedList, Life, Empty, Tree)

    override val baseBenchmarksSet: MutableMap<String, AbstractBenchmarkEntry> = workloads.map {
        "mark::${it.name()}" to BenchmarkEntryManual({ benchMark(it) })
    }.toMap().toMutableMap()
}

fun runBenchmark(logger: Launcher.Logger,
                 numWarmIterations: Int,
                 name: String,
                 recordMeasurement: (RecordTimeMeasurement) -> Unit,
                 recordPause: (Double) -> Unit,
                 benchmark: BenchmarkEntryManual) {
    logger.log("Warm up iterations for benchmark $name\n")
    benchmark.benchmark()

    logger.log("Running benchmark $name ")
    logger.log(".", usePrefix = false)
    // TODO cleanup()
    var pauses: List<Double>? = null
    val time = measureNanoTime {
        pauses = benchmark.benchmark()!! as List<Double>
    }
    recordMeasurement(RecordTimeMeasurement(BenchmarkResult.Status.PASSED, 0, numWarmIterations, time.toDouble()))
    recordPause(pauses!!.max())

    logger.log("\n", usePrefix = false)
}

fun main(args: Array<String>) {
    val launcher = MemoryBenchmarkLauncher()
    BenchmarksRunner.runBenchmarks(args, { arguments: BenchmarkArguments ->
        if (arguments is BaseBenchmarkArguments) {
            // TODO assert repeat number
            launcher.launch(arguments.warmup, arguments.repeat, arguments.prefix,
                    arguments.filter, arguments.filterRegex, arguments.verbose) { logger, numWarmIterations, numberOfAttempts, name, benchmarkResults, benchmark ->
                val prefix = arguments.prefix
                val recordMeasurement: (RecordTimeMeasurement) -> Unit = {
                    benchmarkResults.add(BenchmarkResult(
                            "$prefix$name",
                            it.status,
                            it.durationNs / 1000,
                            BenchmarkResult.Metric.EXECUTION_TIME,
                            it.durationNs / 1000,
                            it.iteration + 1,
                            it.warmupCount))
                }
                val recordPause: (Double) -> Unit = {
                    benchmarkResults.add(BenchmarkResult(
                            "$prefix$name",
                            BenchmarkResult.Status.PASSED,
                            it,
                            BenchmarkResult.Metric.GC_PAUSE,
                            0.0, // TODO
                            1,
                            numWarmIterations))
                }
                runBenchmark(logger, numWarmIterations, name, recordMeasurement, recordPause, benchmark as BenchmarkEntryManual)
            }
        } else emptyList()
    }, benchmarksListAction = launcher::benchmarksListAction)
}
