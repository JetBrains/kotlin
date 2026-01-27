/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.benchmark.LogLevel
import org.jetbrains.kotlin.benchmark.Logger
import org.jetbrains.report.json.JsonLiteral
import org.jetbrains.report.json.JsonObject
import org.jetbrains.report.json.JsonTreeParser
import java.io.ByteArrayOutputStream
import javax.inject.Inject

private fun ExecOperations.execCapturingStdout(action: Action<ExecSpec>): String {
    val output = ByteArrayOutputStream()
    exec {
        action.execute(this)
        standardOutput = output
    }.rethrowFailure()
    return output.toString()
}

private fun String.splitCommaSeparatedOption(optionName: String) = split("\\s*,\\s*".toRegex()).map {
    if (it.isNotEmpty()) listOf(optionName, it) else listOf(null)
}.flatten().filterNotNull()

/**
 * Run the benchmark in [executable] and place the report in [reportFile].
 *
 * When run directly can be configured with
 * * `--filter` or `--filterRegex` to run only a subset of the benchmarks
 * * `--verbose` to enable verbose logging
 * * `--baseOnly` to enable only a predefined subset of the benchmarks
 */
open class RunKotlinNativeTask @Inject constructor(
        private val execOperations: ExecOperations,
        objectFactory: ObjectFactory,
) : DefaultTask() {
    /**
     * Location of benchmark executable
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE) // only the executable, not its location matters
    val executable: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Where to place the benchmark report file
     *
     * NOTE: this is not a complete report, [JsonReportTask] adds some additional information
     */
    @get:OutputFile
    val reportFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Comma-separated list of benchmarks to run
     *
     * @see filterRegex
     */
    @get:Input
    @get:Option(option = "filter", description = "Benchmarks to run (comma-separated)")
    @get:Optional
    val filter: Property<String> = objectFactory.property(String::class.java)

    /**
     * Comma-separated list of benchmarks (described by regular expressions) to run
     *
     * @see filter
     */
    @get:Input
    @get:Option(option = "filterRegex", description = "Benchmarks to run, described by regular expressions (comma-separated)")
    @get:Optional
    val filterRegex: Property<String> = objectFactory.property(String::class.java)

    /**
     * Enable verbose logging
     */
    @get:Input
    @get:Option(option = "verbose", description = "Verbose mode of running benchmarks")
    val verbose: Property<Boolean> = objectFactory.property(Boolean::class.java)

    /**
     * Run only a predefined set of benchmarks
     */
    @get:Input
    @get:Option(option = "baseOnly", description = "Run only set of base benchmarks")
    val baseOnly: Property<Boolean> = objectFactory.property(Boolean::class.java)

    /**
     * How many warmup iterations should each benchmark do
     */
    @get:Input
    val warmupCount: Property<Int> = objectFactory.property(Int::class.java)

    /**
     * How many iterations (excluding [warmupCount]) should each benchmark do
     */
    @get:Input
    val repeatCount: Property<Int> = objectFactory.property(Int::class.java)

    /**
     * Whether this benchmark should perform warmup and repetitions itself or be externally driven.
     */
    @get:Input
    val repeatingType: Property<BenchmarkRepeatingType> = objectFactory.property(BenchmarkRepeatingType::class.java)

    /**
     * Additional arguments for the benchmark executable.
     */
    @get:Input
    val arguments: ListProperty<String> = objectFactory.listProperty(String::class.java)

    /**
     * If `true`, wrap benchmarking code with `cset` to keep it tied to a single core.
     */
    @get:Input
    val useCSet: Property<Boolean> = objectFactory.property(Boolean::class.java)

    /**
     * Additional environment variables with which to run the benchmark executable
     */
    @get:Input
    val environment: MapProperty<String, String> = objectFactory.mapProperty(String::class.java, String::class.java)

    private fun execBenchmarkOnce(
            benchmark: String,
            warmupCount: Int,
            repeatCount: Int,
            verbose: Boolean,
    ): String {
        val output = execOperations.execCapturingStdout {
            if (useCSet.get()) {
                executable = "cset"
                args("shield", "--exec", "--", this@RunKotlinNativeTask.executable.asFile.get())
            } else {
                executable(this@RunKotlinNativeTask.executable.asFile.get())
            }
            args(arguments.get())
            args("-f", benchmark)
            if (verbose) {
                args("-v")
            }
            args("-w", warmupCount.toString())
            args("-r", repeatCount.toString())
            environment.putAll(this@RunKotlinNativeTask.environment.get())
        }
        return output.substringAfter("[").removeSuffix("]")
    }

    private fun execBenchmarkRepeatedly(benchmark: String, warmupCount: Int, repeatCount: Int): List<String> {
        val logger = if (verbose.get()) Logger(LogLevel.DEBUG) else Logger()
        logger.log("Warm up iterations for benchmark $benchmark\n")
        repeat(warmupCount) {
            execBenchmarkOnce(benchmark, 0, 1, false)
        }
        val result = mutableListOf<String>()
        logger.log("Running benchmark $benchmark ")
        repeat(repeatCount) { i ->
            logger.log(".", usePrefix = false)
            val benchmarkReport = JsonTreeParser.parse(execBenchmarkOnce(benchmark, 0, 1, false)).jsonObject
            val modifiedBenchmarkReport = JsonObject(HashMap(benchmarkReport.content).apply {
                put("repeat", JsonLiteral(i))
                put("warmup", JsonLiteral(warmupCount))
            })
            result.add(modifiedBenchmarkReport.toString())
        }
        logger.log("\n", usePrefix = false)
        return result
    }

    @TaskAction
    fun run() {
        val benchmarks = execOperations.execCapturingStdout {
            executable(this@RunKotlinNativeTask.executable.asFile.get())
            if (baseOnly.get()) {
                args("baseOnlyList")
            } else {
                args("list")
            }
        }.lines()

        val filterArgs = filter.orNull?.splitCommaSeparatedOption("-f")
        val filterRegexArgs = filterRegex.orNull?.splitCommaSeparatedOption("-fr")?.map { it.toRegex() }

        val benchmarksToRun = benchmarks.filter { benchmark ->
            if (benchmark.isEmpty()) return@filter false
            if (filterArgs?.let { benchmark in it } == true) {
                return@filter true
            }
            filterRegexArgs?.any { it.matches(benchmark) } != false
        }

        val results = benchmarksToRun.flatMap { benchmark ->
            when (repeatingType.get()) {
                BenchmarkRepeatingType.INTERNAL -> listOf(execBenchmarkOnce(benchmark, warmupCount.get(), repeatCount.get(), verbose.get()))
                BenchmarkRepeatingType.EXTERNAL -> execBenchmarkRepeatedly(benchmark, warmupCount.get(), repeatCount.get())
            }
        }

        reportFile.asFile.get().writeText("[${results.joinToString(",")}]")
    }
}
