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
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.benchmark.Logger
import org.jetbrains.kotlin.benchmark.LogLevel
import org.jetbrains.report.json.*
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.collections.HashMap

private fun ExecOperations.execCapturingStdout(action: Action<ExecSpec>): String {
    val output = ByteArrayOutputStream()
    exec {
        action.execute(this)
        standardOutput = output
    }.rethrowFailure()
    return output.toString()
}

private fun String.splitCommaSeparatedOption(optionName: String) =
        split("\\s*,\\s*".toRegex()).map {
            if (it.isNotEmpty()) listOf(optionName, it) else listOf(null)
        }.flatten().filterNotNull()

open class RunKotlinNativeTask @Inject constructor(
        private val execOperations: ExecOperations,
        objectFactory: ObjectFactory,
) : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE) // only the executable, not its location matters
    val executable: RegularFileProperty = objectFactory.fileProperty()

    @get:OutputFile
    val reportFile: RegularFileProperty = objectFactory.fileProperty()

    @get:Input
    @get:Option(option = "filter", description = "Benchmarks to run (comma-separated)")
    @get:Optional
    val filter: Property<String> = objectFactory.property(String::class.java)

    @get:Input
    @get:Option(option = "filterRegex", description = "Benchmarks to run, described by regular expressions (comma-separated)")
    @get:Optional
    val filterRegex: Property<String> = objectFactory.property(String::class.java)

    @get:Input
    @get:Option(option = "verbose", description = "Verbose mode of running benchmarks")
    val verbose: Property<Boolean> = objectFactory.property(Boolean::class.java)

    @get:Input
    @get:Option(option = "baseOnly", description = "Run only set of base benchmarks")
    val baseOnly: Property<Boolean> = objectFactory.property(Boolean::class.java)

    @get:Input
    val warmupCount: Property<Int> = objectFactory.property(Int::class.java)

    @get:Input
    val repeatCount: Property<Int> = objectFactory.property(Int::class.java)

    @get:Input
    val repeatingType: Property<BenchmarkRepeatingType> = objectFactory.property(BenchmarkRepeatingType::class.java)

    @get:Input
    val arguments: ListProperty<String> = objectFactory.listProperty(String::class.java)

    @get:Input
    val useCSet: Property<Boolean> = objectFactory.property(Boolean::class.java)

    private fun execBenchmarkOnce(
            benchmark: String,
            warmupCount: Int,
            repeatCount: Int,
            verbose: Boolean,
    ) : String {
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
        }
        return output.substringAfter("[").removeSuffix("]")
    }

    private fun execBenchmarkRepeatedly(benchmark: String, warmupCount: Int, repeatCount: Int) : List<String> {
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
            if (benchmark.isEmpty())
                return@filter false
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
