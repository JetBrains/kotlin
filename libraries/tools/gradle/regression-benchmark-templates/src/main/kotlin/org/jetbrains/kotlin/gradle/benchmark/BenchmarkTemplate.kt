/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.benchmark

import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.submodule.SubmoduleWalk
import org.eclipse.jgit.transport.URIish
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.RowExpression
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.DisplayConfiguration
import org.jetbrains.kotlinx.dataframe.io.readCsv
import org.jetbrains.kotlinx.dataframe.io.toStandaloneHtml
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.jetbrains.kotlinx.dataframe.name
import org.jetbrains.kotlinx.dataframe.values
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
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
    private val stableKotlinVersions: String,
) {
    private val workingDir = File(args.first())
    val currentKotlinVersion: String = args[1]
    private val kotlinVersions = setOf(stableKotlinVersions, currentKotlinVersion)
    private val gradleProfilerDir = workingDir.resolve("gradle-profiler-${GRADLE_PROFILER_VERSION}")
    private val asyncProfilerDir = workingDir.resolve("async-profiler-${ASYNC_PROFILER_VERSION}")
    private val projectRepoDir = workingDir.resolve(projectName)
    private val scenariosDir = workingDir.resolve("scenarios")
    private val benchmarkOutputsDir = workingDir.resolve("outputs")

    private val gitOperationsPrinter = TextProgressMonitor()

    fun <T : InputStream> runBenchmarks(
        repoPatch: (() -> List<Pair<String, T>>)?,
        suite: ScenarioSuite,
    ) {
        val asyncProfilerConfig = asyncProfilerConfig()
        printStartingMessage()
        downloadGradleProfilerIfNotAvailable()
        asyncProfilerConfig?.let {
            downloadAsyncProfilerIfNotAvailable(it)
        }
        checkoutRepository()

        repoPatch?.let {
            it().forEach { (patchName, patch) ->
                repoApplyPatch(patchName, patch)
            }
        }
        val results = runBenchmarksSplitByAsyncProfilerSupport(suite, asyncProfilerConfig)
        aggregateBenchmarkResults(results)

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

    fun downloadAsyncProfilerIfNotAvailable(asyncProfilerConfig: AsyncProfilerConfiguration) {
        if (asyncProfilerDir.exists()) {
            println("Async profiler has been already downloaded")
        } else {
            downloadAndExtractAsyncProfiler(asyncProfilerConfig)
        }
    }

    fun checkoutRepository(): File {
        val git = if (projectRepoDir.resolve(".git").exists()) {
            println("Repository is available")
            Git.open(projectRepoDir)
        } else {
            println("Running git init for $projectGitUrl")
            projectRepoDir.mkdirs()
            Git.init()
                .setDirectory(projectRepoDir)
                .call().also {
                    it.remoteAdd()
                        .setName(Constants.DEFAULT_REMOTE_NAME)
                        .setUri(URIish(projectGitUrl))
                        .call()
                }
        }

        git.use { git ->
            git.fetch()
                .setRefSpecs(gitCommitSha)
                .setDepth(1)
                .setProgressMonitor(gitOperationsPrinter)
                .call()

            git.resetRepositoryState(gitCommitSha)
            git.updateSubmodules()

            git.walkSubmodulesRecursively { submodule ->
                submodule.resetRepositoryState("HEAD")
                submodule.updateSubmodules()
            }

            val status = git.status().setProgressMonitor(gitOperationsPrinter).call()
            println("Status isClean: ${status.isClean}")
            status.untracked.forEach {
                println("Status untracked: ${it}")
            }
            status.uncommittedChanges.forEach {
                println("Status uncommitted: ${it}")
            }
        }

        return projectRepoDir
    }

    private fun Git.walkSubmodulesRecursively(action: (Git) -> Unit) {
        val submodules = SubmoduleWalk.forIndex(repository)
        while (submodules.next()) {
            val submodule = submodules.repository ?: continue
            Git(submodule).use { submoduleGit ->
                println("Submodule walk: ${submoduleGit}")
                action(submoduleGit)
                submoduleGit.walkSubmodulesRecursively(action)
            }
        }
    }

    private fun Git.resetRepositoryState(ref: String) {
        cleanXffd()
        reset()
            .setRef(ref)
            .setMode(ResetCommand.ResetType.HARD)
            .setProgressMonitor(gitOperationsPrinter)
            .call()
    }

    private fun Git.updateSubmodules() {
        submoduleInit().call().forEach { println("$this submodule init: $it") }
        submoduleSync().call().forEach { println("$this submodule sync: $it") }
        submoduleUpdate().setProgressMonitor(gitOperationsPrinter).call().forEach { println("$this submodule update: $it") }
    }

    private fun Git.cleanXffd() {
        clean()
            .setForce(true)
            .setIgnore(true)
            .setCleanDirectories(true)
            .call().forEach {
                println("Clean ${this.repository}: ${it}")
            }
    }

    fun repoApplyPatchFromFile(
        patchFile: String,
    ) {
        val patch = File(patchFile)
        repoApplyPatch(patch.name, patch.inputStream())
    }

    fun repoApplyPatch(
        patchName: String,
        patch: InputStream,
    ) {
        println("Applying patch $patchName to repository")
        Git.open(projectRepoDir).use { git ->
            git.apply()
                .setPatch(patch)
                .call()
        }
    }

    private fun runBenchmarksSplitByAsyncProfilerSupport(
        scenarioSuite: ScenarioSuite,
        asyncProfilerConfig: AsyncProfilerConfiguration?,
        @Suppress("UNUSED_PARAMETER") dryRun: Boolean = false,
    ): List<BenchmarkResult> {
        /**
         * For some reason gradle-profiler doesn't allow async-profiling scenarios that have cleanup steps
         */
        val scenariosWithAsyncProfilerSupport = scenarioSuite.scenarios.filter { it.cleanupTasks.isEmpty() }
        val scenariosWithoutAsyncProfilerSupport = scenarioSuite.scenarios.filter { it.cleanupTasks.isNotEmpty() }

        return listOf(
            runBenchmark(
                scenarioSuite = ScenarioSuite(scenariosWithoutAsyncProfilerSupport.toMutableList()),
                scenarioSuffix = "clean",
                asyncProfilerConfig = null,
                dryRun = dryRun,
            ),
            runBenchmark(
                scenarioSuite = ScenarioSuite(scenariosWithAsyncProfilerSupport.toMutableList()),
                scenarioSuffix = "incremental",
                asyncProfilerConfig = asyncProfilerConfig,
                dryRun = dryRun,
            ),
        )
    }

    private fun runBenchmark(
        scenarioSuite: ScenarioSuite,
        scenarioSuffix: String,
        asyncProfilerConfig: AsyncProfilerConfiguration?,
        @Suppress("UNUSED_PARAMETER") dryRun: Boolean,
    ): BenchmarkResult {
        println("Staring benchmark $projectName $scenarioSuffix")
        val normalizedBenchmarkName = "${projectName}_${scenarioSuffix}".normalizeTitle
        if (!scenariosDir.exists()) scenariosDir.mkdirs()
        val scenarioFile = scenariosDir.resolve("$normalizedBenchmarkName.scenario")

        scenarioSuite.writeTo(scenarioFile)
        val benchmarkOutputDir = benchmarkOutputsDir
            .resolve(projectName)
            .resolve(normalizedBenchmarkName)
            .also {
                if (it.exists()) it.deleteRecursively()
                it.mkdirs()
            }

        val asyncProfilerArgs: Array<String> = asyncProfilerConfig?.let {
            arrayOf(
                "--async-profiler-home", asyncProfilerDir.path,
                "--async-profiler-event", asyncProfilerConfig.cpuProfiler,
                "--profile", "async-profiler",
            )
        } ?: emptyArray()

        val profilerProcessBuilder = ProcessBuilder()
            .directory(workingDir)
            .inheritIO()
            .command(
                gradleProfilerBin.absolutePath,
                "--benchmark",
                "--measure-config-time",
                *asyncProfilerArgs,
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

        println("Benchmark $projectName has ended")
        return BenchmarkResult(
            projectName,
            benchmarkOutputDir.resolve("benchmark.csv")
        )
    }

    fun percentageChange(columnName: String): RowExpression<Any?, String> {
        return {
            val stableReleaseConfiguration = column<Double>("${columnName}: $stableKotlinVersions").getValue(this)
            val currentReleaseConfiguration = column<Double>("${columnName}: $currentKotlinVersion").getValue(this)
            val percent = currentReleaseConfiguration * 100 / stableReleaseConfiguration
            String.format("%.2f", percent) + "%"
        }
    }

    // Not working as intended due to this bug: https://github.com/gradle/gradle-profiler/issues/317
    fun aggregateBenchmarkResults(benchmarkResults: List<BenchmarkResult>) {
        println("Aggregating benchmark results...")
        val results = benchmarkResults.map {
            DataFrame.readCsv(it.result, allowMissingColumns = true)
        }.reduce { acc, frame -> acc.fullJoin(frame) }
            .drop {
                // Removing unused rows
                it["scenario"] in listOf("version", "tasks") ||
                        it["scenario"].toString().startsWith("warm-up build")
            }
            .flipColumnsWithRows()
            .groupBy { it["scenario"] }.map {
                val totalExecutionTime = it.group.single { it["value"] == "total execution time" }
                val configurationTime = it.group.single { it["value"] == "task start" }
                it.group.concat(
                    dataFrameOf<String, Any>(it.group.columnNames()) { column ->
                        listOf(
                            when {
                                column == "scenario" -> totalExecutionTime[column]
                                column == "value" -> "execution only time"
                                column.startsWith("measured") -> (totalExecutionTime[column] as Double) - (configurationTime[column] as Double)
                                else -> error(column)
                            }!!
                        )
                    }
                )
            }
            .concat()
            .add("median build time") { // squashing build times into median build time
                rowMedianOf<Double>()
            }
            .remove { nameContains("measured build") } // removing iterations results
            .groupBy("scenario").aggregate { // merging configuration and build times into one row
                first { it.values().contains("task start") }["median build time"] into "tasks start median time"
                first { it.values().contains("execution only time") }["median build time"] into "execution only median time"
                first { it.values().contains("total execution time") }["median build time"] into "execution median time"
            }
            .groupBy {
                expr {
                    val scenarioName = get("scenario").toString()
                    if (scenarioName.endsWith(currentKotlinVersion)) {
                        scenarioName.substringBefore(currentKotlinVersion)
                    } else {
                        scenarioName.substringBefore(stableKotlinVersions)
                    }
                }
            }
            .aggregate {
                forEach { row ->
                    val version = if (row["scenario"].toString().endsWith(stableKotlinVersions)) {
                        stableKotlinVersions
                    } else {
                        currentKotlinVersion
                    }
                    row["tasks start median time"] into "Configuration: $version"
                    row["execution only median time"] into "Execution only: $version"
                    row["execution median time"] into "Execution: $version"
                }
            }
            .rename { col(0) }.into("Scenario")
            .sortBy("Scenario")
            .reorderColumnsBy {
                // "Scenario" column should always be in the first place
                if (it.name() == "Scenario") "0000Scenario" else it.name()
            }
            .insert("Configuration diff from stable release", expression = percentageChange("Configuration"))
            .after("Configuration: $currentKotlinVersion")
            .insert("Execution only diff from stable release", expression = percentageChange("Execution only"))
            .after("Execution only: $currentKotlinVersion")
            .insert("Execution diff from stable release", expression = percentageChange("Execution"))
            .after("Execution: $currentKotlinVersion")

        println("Benchmark results:")
        println(results.print(borders = true))

        val benchmarkOutputDir = benchmarkOutputsDir.resolve(projectName)
        val benchmarkCsv = benchmarkOutputDir.resolve("$projectName.csv")
        results.writeCsv(benchmarkCsv)
        val benchmarkHtml = benchmarkOutputDir.resolve("$projectName.html")
        benchmarkHtml.writeText(
            results.toStandaloneHtml(
                configuration = DisplayConfiguration(
                    cellContentLimit = 120,
                    cellFormatter = { dataRow, dataColumn ->
                        if (dataColumn.name.contains("diff from stable release")) {
                            val percent = dataRow.getValue<String>(dataColumn.name).removeSuffix("%").toDouble().toInt()
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
                getFooter = {
                    "Results for: $projectName"
                }
            ).toString()
        )
        println("Result in csv format: ${benchmarkCsv.absolutePath}")
        println("Result in html format: ${benchmarkHtml.absolutePath}")
    }

    private fun downloadAndExtractGradleProfiler() {
        zipDownloadAndExtract(
            gradleProfilerDir,
            GRADLE_PROFILER_URL,
        )
        gradleProfilerBin.setExecutable(true)

        println("Finished downloading gradle-profiler")
    }

    private fun downloadAndExtractAsyncProfiler(asyncProfilerConfig: AsyncProfilerConfiguration) {
        when (asyncProfilerConfig.decompressionMethod) {
            Decompression.TAR_GZ -> tarGzipDownloadAndExtract(
                asyncProfilerDir,
                asyncProfilerConfig.downloadUrl,
            )
            Decompression.ZIP -> zipDownloadAndExtract(
                asyncProfilerDir,
                asyncProfilerConfig.downloadUrl,
            )
        }

        val asyncProfilerBin = "bin/asprof"
        asyncProfilerDir.resolve(asyncProfilerBin).setExecutable(true)

        /**
         * 4.1 no longer has the script, so. symlink manually
         */
        val asyncProfilerExecutable = asyncProfilerDir.resolve("profiler.sh")
        Files.createSymbolicLink(asyncProfilerExecutable.toPath(), Paths.get(asyncProfilerBin))

        println("Finished downloading async-profiler")
    }

    private fun tarGzipDownloadAndExtract(
        outputDir: File,
        url: String,
    ) = downloadAndExtract(
        outputDir = outputDir,
        url = url,
        useDecompressedStream = { inputStream, action ->
            GzipCompressorInputStream(inputStream).use { zip ->
                TarArchiveInputStream(zip).use { tar ->
                    action(tar)
                }
            }
        },
        nextEntry = { nextEntry },
        name = { name },
        size = { size },
        isDirectory = { isDirectory },
    )

    private fun zipDownloadAndExtract(
        outputDir: File,
        url: String,
    ) = downloadAndExtract(
        outputDir = outputDir,
        url = url,
        useDecompressedStream = { inputStream, action ->
            ZipInputStream(inputStream).use {
                action(it)
            }
        },
        nextEntry = { nextEntry },
        name = { name },
        size = { size },
        isDirectory = { isDirectory },
    )

    private fun <
            DecompressedStream : InputStream,
            StreamEntry,
            > downloadAndExtract(
        outputDir: File,
        url: String,
        useDecompressedStream: (InputStream, (DecompressedStream) -> Unit) -> Unit,
        nextEntry: DecompressedStream.() -> StreamEntry?,
        name: StreamEntry.() -> String,
        size: StreamEntry.() -> Long,
        isDirectory: StreamEntry.() -> Boolean,
    ) {
        println("Downloading ${url} into ${outputDir.absolutePath}")

        outputDir.mkdirs()

        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
            .get()
            .url(url)
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to download ${url}, error code: ${response.code}")
        }

        val contentLength = response.body!!.contentLength()
        var downloadedLength = 0L
        print("Downloading: ")
        response.body!!.byteStream().buffered().use { responseContent ->
            useDecompressedStream(responseContent) { stream ->
                var entry = stream.nextEntry()
                while (entry != null) {
                    if (entry.isDirectory()) {
                        outputDir.resolve(entry.name().dropLeadingDir).also { it.mkdirs() }
                    } else {
                        outputDir.resolve(entry.name().dropLeadingDir).outputStream().buffered().use { outputStream ->
                            stream.copyTo(outputStream)
                        }
                    }
                    downloadedLength += entry.size()
                    print("..${downloadedLength * 100 / contentLength}%")
                    entry = stream.nextEntry()
                }
            }

            println()
        }
    }

    private fun ScenarioSuite.writeTo(output: File) {
        val allScenarios = scenarios
            .map {scenario ->
                kotlinVersions.map { scenario to it }
            }
            .flatten()
            .map { (scenario, version) ->
                "${scenario.title} $version".normalizeTitle
            }
        output.writeText(
            """
            |default-scenarios = [${allScenarios.joinToString { "\"$it\"" }}]
            |
            |${scenarios.joinToString(separator = "\n") { it.write() }}
            """.trimMargin()
        )
    }

    private fun Scenario.write(): String = kotlinVersions.joinToString(separator = "\n") { kotlinVersion ->
        val finalGradleArgs = gradleArgs
            .plus("-PkotlinVersion=$kotlinVersion")
            .joinToString { "\"$it\"" }
        """
        |${"$title $kotlinVersion".normalizeTitle} {
        |    title = "$title $kotlinVersion"
        |    warm-ups = $warmups
        |    iterations = $iterations
        |    tasks = [${tasks.joinToString { "\"$it\"" }}]
        |    gradle-args = [$finalGradleArgs]
        |    ${if (cleanupTasks.isNotEmpty()) "cleanup-tasks = [${cleanupTasks.joinToString { "\"$it\"" }}]" else ""}
        |    ${if (applyAbiChange.isNotEmpty()) "apply-abi-change-to = [${applyAbiChange.joinToString { "\"$it\"" }}]" else ""}
        |    ${if (applyAndroidResourceValueChange.isNotEmpty()) "apply-android-resource-value-change-to = [${applyAndroidResourceValueChange.joinToString { "\"$it\"" }}]" else ""}
        |}
        |
        """.trimMargin()
    }

    private val String.dropLeadingDir: String get() = substringAfter('/')
    private val String.normalizeTitle: String get() = lowercase().replace(" ", "_").replace(".", "_")

    private fun DataFrame<*>.flipColumnsWithRows(): DataFrame<*> {
        val firstColumn = columns().first()
        return DataFrameBuilder(
            listOf(firstColumn.name) + firstColumn.values.map { it.toString() }
        ).withColumns { columnName ->
            when {
                columnName == "scenario" -> DataColumn.createValueColumn(
                    columnName,
                    columns().map {
                        if (it.name.contains(currentKotlinVersion)) {
                            it.name.substringBefore(currentKotlinVersion) + currentKotlinVersion
                        } else {
                            it.name.substringBefore(stableKotlinVersions) + stableKotlinVersions
                        }
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
                    rowToColumn(columnName) { it.toString().toDouble() }
                )
                columnName.startsWith("measured build") -> DataColumn.createValueColumn(
                    columnName,
                    rowToColumn(columnName) { it.toString().toDoubleOrNull() }
                )
                else -> throw IllegalArgumentException("Unknown column name: $columnName")
            }
        }
    }

    private fun <T : Any?> DataFrame<*>.rowToColumn(
        rowName: String,
        typeConversion: (Any?) -> T,
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

    enum class Decompression {
        TAR_GZ,
        ZIP
    }
    class AsyncProfilerConfiguration(
        val downloadUrl: String,
        val decompressionMethod: Decompression,
        val cpuProfiler: String,
    )

    private fun asyncProfilerConfig(): AsyncProfilerConfiguration? {
        val javaOsName = System.getProperty("os.name")
        return when {
            javaOsName == "Mac OS X" -> BenchmarkTemplate.AsyncProfilerConfiguration(
                "https://github.com/async-profiler/async-profiler/releases/download/v${ASYNC_PROFILER_VERSION}/async-profiler-${ASYNC_PROFILER_VERSION}-macos.zip",
                Decompression.ZIP,
                cpuProfiler = "cpu",
            )
            javaOsName == "Linux" -> BenchmarkTemplate.AsyncProfilerConfiguration(
                "https://github.com/async-profiler/async-profiler/releases/download/v${ASYNC_PROFILER_VERSION}/async-profiler-${ASYNC_PROFILER_VERSION}-linux-x64.tar.gz",
                Decompression.TAR_GZ,
                // On CI this will implicitly run in "ctimer" mode because we run containerized builds which don't have access to perf_events
                cpuProfiler = "cpu",
            )
            javaOsName.startsWith("Windows") -> null
            else -> error("Unknown OS ${javaOsName}")
        }
    }

    companion object {
        private const val STEP_SEPARATOR = "###############"
        private const val GRADLE_PROFILER_VERSION = "0.23.0"
        private const val GRADLE_PROFILER_URL: String =
            "https://repo1.maven.org/maven2/org/gradle/profiler/gradle-profiler/$GRADLE_PROFILER_VERSION/gradle-profiler-$GRADLE_PROFILER_VERSION.zip"
        private const val ASYNC_PROFILER_VERSION = "4.1"

    }
}

typealias BenchmarkName = String

@Suppress("Unused")
class BenchmarkFailedException(exitCode: Int) : Exception(
    "Benchmark process was not finished successfully with $exitCode exit code."
)

class BenchmarkResult(
    val name: BenchmarkName,
    val result: File,
)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class BenchmarkProject(
    val name: String,
    val gitUrl: String,
    val gitCommitSha: String,
    val stableKotlinVersion: String,
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
            val stableKotlinVersion: String = compileConf[benchmarkStableKotlinVersion]!!

            context.evaluationConfiguration.with evalConf@{
                constructorArgs(name, gitUrl, gitCommitSha, stableKotlinVersion)
            }.asSuccess()
        }
    }
)

internal class BenchmarkScriptConfigurator : RefineScriptCompilationConfigurationHandler {
    override fun invoke(
        context: ScriptConfigurationRefinementContext,
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
            data[benchmarkStableKotlinVersion] = benchmarkProject.stableKotlinVersion
        }.asSuccess()
    }
}

private val benchmarkProjectName by PropertiesCollection.key<String>()
private val benchmarkGitUrl by PropertiesCollection.key<String>()
private val benchmarkGitCommitSha by PropertiesCollection.key<String>()
private val benchmarkStableKotlinVersion by PropertiesCollection.key<String>()
