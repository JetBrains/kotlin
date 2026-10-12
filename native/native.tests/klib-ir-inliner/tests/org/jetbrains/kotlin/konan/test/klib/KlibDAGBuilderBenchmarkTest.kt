/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.backend.konan.library.InternalKlibDAGApi
import org.jetbrains.kotlin.backend.konan.library.KlibDAG
import org.jetbrains.kotlin.backend.konan.library.KlibDAGBuilder
import org.jetbrains.kotlin.backend.konan.library.KlibDAGBuilder.Parameters
import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.PrintWriter
import java.nio.file.Path
import java.util.EnumMap
import kotlin.io.path.absolute
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime

@Disabled // The test is disabled by default because it's not intended to be executed on every CI run.
@Tag("klib")
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KlibDAGBuilderBenchmarkTest : AbstractNativeSimpleTest() {

    companion object {
        @TempDir
        private lateinit var sharedTmpDir: Path
    }

    private lateinit var reporter: BenchmarkResultsReporter
    private lateinit var testInfo: TestInfo

    @BeforeAll
    fun setUpOnce(@TempDir(cleanup = CleanupMode.NEVER) permanentTempDir: Path) {
        reporter = BenchmarkResultsReporter(permanentTempDir.resolve("KlibDAGBuilderBenchmarkTest.md"))
    }

    @BeforeEach
    fun setUpForEach(testInfo: TestInfo) {
        this.testInfo = testInfo
    }

    @AfterAll
    fun tearDownOnce() {
        reporter.writeReport()
    }

    /**
     * roots: []
     * participating libraries: stdlib + <N> platform libs
     * resulting DAG size: 1
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries only (no roots)`(mode: KlibDAGBuildingMode) {
        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = emptySet(),
            isRoot = { false },
            expectedRootsNumber = 0,
            mode,
        )
    }

    /**
     * roots: [stdlib, Foundation]
     * participating libraries: stdlib + <N> platform libs
     * resulting DAG size: 10
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries only (roots = stdlib + Foundation)`(mode: KlibDAGBuildingMode) {
        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = emptySet(),
            isRoot = { it.isNativeStdlib || it.uniqueName.endsWith(".Foundation") },
            expectedRootsNumber = 2,
            mode,
        )
    }

    /**
     * roots: 20 regular user libs
     * participating libraries: stdlib + <N> platform libs + 20 user libs
     * resulting DAG size: 21
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 20 + 0 user libs)`(mode: KlibDAGBuildingMode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 20, cInteropLibsNumber = 0)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 20,
            mode,
        )
    }

    /**
     * roots: 15 regular + 5 C-interop user libs
     * participating libraries: stdlib + <N> platform libs + 20 user libs
     * resulting DAG size: 21
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 15 + 5 user libs)`(mode: KlibDAGBuildingMode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 15, cInteropLibsNumber = 5)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 20,
            mode,
        )
    }

    /**
     * roots: 10 regular + 10 C-interop user libs
     * participating libraries: stdlib + <N> platform libs + 20 user libs
     * resulting DAG size: 21
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 10 + 10 user libs)`(mode: KlibDAGBuildingMode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 10, cInteropLibsNumber = 10)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 20,
            mode,
        )
    }

    /**
     * roots: 100 regular user libs
     * participating libraries: stdlib + <N> platform libs + 100 user libs
     * resulting DAG size: 101
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 100 + 0 user libs)`(mode: KlibDAGBuildingMode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 100, cInteropLibsNumber = 0)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 100,
            mode,
        )
    }

    /**
     * roots: 75 regular and 25 C-interop user libs
     * participating libraries: stdlib + <N> platform libs + 100 user libs
     * resulting DAG size: 101
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 75 + 25 user libs)`(mode: KlibDAGBuildingMode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 75, cInteropLibsNumber = 25)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 100,
            mode,
        )
    }

    /**
     * roots: 50 regular and 50 C-interop user libs
     * participating libraries: stdlib + <N> platform libs + 100 user libs
     * resulting DAG size: 101
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 50 + 50 user libs)`(mode: KlibDAGBuildingMode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 50, cInteropLibsNumber = 50)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 100,
            mode,
        )
    }

    private fun generateUserLibraries(regularLibsNumber: Int, cInteropLibsNumber: Int): Set<String> {
        require(regularLibsNumber >= 0)
        require(cInteropLibsNumber >= 0)
        require(regularLibsNumber + cInteropLibsNumber > 0)

        val cInteropLibraries: List<GeneratedCInteropLibrary> = generateAndCacheCInteropLibraries(cInteropLibsNumber)
        assertEquals(cInteropLibsNumber, cInteropLibraries.size)

        val regularLibraries: List<GeneratedRegularLibrary> = generateAndCacheRegularLibraries(regularLibsNumber, cInteropLibraries)
        assertEquals(regularLibsNumber, regularLibraries.size)

        val allPaths = buildSet {
            cInteropLibraries.mapTo(this) { it.path.toRealPath().pathString }
            regularLibraries.mapTo(this) { it.path.toRealPath().pathString }
        }
        assertEquals(regularLibsNumber + cInteropLibsNumber, allPaths.size)

        return allPaths
    }

    private interface GeneratedLibrary {
        val name: String
        val path: Path
    }

    private class GeneratedCInteropLibrary(val index: Int, basePath: Path) : GeneratedLibrary {
        override val name = "I$index"
        override val path: Path = basePath.resolve(name)
        val headerFilePath: Path = basePath.resolve(path.name + ".h")
    }

    /**
     * Builds `cInteropLibsNumber` C-interop modules in the following way:
     * - C-Interop module "I0"
     * - C-Interop module "I1", depends on "I0"
     * - C-Interop module "I2", depends on "I1" and "I0"
     * - C-Interop module "I3", depends on "I2" and "I1"
     * ...
     * - C-interop module "I<cInteropLibsNumber-1>", depends on "I<cInteropLibsNumber-2>" and "I<cInteropLibsNumber-3>"
     *
     * Note: The process of building of C-interop libraries is time-consuming. To speed it up, the libraries generated on
     * previous invocations of [generateAndCacheCInteropLibraries] with the same parameters are re-used.
     */
    private fun generateAndCacheCInteropLibraries(cInteropLibsNumber: Int): List<GeneratedCInteropLibrary> {
        require(cInteropLibsNumber >= 0)

        if (cInteropLibsNumber == 0) return emptyList()

        // Compute the directory where libraries should be cached:
        val generatedOrCachedLibraryBaseDir: Path = sharedTmpDir
            .resolve("interop-libs")
            .createDirectories()

        // Compute the names and paths of C-interop libraries:
        val generatedLibraries = (0 until cInteropLibsNumber).map { libraryIndex ->
            GeneratedCInteropLibrary(libraryIndex, generatedOrCachedLibraryBaseDir)
        }

        // Generate the libraries that have not been yet generated, and cache them:
        for ((index, name, path) in generatedLibraries) {
            if (!path.exists()) {
                // just take last 2 c-interop modules
                val dependencies: List<GeneratedCInteropLibrary> = generatedLibraries.subList(0, index).takeLast(2)

                newSourceModules(
                    customGeneratedSourcesDir = generatedOrCachedLibraryBaseDir,
                ) {
                    // Register already existing modules. They won't be compiled but will be used as the dependencies.
                    for (dependency in dependencies) {
                        addCInteropModule(dependency.name, dependency.path, dependency.headerFilePath)
                    }

                    addCInteropModule(name) {
                        dependsOn(dependencies.map { it.name })

                        // add 2k declarations to add "weight" to the library
                        headerFileAddend(
                            List(2000) { index -> "void ${name}_$index() {}" }.joinToString("\n")
                        )
                    }
                }.compileToKlibsViaCli(
                    customGeneratedKlibsDir = generatedOrCachedLibraryBaseDir,
                ) { module, successKlib ->
                    when (module) {
                        is CInteropKlibTestSourceModule -> {
                            assertEquals(path, successKlib.resultingArtifact.klibFile.toPath())
                            println("Generated C-interop library ${module.name}: ${path.relativeTo(generatedOrCachedLibraryBaseDir)}")
                        }

                        is CInteropKlibExistingModule -> {
                            // Do nothing.
                            println("Re-used C-interop library ${module.name}: ${path.relativeTo(generatedOrCachedLibraryBaseDir)}")
                        }

                        else -> fail("Unexpected module ${module.name}")
                    }
                }
            }
        }

        return generatedLibraries
    }

    private class GeneratedRegularLibrary(val index: Int, basePath: Path) : GeneratedLibrary {
        override val name = "R$index"
        override val path: Path = basePath.resolve(name)
    }

    /**
     * Build `regularLibsNumber` regular modules in the following way:
     * - Regular module "R0", depends on "I<cInteropLibsNumber-1>" and "I<cInteropLibsNumber-2>"
     * - Regular module "R1", depends on "R0" and "I<cInteropLibsNumber-1>"
     * - Regular module "R2", depends on "R1" and "R0"
     * ...
     * - Regular module "R<regularLibsNumber-1>", depends on "R<regularLibsNumber-2>" and "R<regularLibsNumber-3>"
     *
     * Note: The process of building of C-interop libraries is time-consuming. To speed it up, the libraries generated on
     * previous invocations of [generateAndCacheRegularLibraries] with the same parameters are re-used.
     */
    private fun generateAndCacheRegularLibraries(
        regularLibsNumber: Int,
        cInteropLibraries: List<GeneratedCInteropLibrary>,
    ): List<GeneratedRegularLibrary> {
        require(regularLibsNumber >= 0)

        if (regularLibsNumber == 0) return emptyList()

        // Compute the directory where libraries should be cached:
        val generatedOrCachedLibraryBaseDir: Path = sharedTmpDir
            .resolve("regular-libs-for-${cInteropLibraries.size}-interop-libs")
            .createDirectories()

        // Compute the names and paths of C-interop libraries:
        val generatedLibraries = (0 until regularLibsNumber).map { libraryIndex ->
            GeneratedRegularLibrary(libraryIndex, generatedOrCachedLibraryBaseDir)
        }

        for ((index, name, path) in generatedLibraries) {
            if (!path.exists()) {

                val dependencies: List<GeneratedLibrary> = buildList {
                    addAll(generatedLibraries.subList(0, index).takeLast(2)) // just take last 2 regular modules
                    addAll(cInteropLibraries.takeLast(2 - size)) // for the first 2 modules also add deps on C-interop modules
                }

                newSourceModules {
                    // Register already existing modules. They won't be compiled but will be used as the dependencies.
                    for (dependency in dependencies) {
                        when (dependency) {
                            is GeneratedRegularLibrary -> addRegularModule(dependency.name, dependency.path)
                            is GeneratedCInteropLibrary -> addCInteropModule(dependency.name, dependency.path, dependency.headerFilePath)
                            else -> fail("Unexpected dependency ${dependency.name}")
                        }
                    }

                    addRegularModule(name) {
                        dependsOn(dependencies.map { it.name })

                        // add 2k declarations to add "weight" to the library
                        // (it's approx. the number of declarations in the coroutines-core library)
                        sourceFileAddend(
                            List(2000) { index -> "fun ${name}_$index() {}" }.joinToString("\n")
                        )
                    }
                }.compileToKlibsViaCli(
                    customGeneratedKlibsDir = generatedOrCachedLibraryBaseDir,
                ) { module, successKlib ->
                    when (module) {
                        is RegularKlibTestSourceModule -> {
                            assertEquals(path, successKlib.resultingArtifact.klibFile.toPath())
                            println("Generated regular library ${module.name}: ${path.relativeTo(generatedOrCachedLibraryBaseDir)}")
                        }

                        is ExistingKlibModule -> {
                            // Do nothing.
                            println("Re-used regular library ${module.name}: ${path.relativeTo(generatedOrCachedLibraryBaseDir)}")
                        }

                        else -> fail("Unexpected module ${module.name}")
                    }
                }
            }
        }

        return generatedLibraries
    }

    private fun benchmark(
        testName: String,
        extraLibraryPaths: Set<String>,
        isRoot: (KotlinLibrary) -> Boolean,
        expectedRootsNumber: Int, // Sanity check.
        mode: KlibDAGBuildingMode,
    ) {
        repeat(2) {
            System.gc()
            Thread.sleep(100)
        }

        // Load libraries.
        val target = testRunSettings.get<KotlinNativeTargets>().testTarget

        val loadingResult = KlibLoader {
            libraryProviders(
                KlibNativeDistributionLibraryProvider(nativeHome = testRunSettings.get<KotlinNativeHome>().dir) {
                    withStdlib()
                    withPlatformLibs(target)
                }
            )
            libraryPaths(extraLibraryPaths.toList())
        }.load()

        loadingResult.reportLoadingProblemsIfAny { _, message -> fail { message } }
        assertFalse(loadingResult.hasProblems)

        val allLibraries = loadingResult.librariesStdlibFirst
        val roots = allLibraries.filter { isRoot(it) }.toSet()

        // Sanity check.
        assertEquals(expectedRootsNumber, roots.size)

        var latestDag: KlibDAG? = null

        // Run the benchmark.
        runBenchWithWarmup(
            testName = testName,
            target = target,
            numberOfLibraries = allLibraries.size,
            mode = mode,
            warmupRounds = 10,
            benchmarkRounds = 20,
            pre = System::gc,
            post = {
                println("The computed DAG size is: ${latestDag!!.librariesReverseTopoSorted.size}")
            },
        ) {
            latestDag = KlibDAGBuilder(
                Parameters(libraries = allLibraries) { it in roots }.apply {
                    @OptIn(InternalKlibDAGApi::class)
                    useSignatureIndices = mode.useSignatureIndices
                }
            ).build()
        }
    }

    @Suppress("SameParameterValue")
    private fun runBenchWithWarmup(
        testName: String,
        target: KonanTarget,
        numberOfLibraries: Int,
        mode: KlibDAGBuildingMode,
        warmupRounds: Int,
        benchmarkRounds: Int,
        pre: () -> Unit,
        post: () -> Unit,
        bench: () -> Unit,
    ) {
        val displayTestName = "$testName ($target, $numberOfLibraries libraries)"

        println("Run [$displayTestName] benchmark")
        println("Warmup: $warmupRounds times...")

        repeat(warmupRounds) {
            println("W: ${it + 1} out of $warmupRounds")
            pre()
            bench()
            post()
        }

        val measurements = ArrayList<Duration>(benchmarkRounds)

        println("Run bench: $benchmarkRounds times...")

        repeat(benchmarkRounds) {
            print("B: ${it + 1} out of $benchmarkRounds ")
            pre()
            val duration = measureTime { bench() }
            println("takes $duration")
            post()
            measurements += duration
        }

        val averageDuration = (measurements.fold(Duration.ZERO) { a, b -> a + b } / benchmarkRounds)
        val medianDuration = measurements.sorted()[benchmarkRounds / 2]

        println("[$displayTestName] average duration is ${averageDuration.toPrettyString()}")
        println("[$displayTestName] median duration is ${medianDuration.toPrettyString()}")

        reporter.logMeasurements(
            testName = testName,
            target = target,
            numberOfLibraries = numberOfLibraries,
            mode = mode,
            medianDuration = medianDuration,
        )
    }
}

private fun Duration.toPrettyString(): String {
    val displayUnit = listOf(
        DurationUnit.DAYS,
        DurationUnit.HOURS,
        DurationUnit.MINUTES,
        DurationUnit.SECONDS,
        DurationUnit.MILLISECONDS,
        DurationUnit.MICROSECONDS,
        DurationUnit.NANOSECONDS,
    ).firstOrNull { absoluteValue.toDouble(it) >= 1.0 } ?: DurationUnit.NANOSECONDS

    val stringValue = toString(displayUnit, decimals = 2)
    val i = stringValue.indexOfFirst { it.isLetter() }
    return if (i != -1) stringValue.substring(0, i) + " " + stringValue.substring(i) else stringValue
}

private class BenchmarkResultsReporter(private val reportPath: Path) {
    private data class TestKey(
        val testName: String,
        val rootsNumber: Int,
        val target: KonanTarget,
    ) : Comparable<TestKey> {
        override fun compareTo(other: TestKey): Int {
            rootsNumber.compareTo(other.rootsNumber).let { if (it != 0) return it }
            testName.compareTo(other.testName).let { if (it != 0) return it }
            return target.name.compareTo(other.target.name)
        }
    }

    private val measurements: MutableMap<TestKey, MutableMap<KlibDAGBuildingMode, Duration>> = hashMapOf()

    fun logMeasurements(
        testName: String,
        target: KonanTarget,
        numberOfLibraries: Int,
        mode: KlibDAGBuildingMode,
        medianDuration: Duration,
    ) {
        measurements.getOrPut(TestKey(testName, numberOfLibraries, target)) { EnumMap(KlibDAGBuildingMode::class.java) }[mode] = medianDuration
    }

    fun writeReport() {
        PrintWriter(reportPath.bufferedWriter()).use { writer ->
            fun printRow(items: List<String>) {
                writer.println(items.joinToString(prefix = "| ", postfix = " |", separator = " | "))
            }

            fun printRow(first: String, others: List<String>) {
                printRow(listOf(first) + others)
            }

            fun printHeader(first: String, others: List<String>) {
                val items = listOf(first) + others
                printRow(items)
                writer.println(items.joinToString(prefix = "|", postfix = "|", separator = "|") { "---" })
            }

            writer.println("# Measurements of KLIB DAG generation")
            writer.println()
            writer.println("* Date & time: ${Clock.System.now()}")
            writer.println("* OS: ${System.getProperty("os.name")} (${System.getProperty("os.version")}), ${System.getProperty("os.arch")}")
            writer.println("* CPU cores: ${Runtime.getRuntime().availableProcessors()}")
            writer.println("* Max heap: ${Runtime.getRuntime().maxMemory() / (1024 * 1024)} MiB")

            val allTargets = measurements.keys.mapToSetOrEmpty { it.target }
            for (currentTarget in allTargets) {
                writer.println()
                writer.println("### Target: $currentTarget")

                printHeader("Test name", KlibDAGBuildingMode.entries.map { "$it<br/>(median time)" })

                for (testKey in measurements.keys.filter { it.target == currentTarget }.sorted()) {
                    val modeAndDuration: Map<KlibDAGBuildingMode, Duration> = measurements.getValue(testKey)
                    val durations: List<Duration?> = KlibDAGBuildingMode.entries.map(modeAndDuration::get)

                    val renderedDurations = durations.mapIndexed { index, duration ->
                        if (duration == null)
                            ""
                        else buildString {
                            append(duration.toPrettyString())

                            val firstDuration = durations.first()
                            if (firstDuration != null) {
                                append("<br/>")
                                if (index == 0)
                                    append("100")
                                else {
                                    val ratio = duration / firstDuration
                                    when {
                                        ratio < 1.0 -> append("↓ ")
                                        ratio > 1.0 -> append("↑ ")
                                        else -> Unit
                                    }
                                    append(String.format("%.2f", ratio * 100.0))
                                }
                                append("%")
                            }
                        }
                    }

                    printRow(testKey.testName, renderedDurations)
                }
            }
        }

        println("Benchmarks report is available here: ${reportPath.absolute().toUri().toASCIIString()}")
    }
}
