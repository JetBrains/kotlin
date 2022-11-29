/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.benchmark

import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.TextProgressMonitor
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.DisplayConfiguration
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.io.toHTML
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.jetbrains.kotlinx.dataframe.math.median
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.PropertiesCollection

@Suppress("unused")
@KotlinScript(
    fileExtension = "benchmark.kts",
    compilationConfiguration = BenchmarkScriptDefinition::class,
    evaluationConfiguration = BenchmarkEvaluationConfiguration::class
)
abstract class BenchmarkTemplate(
    vararg args: String,
    private val projectName: String,
    private val projectGitUrl: String,
    private val gitCommitSha: String,
) {
    private val workingDir = File(args.first())
    val currentKotlinVersion: String = args[1]
    private val gradleProfilerDir = workingDir.resolve("gradle-profiler")
    private val projectRepoDir = workingDir.resolve(projectName)
    private val scenariosDir = workingDir.resolve("scenarios")
    private val benchmarkOutputsDir = workingDir.resolve("outputs")

    private val gitOperationsPrinter = TextProgressMonitor()

    fun <T : InputStream> runAllBenchmarks(
        suite: ScenarioSuite,
        benchmarks: Map<BenchmarkName, (() -> Pair<String, T>)?>
    ) {
        printStartingMessage()
        downloadGradleProfilerIfNotAvailable()
        checkoutRepository()

        val results = benchmarks.map { (name, patchFile) ->
            repoReset()
            patchFile?.let {
                val (patchName, patch) = it()
                repoApplyPatch(patchName, patch)
            }
            runBenchmark(suite, name)
        }
        aggregateBenchmarkResults(*results.toTypedArray())

        printEndingMessage()
    }

    fun printStartingMessage() {
        println("$STEP_SEPARATOR Starting Gradle regression benchmark for $projectName $STEP_SEPARATOR")
    }

    fun printEndingMessage() {
        println("$STEP_SEPARATOR Gradle regression benchmark for $projectName has ended $STEP_SEPARATOR")
    }

    fun downloadGradleProfilerIfNotAvailable() {
        if (gradleProfilerDir.exists()) {
            println("Gradle profiler has been already downloaded")
        } else {
            downloadAndExtractGradleProfiler()
        }
    }

    fun checkoutRepository(): File {
        val git = if (projectRepoDir.exists()) {
            println("Repository is available, resetting it state")
            Git.open(projectRepoDir).also {
                it.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .setProgressMonitor(gitOperationsPrinter)
                    .call()
            }
        } else {
            println("Running git checkout for $projectGitUrl")
            projectRepoDir.mkdirs()
            Git.cloneRepository()
                .setDirectory(projectRepoDir)
                .setCloneSubmodules(true)
                .setProgressMonitor(gitOperationsPrinter)
                .setURI(projectGitUrl)
                .call()
        }

        git.checkout()
            .setName(gitCommitSha)
            .setProgressMonitor(gitOperationsPrinter)
            .call()

        return projectRepoDir
    }

    fun repoApplyPatchFromFile(
        patchFile: String
    ) {
        val patch = File(patchFile)
        repoApplyPatch(patch.name, patch.inputStream())
    }

    fun repoApplyPatch(
        patchName: String,
        patch: InputStream
    ) {
        println("Applying patch $patchName to repository")
        Git.open(projectRepoDir)
            .apply()
            .setPatch(patch)
            .call()
    }

    fun repoReset() {
        println("Hard resetting project repo")
        Git.open(projectRepoDir)
            .reset()
            .setMode(ResetCommand.ResetType.HARD)
            .setProgressMonitor(gitOperationsPrinter)
            .call()
    }

    fun runBenchmark(
        scenarioSuite: ScenarioSuite,
        benchmarkName: BenchmarkName,
        dryRun: Boolean = false
    ): BenchmarkResult {
        println("Staring benchmark $benchmarkName")
        val normalizedBenchmarkName = benchmarkName.normalizeTitle
        if (!scenariosDir.exists()) scenariosDir.mkdirs()
        val scenarioFile = scenariosDir.resolve("${projectName}_$normalizedBenchmarkName.scenario")
        scenarioSuite.writeTo(scenarioFile)
        val benchmarkOutputDir = benchmarkOutputsDir
            .resolve(projectName)
            .resolve(normalizedBenchmarkName)
            .also {
                if (it.exists()) it.deleteRecursively()
                it.mkdirs()
            }

        val profilerProcessBuilder = ProcessBuilder()
            .directory(workingDir)
            .inheritIO()
            .command(
                gradleProfilerBin.absolutePath,
                "--benchmark",
                "--measure-config-time",
                "--project-dir",
                projectRepoDir.absolutePath,
                "--scenario-file",
                scenarioFile.absolutePath,
                "--output-dir",
                benchmarkOutputDir.absolutePath
            ).also {
                // Required, so 'gradle-profiler' will use toolchain JDK instead of current user one
                it.environment()["JAVA_HOME"] = System.getProperty("java.home")
                if (dryRun) it.command().add("--dry-run")
            }

        val profilerProcess = profilerProcessBuilder.start()
        // Stop profiler on script stop
        Runtime.getRuntime().addShutdownHook(Thread {
            profilerProcess.destroy()
        })
        profilerProcess.waitFor()

        if (profilerProcess.exitValue() != 0) {
            throw BenchmarkFailedException(profilerProcess.exitValue())
        }

        println("Benchmark $benchmarkName has ended")
        return BenchmarkResult(
            benchmarkName,
            benchmarkOutputDir.resolve("benchmark.csv")
        )
    }

    fun aggregateBenchmarkResults(vararg benchmarkResults: BenchmarkResult) {
        println("Aggregating benchmark results...")
        val sortedResults = benchmarkResults.sortedBy { it.name }
        val results = sortedResults
            .map { result ->
                DataFrame
                    .readCSV(result.result)
                    .flipColumnsWithRows()
                    .remove("version", "tasks") // removing unused columns
                    .remove { nameContains("warm-up build") } // removing warm-up times
                    .add("median build time") { // squashing build times into median build time
                        valuesOf<Int>().median()
                    }
                    .remove { nameContains("measured build") } // removing iterations results
                    .groupBy("scenario").aggregate { // merging configuration and build times into one row
                        first { it.values().contains("task start") }["median build time"] into "tasks start median time"
                        first { it.values().contains("total execution time") }["median build time"] into "execution median time"
                    }
                    .add("benchmark") { result.name }
            }
            .concat()
            .groupBy("scenario").aggregate { // Merging scenarios from different benchmarks into one row
                forEach { row ->
                    row["tasks start median time"] into "Configuration: ${row["benchmark"]}"
                    row["execution median time"] into "Execution: ${row["benchmark"]}"
                }
            }
            .sortBy("scenario")
            .rename("scenario" to "Scenario")
            .reorderColumnsBy {
                // "Scenario" column should always be in the first place
                if (it.name() == "Scenario") "0000Scenario" else it.name()
            }
            .insert("Configuration diff from stable release") {
                val stableReleaseConfiguration = column<Int>("Configuration: ${sortedResults.first().name}").getValue(this)
                val currentReleaseConfiguration = column<Int>("Configuration: ${sortedResults.last().name}").getValue(this)
                val percent = currentReleaseConfiguration * 100 / stableReleaseConfiguration
                "${percent}%"
            }
            .after("Configuration: ${sortedResults.last().name}")
            .insert("Execution diff from stable release") {
                val stableReleaseConfiguration = column<Int>("Execution: ${sortedResults.first().name}").getValue(this)
                val currentReleaseConfiguration = column<Int>("Execution: ${sortedResults.last().name}").getValue(this)
                val percent = currentReleaseConfiguration * 100 / stableReleaseConfiguration
                "${percent}%"
            }
            .after("Execution: ${sortedResults.last().name}")

        println("Benchmark results:")
        println(results.print(borders = true))

        val benchmarkOutputDir = benchmarkOutputsDir.resolve(projectName)
        val benchmarkCsv = benchmarkOutputDir.resolve("$projectName.csv")
        results.writeCSV(benchmarkCsv)
        val benchmarkHtml = benchmarkOutputDir.resolve("$projectName.html")
        benchmarkHtml.writeText(
            results.toHTML(
                configuration = DisplayConfiguration(
                    cellContentLimit = 120,
                    cellFormatter = { dataRow, dataColumn ->
                        if (dataColumn.name.contains("diff from stable release")) {
                            val percent = dataRow.getValue<String>(dataColumn.name).removeSuffix("%").toInt()
                            when {
                                percent <= 100 -> background(184, 255, 184) // Green
                                percent in 101..105 -> this.background(255, 255, 184) // Yellow
                                else -> background(255, 184, 184) // Red
                            }
                        } else if (dataColumn.name == "Scenario") {
                            background(225, 225, 225) // Gray
                            bold
                        } else {
                            null
                        }
                    }
                ),
                includeInit = true,
                getFooter = {
                    "Results for: $projectName"
                }
            ).toString()
        )
        println("Result in csv format: ${benchmarkCsv.absolutePath}")
        println("Result in html format: ${benchmarkHtml.absolutePath}")
    }

    private fun downloadAndExtractGradleProfiler() {
        println("Downloading gradle-profiler into ${gradleProfilerDir.absolutePath}")

        gradleProfilerDir.mkdirs()

        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
            .get()
            .url(GRADLE_PROFILER_URL)
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to download gradle-profiler, error code: ${response.code}")
        }

        val contentLength = response.body!!.contentLength()
        var downloadedLength = 0L
        print("Downloading: ")
        response.body!!.byteStream().buffered().use { responseContent ->
            ZipInputStream(responseContent).use { zip ->
                var zipEntry = zip.nextEntry
                while (zipEntry != null) {
                    if (zipEntry.isDirectory) {
                        gradleProfilerDir.resolve(zipEntry.name.dropLeadingDir).also { it.mkdirs() }
                    } else {
                        gradleProfilerDir.resolve(zipEntry.name.dropLeadingDir).outputStream().buffered().use {
                            zip.copyTo(it)
                        }
                    }
                    downloadedLength += zipEntry.compressedSize
                    print("..${downloadedLength * 100 / contentLength}%")
                    zip.closeEntry()
                    zipEntry = zip.nextEntry
                }
            }
            print("\n")
        }
        gradleProfilerBin.setExecutable(true)
        println("Finished downloading gradle-profiler")
    }

    private fun ScenarioSuite.writeTo(output: File) {
        output.writeText(
            """
            |default-scenarios = [${scenarios.joinToString { "\"${it.title.normalizeTitle}\"" }}]
            |
            |${scenarios.joinToString(separator = "\n") { it.write() }}
            """.trimMargin()
        )
    }

    private fun Scenario.write(): String =
        """
        |${title.normalizeTitle} {
        |    title = "$title"
        |    warm-ups = $warmups
        |    iterations = $iterations
        |    tasks = [${tasks.joinToString { "\"$it\"" }}]
        |    ${if (gradleArgs.isNotEmpty()) "gradle-args = [${gradleArgs.joinToString { "\"$it\"" }}]" else ""}
        |    ${if (cleanupTasks.isNotEmpty()) "cleanup-tasks = [${cleanupTasks.joinToString { "\"$it\"" }}]" else ""}
        |    ${if (applyAbiChange.isNotEmpty()) "apply-abi-change-to = [${applyAbiChange.joinToString { "\"$it\"" }}]" else ""}
        |    ${if (applyAndroidResourceValueChange.isNotEmpty()) "apply-android-resource-value-change-to = [${applyAndroidResourceValueChange.joinToString { "\"$it\"" }}]" else ""}
        |}
        """.trimMargin()

    private val String.dropLeadingDir: String get() = substringAfter('/')
    private val String.normalizeTitle: String get() = lowercase().replace(" ", "_")

    private fun DataFrame<*>.flipColumnsWithRows(): DataFrame<*> {
        val firstColumn = columns().first()
        return DataFrameBuilder(
            listOf(firstColumn.name) + firstColumn.values.map { it.toString() }
        ).withColumns { columnName ->
            when {
                columnName == "scenario" -> DataColumn.createValueColumn(
                    columnName,
                    columns().map {
                        it.name.dropLastWhile { it.isDigit() }
                    }.drop(1)
                )
                columnName == "version" -> DataColumn.createValueColumn(
                    columnName,
                    rowToColumn(columnName) { it.toString() }
                )
                columnName == "tasks" -> DataColumn.createValueColumn(
                    columnName,
                    rowToColumn(columnName) { it.toString() }
                )
                columnName == "value" -> DataColumn.createValueColumn(
                    columnName,
                    rowToColumn(columnName) { it.toString() }
                )
                columnName.startsWith("warm-up build") -> DataColumn.createValueColumn(
                    columnName,
                    rowToColumn(columnName) { it.toString().toInt() }
                )
                columnName.startsWith("measured build") -> DataColumn.createValueColumn(
                    columnName,
                    rowToColumn(columnName) { it.toString().toIntOrNull() }
                )
                else -> throw IllegalArgumentException("Unknown column name: $columnName")
            }
        }
    }

    private fun <T : Any?> DataFrame<*>.rowToColumn(
        rowName: String,
        typeConversion: (Any?) -> T
    ): List<T> =
        rows().first { it.values().first() == rowName }.values().drop(1).map { typeConversion(it) }

    private val gradleProfilerBin: File
        get() = gradleProfilerDir
            .resolve("bin")
            .run {
                if (System.getProperty("os.name").contains("windows", ignoreCase = true)) {
                    resolve("gradle-profiler.bat")
                } else {
                    resolve("gradle-profiler").also { it.setExecutable(true) }
                }
            }

    companion object {
        private const val STEP_SEPARATOR = "###############"
        private const val GRADLE_PROFILER_VERSION = "0.19.0"
        private const val GRADLE_PROFILER_URL: String =
            "https://repo1.maven.org/maven2/org/gradle/profiler/gradle-profiler/$GRADLE_PROFILER_VERSION/gradle-profiler-$GRADLE_PROFILER_VERSION.zip"

    }
}

typealias BenchmarkName = String

class BenchmarkFailedException(exitCode: Int) : Exception(
    "Benchmark process was not finished successfully with $exitCode exit code."
)

class BenchmarkResult(
    val name: BenchmarkName,
    val result: File
)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class BenchmarkProject(
    val name: String,
    val gitUrl: String,
    val gitCommitSha: String
)

class BenchmarkScriptDefinition : ScriptCompilationConfiguration(
    {
        defaultImports(
            listOf(
                BenchmarkProject::class.qualifiedName!!,
                "org.jetbrains.kotlin.gradle.benchmark.suite"
            )
        )
        refineConfiguration {
            onAnnotations(BenchmarkProject::class, handler = BenchmarkScriptConfigurator())
        }
    }
)

class BenchmarkEvaluationConfiguration : ScriptEvaluationConfiguration(
    {
        refineConfigurationBeforeEvaluate { context ->
            val compileConf = context.evaluationConfiguration[compilationConfiguration]
                ?: return@refineConfigurationBeforeEvaluate makeFailureResult("Script compilation configuration is not available")

            val name: String = compileConf[benchmarkProjectName]!!
            val gitUrl: String = compileConf[benchmarkGitUrl]!!
            val gitCommitSha: String = compileConf[benchmarkGitCommitSha]!!

            context.evaluationConfiguration.with evalConf@{
                constructorArgs(name, gitUrl, gitCommitSha)
            }.asSuccess()
        }
    }
)

internal class BenchmarkScriptConfigurator : RefineScriptCompilationConfigurationHandler {
    override fun invoke(
        context: ScriptConfigurationRefinementContext
    ): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val benchmarkProject = context.collectedData
            ?.get(ScriptCollectedData.foundAnnotations)
            ?.find { it is BenchmarkProject } as? BenchmarkProject
            ?: return run {
                makeFailureResult("Script does not contain ${BenchmarkProject::name} annotation!")
            }

        return ScriptCompilationConfiguration(context.compilationConfiguration) {
            data[benchmarkProjectName] = benchmarkProject.name
            data[benchmarkGitUrl] = benchmarkProject.gitUrl
            data[benchmarkGitCommitSha] = benchmarkProject.gitCommitSha
        }.asSuccess()
    }
}

private val benchmarkProjectName by PropertiesCollection.key<String>()
private val benchmarkGitUrl by PropertiesCollection.key<String>()
private val benchmarkGitCommitSha by PropertiesCollection.key<String>()