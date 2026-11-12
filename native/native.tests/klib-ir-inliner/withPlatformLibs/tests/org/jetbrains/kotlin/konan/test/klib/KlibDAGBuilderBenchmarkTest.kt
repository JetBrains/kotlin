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
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
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
import kotlin.math.max
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
    fun `stdlib and platform libs only (no roots)`(mode: KlibDAGBuildingBenchmarkMode) {
        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = emptySet(),
            isRoot = { false },
            expectedRootsNumber = 0,
            mode,
            configuration = null,
        )
    }

    /**
     * roots: [stdlib, Foundation]
     * participating libraries: stdlib + <N> platform libs
     * resulting DAG size: 10
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libs only (roots = stdlib + Foundation)`(mode: KlibDAGBuildingBenchmarkMode) {
        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = emptySet(),
            isRoot = { it.isNativeStdlib || it.uniqueName.endsWith(".Foundation") },
            expectedRootsNumber = 2,
            mode,
            configuration = null,
        )
    }

    /**
     * roots: [KlibDAGUserLibsConfiguration.totalLibsNumber] user libs
     * participating libraries: stdlib + <N> platform libs + [KlibDAGUserLibsConfiguration.totalLibsNumber] user libs
     * resulting DAG size: 1+[KlibDAGUserLibsConfiguration.totalLibsNumber]
     */
    @ParameterizedTest
    @MethodSource("modeAndUserLibsConfiguration")
    fun `stdlib, platform and user libs`(
        mode: KlibDAGBuildingBenchmarkMode,
        userLibsConfiguration: KlibDAGUserLibsConfiguration,
    ) {
        val userLibraryPaths = generateUserLibraries(mode, userLibsConfiguration)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = userLibsConfiguration.totalLibsNumber,
            mode,
            userLibsConfiguration,
        )
    }

    private fun generateUserLibraries(
        mode: KlibDAGBuildingBenchmarkMode,
        userLibsConfiguration: KlibDAGUserLibsConfiguration,
    ): Set<String> = with(mode) {
        with(userLibsConfiguration) {
            val maxNumberOfDependencies = max(1, ((regularLibsNumber + cInteropLibsNumber) * density).toInt())

            val cInteropLibraries: List<GeneratedCInteropLibrary> = generateAndCacheCInteropLibraries(cInteropLibsNumber, maxNumberOfDependencies)
            assertEquals(cInteropLibsNumber, cInteropLibraries.size)

            val regularLibraries: List<GeneratedRegularLibrary> = generateAndCacheRegularLibraries(regularLibsNumber, maxNumberOfDependencies, cInteropLibraries)
            assertEquals(regularLibsNumber, regularLibraries.size)

            val allPaths = buildSet {
                cInteropLibraries.mapTo(this) { it.path.toRealPath().pathString }
                regularLibraries.mapTo(this) { it.path.toRealPath().pathString }
            }
            assertEquals(regularLibsNumber + cInteropLibsNumber, allPaths.size)

            return allPaths
        }
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
    private fun generateAndCacheCInteropLibraries(
        cInteropLibsNumber: Int,
        maxNumberOfDependencies: Int,
    ): List<GeneratedCInteropLibrary> {
        require(cInteropLibsNumber >= 0)
        require(maxNumberOfDependencies > 0)

        if (cInteropLibsNumber == 0) return emptyList()

        // Compute the directory where libraries should be cached:
        val generatedOrCachedLibraryBaseDir: Path = sharedTmpDir
            .resolve("interop-libs-with-$maxNumberOfDependencies-deps")
            .createDirectories()

        // Compute the names and paths of C-interop libraries:
        val generatedLibraries = (0 until cInteropLibsNumber).map { libraryIndex ->
            GeneratedCInteropLibrary(libraryIndex, generatedOrCachedLibraryBaseDir)
        }

        // Generate the libraries that have not been yet generated, and cache them:
        for ((index, name, path) in generatedLibraries) {
            if (!path.exists()) {
                // just take last `maxNumberOfDependencies` c-interop modules
                val dependencies: List<GeneratedCInteropLibrary> = generatedLibraries.subList(0, index).takeLast(maxNumberOfDependencies)

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
        maxNumberOfDependencies: Int,
        cInteropLibraries: List<GeneratedCInteropLibrary>,
    ): List<GeneratedRegularLibrary> {
        require(regularLibsNumber >= 0)
        require(maxNumberOfDependencies > 0)

        if (regularLibsNumber == 0) return emptyList()

        // Compute the directory where libraries should be cached:
        val generatedOrCachedLibraryBaseDir: Path = sharedTmpDir
            .resolve("regular-libs-for-${cInteropLibraries.size}-interop-libs-with-$maxNumberOfDependencies-deps")
            .createDirectories()

        // Compute the names and paths of C-interop libraries:
        val generatedLibraries = (0 until regularLibsNumber).map { libraryIndex ->
            GeneratedRegularLibrary(libraryIndex, generatedOrCachedLibraryBaseDir)
        }

        for ((index, name, path) in generatedLibraries) {
            if (!path.exists()) {

                val dependencies: List<GeneratedLibrary> = buildList {
                    addAll(generatedLibraries.subList(0, index).takeLast(maxNumberOfDependencies)) // just take last `maxNumberOfDependencies` regular modules
                    addAll(cInteropLibraries.takeLast(maxNumberOfDependencies - size)) // for the first `maxNumberOfDependencies` modules also add deps on C-interop modules
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
        mode: KlibDAGBuildingBenchmarkMode,
        configuration: KlibDAGUserLibsConfiguration?,
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
            mode = mode,
            configuration = configuration,
            warmupRounds = 10,
            benchmarkRounds = 40,
            pre = System::gc,
            post = {
                println("The computed DAG size is: ${latestDag!!.librariesReverseTopoSorted.size}")
            },
        ) {
            latestDag = KlibDAGBuilder(
                Parameters(libraries = allLibraries) { it in roots }.apply {
                    @OptIn(InternalKlibDAGApi::class)
                    useSignatureIndices = mode.dagMode.useSignatureIndices
                }
            ).build()
        }
    }

    @Suppress("SameParameterValue")
    private fun runBenchWithWarmup(
        testName: String,
        target: KonanTarget,
        mode: KlibDAGBuildingBenchmarkMode,
        configuration: KlibDAGUserLibsConfiguration?,
        warmupRounds: Int,
        benchmarkRounds: Int,
        pre: () -> Unit,
        post: () -> Unit,
        bench: () -> Unit,
    ) {
        val displayTestName = "$testName ($target${if (configuration != null) ", $configuration" else ""})"

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
            mode = mode,
            configuration = configuration,
            medianDuration = medianDuration,
        )
    }

    fun modeAndUserLibsConfiguration(): List<Arguments> = KlibDAGUserLibsConfiguration.entries.flatMap { configuration ->
        KlibDAGBuildingBenchmarkMode.entries.map { mode -> Arguments.of(mode, configuration) }
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
        val configuration: KlibDAGUserLibsConfiguration?,
        val target: KonanTarget,
    ) : Comparable<TestKey> {
        override fun compareTo(other: TestKey): Int {
            testName.compareTo(other.testName).let { if (it != 0) return it }
            compareValues(configuration, other.configuration).let { if (it != 0) return it }
            return target.name.compareTo(other.target.name)
        }
    }

    private val measurements: MutableMap<TestKey, MutableMap<KlibDAGBuildingBenchmarkMode, Duration>> = hashMapOf()

    fun logMeasurements(
        testName: String,
        target: KonanTarget,
        mode: KlibDAGBuildingBenchmarkMode,
        configuration: KlibDAGUserLibsConfiguration?,
        medianDuration: Duration,
    ) {
        measurements.getOrPut(TestKey(testName, configuration, target)) {
            EnumMap(KlibDAGBuildingBenchmarkMode::class.java)
        }[mode] = medianDuration
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

                printHeader("Test name", KlibDAGBuildingBenchmarkMode.entries.map { "$it<br/>(median time)" })

                for (testKey in measurements.keys.filter { it.target == currentTarget }.sorted()) {
                    val modeAndDuration: Map<KlibDAGBuildingBenchmarkMode, Duration> = measurements.getValue(testKey)
                    val durations: List<Duration?> = KlibDAGBuildingBenchmarkMode.entries.map(modeAndDuration::get)

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

                    printRow(testKey.testName + testKey.configuration?.let { "<br/>${it}" }.orEmpty(), renderedDurations)
                }
            }
        }

        println("Benchmarks report is available here: ${reportPath.absolute().toUri().toASCIIString()}")
    }
}

@Suppress("unused") // Enum entries are not used directly in the source code.
enum class KlibDAGBuildingBenchmarkMode(
    val dagMode: KlibDAGBuildingMode,
    val density: Double,
) {
    NO_INDICES(dagMode = KlibDAGBuildingMode.NO_INDICES, density = 0.1),
    LOW_DENSITY(dagMode = KlibDAGBuildingMode.WITH_INDICES, density = 0.1),
    MEDIUM_DENSITY(dagMode = KlibDAGBuildingMode.WITH_INDICES, density = 0.5),
    HIGH_DENSITY(dagMode = KlibDAGBuildingMode.WITH_INDICES, density = 0.9);

    init {
        check(density > 0 && density <= 1.0)
    }

    override fun toString() = "$dagMode${"/d=%.2f".format(density)}"
}

@Suppress("unused") // Enum entries are not used directly in the source code.
enum class KlibDAGUserLibsConfiguration(
    val regularLibsNumber: Int,
    val cInteropLibsNumber: Int,
) {
    REGULAR_20(20, 0),
    REGULAR_15_CINTEROP_5(15, 5),
    REGULAR_10_CINTEROP_10(10, 10),
    REGULAR_100(100, 0),
    REGULAR_75_CINTEROP_25(75, 25),
    REGULAR_50_CINTEROP_50(50, 50),
    ;

    init {
        check(regularLibsNumber > 0)
        check(cInteropLibsNumber >= 0)
    }

    val totalLibsNumber get() = regularLibsNumber + cInteropLibsNumber

    override fun toString() = buildString {
        append("roots = $regularLibsNumber regular")
        if (cInteropLibsNumber > 0) {
            append(" + $cInteropLibsNumber C-interop")
        }
        append(" libs")
    }
}
