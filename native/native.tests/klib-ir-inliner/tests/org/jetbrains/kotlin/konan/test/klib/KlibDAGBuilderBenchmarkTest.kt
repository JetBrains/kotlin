/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.backend.konan.library.KlibDAG
import org.jetbrains.kotlin.backend.konan.library.KlibDAGBuilder
import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.uniqueName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively
import kotlin.io.path.pathString
import kotlin.time.Duration
import kotlin.time.measureTime

@Disabled // The test is disabled by default because it's not intended to be executed on every CI run.
@Tag("klib")
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class KlibDAGBuilderBenchmarkTest : AbstractNativeSimpleTest() {

    private lateinit var testInfo: TestInfo

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        this.testInfo = testInfo
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - roots: []
     * - target: macos_arm64
     * - number of libraries: 177 (stdlib + platform libs)
     * - resulting DAG size: 1
     * - median duration: < 1ms (any mode)
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries only (no roots)`(mode: KlibDAGBuildingMode) = context(mode) {
        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = emptySet(),
            isRoot = { false },
            expectedRootsNumber = 0,
        )

        assertExactNumberOfExternalIndices(0)
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - roots: [stdlib, Foundation]
     * - target: macos_arm64
     * - number of libraries: 177 (stdlib + platform libs)
     * - resulting DAG size: 10
     * - median duration:
     *   - NoIndices: 665 ms
     *   - all other modes: ~4 ms
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries only (roots = stdlib + Foundation)`(mode: KlibDAGBuildingMode) = context(mode) {
        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = emptySet(),
            isRoot = { it.isNativeStdlib || it.uniqueName.endsWith(".Foundation") },
            expectedRootsNumber = 2,
        )

        assertExactNumberOfExternalIndices(0)
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - roots: 20 regular user libs
     * - target: macos_arm64
     * - number of libraries: 197 (stdlib + platform libs + 20 user libs)
     * - resulting DAG size: 21
     * - median duration:
     *   - NoIndices: 11 ms
     *   - OwnIndicesEverywhere: 5 ms
     *   - OwnIndicesForDistNoIndicesForOthers: 10 ms
     *   - OwnIndicesForDistExternalIndicesForOthers: 8 ms
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 20 + 0 user libs)`(mode: KlibDAGBuildingMode) = context(mode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 20, cInteropLibsNumber = 0)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 20,
        )

        assertExactNumberOfExternalIndices(librariesWithoutOwnIndices = 20)
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - roots: 20 regular user libs
     * - target: macos_arm64
     * - number of libraries: 197 (stdlib + platform libs + 20 user libs)
     * - resulting DAG size: 21
     * - median duration:
     *   - NoIndices: 25 ms
     *   - OwnIndicesEverywhere: 5 ms
     *   - OwnIndicesForDistNoIndicesForOthers: 24 ms
     *   - OwnIndicesForDistExternalIndicesForOthers: 8 ms
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 19 + 1 user libs)`(mode: KlibDAGBuildingMode) = context(mode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 19, cInteropLibsNumber = 1)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 20,
        )

        assertExactNumberOfExternalIndices(librariesWithoutOwnIndices = 20)
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - roots: 20 regular user libs
     * - target: macos_arm64
     * - number of libraries: 197 (stdlib + platform libs + 20 user libs)
     * - resulting DAG size: 21
     * - median duration:
     *   - NoIndices: 27 ms
     *   - OwnIndicesEverywhere: 5 ms
     *   - OwnIndicesForDistNoIndicesForOthers: 26 ms
     *   - OwnIndicesForDistExternalIndicesForOthers: 8 ms
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 18 + 2 user libs)`(mode: KlibDAGBuildingMode) = context(mode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 18, cInteropLibsNumber = 2)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 20,
        )

        assertExactNumberOfExternalIndices(librariesWithoutOwnIndices = 20)
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - roots: 15 regular + 5 C-interop user libs
     * - target: macos_arm64
     * - number of libraries: 197 (stdlib + platform libs + 20 user libs)
     * - resulting DAG size: 21
     * - median duration:
     *   - NoIndices: 70 ms
     *   - OwnIndicesEverywhere: 6 ms
     *   - OwnIndicesForDistNoIndicesForOthers: 68 ms
     *   - OwnIndicesForDistExternalIndicesForOthers: 9 ms
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 15 + 5 user libs)`(mode: KlibDAGBuildingMode) = context(mode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 15, cInteropLibsNumber = 5)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 20,
        )

        assertExactNumberOfExternalIndices(librariesWithoutOwnIndices = 20)
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - roots: 10 regular + 10 C-interop user libs
     * - target: macos_arm64
     * - number of libraries: 197 (stdlib + platform libs + 20 user libs)
     * - resulting DAG size: 21
     * - median duration:
     *   - NoIndices: 155 ms
     *   - OwnIndicesEverywhere: 9 ms
     *   - OwnIndicesForDistNoIndicesForOthers: 154 ms
     *   - OwnIndicesForDistExternalIndicesForOthers: 15 ms
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 10 + 10 user libs)`(mode: KlibDAGBuildingMode) = context(mode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 10, cInteropLibsNumber = 10)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 20,
        )

        assertExactNumberOfExternalIndices(librariesWithoutOwnIndices = 20)
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - roots: 100 regular user libs
     * - target: macos_arm64
     * - number of libraries: 277 (stdlib + platform libs + 100 user libs)
     * - resulting DAG size: 101
     * - median duration:
     *   - NoIndices: 57 ms
     *   - OwnIndicesEverywhere: 25 ms
     *   - OwnIndicesForDistNoIndicesForOthers: 54 ms
     *   - OwnIndicesForDistExternalIndicesForOthers: 42 ms
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 100 + 0 user libs)`(mode: KlibDAGBuildingMode) = context(mode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 100, cInteropLibsNumber = 0)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 100,
        )

        assertExactNumberOfExternalIndices(librariesWithoutOwnIndices = 100)
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - roots: 100 regular user libs
     * - target: macos_arm64
     * - number of libraries: 277 (stdlib + platform libs + 100 user libs)
     * - resulting DAG size: 101
     * - median duration:
     *   - NoIndices: 59 ms
     *   - OwnIndicesEverywhere: 24 ms
     *   - OwnIndicesForDistNoIndicesForOthers: 56 ms
     *   - OwnIndicesForDistExternalIndicesForOthers: 43 ms
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 99 + 1 user libs)`(mode: KlibDAGBuildingMode) = context(mode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 99, cInteropLibsNumber = 1)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 100,
        )

        assertExactNumberOfExternalIndices(librariesWithoutOwnIndices = 100)
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - roots: 100 regular user libs
     * - target: macos_arm64
     * - number of libraries: 277 (stdlib + platform libs + 100 user libs)
     * - resulting DAG size: 101
     * - median duration:
     *   - NoIndices: 66 ms
     *   - OwnIndicesEverywhere: 24 ms
     *   - OwnIndicesForDistNoIndicesForOthers: 64 ms
     *   - OwnIndicesForDistExternalIndicesForOthers: 44 ms
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 98 + 2 user libs)`(mode: KlibDAGBuildingMode) = context(mode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 98, cInteropLibsNumber = 2)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 100,
        )

        assertExactNumberOfExternalIndices(librariesWithoutOwnIndices = 100)
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - roots: 75 regular and 25 C-interop user libs
     * - target: macos_arm64
     * - number of libraries: 277 (stdlib + platform libs + 100 user libs)
     * - resulting DAG size: 101
     * - median duration:
     *   - NoIndices: 96 ms
     *   - OwnIndicesEverywhere: 23 ms
     *   - OwnIndicesForDistNoIndicesForOthers: 94 ms
     *   - OwnIndicesForDistExternalIndicesForOthers: 41 ms
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 95 + 5 user libs)`(mode: KlibDAGBuildingMode) = context(mode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 95, cInteropLibsNumber = 5)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 100,
        )

        assertExactNumberOfExternalIndices(librariesWithoutOwnIndices = 100)
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - roots: 50 regular and 50 C-interop user libs
     * - target: macos_arm64
     * - number of libraries: 277 (stdlib + platform libs + 100 user libs)
     * - resulting DAG size: 101
     * - median duration:
     *   - NoIndices: 206 ms
     *   - OwnIndicesEverywhere: 24 ms
     *   - OwnIndicesForDistNoIndicesForOthers: 206 ms
     *   - OwnIndicesForDistExternalIndicesForOthers: 43 ms
     */
    @ParameterizedTest
    @EnumSource
    fun `stdlib and platform libraries (roots = 90 + 10 user libs)`(mode: KlibDAGBuildingMode) = context(mode) {
        val userLibraryPaths = generateUserLibraries(regularLibsNumber = 90, cInteropLibsNumber = 10)

        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = userLibraryPaths,
            isRoot = { it.canonicalPath.pathString in userLibraryPaths },
            expectedRootsNumber = 100,
        )

        assertExactNumberOfExternalIndices(librariesWithoutOwnIndices = 100)
    }

    context(mode: KlibDAGBuildingMode)
    private fun generateUserLibraries(regularLibsNumber: Int, cInteropLibsNumber: Int): Set<String> {
        require(regularLibsNumber + cInteropLibsNumber > 0)

        val generatedLibraries = hashSetOf<String>()

        /*
         * Build `regularLibsNumber` regular modules and `cInteropLibsNumber` C-interop modules in the following way:
         * - C-Interop module "I0"
         * - C-Interop module "I1", depends on "I0"
         * - C-Interop module "I2", depends on "I1" and "I0"
         * - C-Interop module "I3", depends on "I2" and "I1"
         * ...
         * - C-interop module "I<cInteropLibsNumber-1>", depends on "I<cInteropLibsNumber-2>" and "I<cInteropLibsNumber-3>"
         * - Regular module "R0", depends on "I<cInteropLibsNumber-1>" and "I<cInteropLibsNumber-2>"
         * - Regular module "R1", depends on "R0" and "I<cInteropLibsNumber-1>"
         * - Regular module "R2", depends on "R1" and "R0"
         * ...
         * - Regular module "R<regularLibsNumber-1>", depends on "R<regularLibsNumber-2>" and "R<regularLibsNumber-3>"
         */
        newSourceModules {
            val interopModuleNames = mutableListOf<String>()

            for (moduleIndex in 0 until cInteropLibsNumber) {
                val thisModuleName = "I$moduleIndex"
                val dependencyModuleNames = interopModuleNames.takeLast(2) // just take last 2 c-interop modules

                addCInteropModule(thisModuleName) {
                    for (dependency in dependencyModuleNames) dependsOn(dependency)

                    // add 2k declarations to add "weight" to the library
                    headerFileAddend(
                        List(2000) { index -> "void ${thisModuleName}_$index() {}" }.joinToString("\n")
                    )
                }

                interopModuleNames += thisModuleName
            }

            val regularModuleNames = mutableListOf<String>()

            for (moduleIndex in 0 until regularLibsNumber) {
                val thisModuleName = "R$moduleIndex"
                val dependencyModuleNames = buildList {
                    addAll(regularModuleNames.takeLast(2)) // just take last 2 regular modules
                    addAll(interopModuleNames.takeLast(2 - size)) // for the first 2 modules also add deps on C-interop modules
                }

                addRegularModule(thisModuleName) {
                    for (dependency in dependencyModuleNames) dependsOn(dependency)

                    // add 2k declarations to add "weight" to the library
                    // (it's approx. the number of declarations in the coroutines-core library)
                    sourceFileAddend(
                        List(2000) { index -> "fun ${thisModuleName}_$index() {}" }.joinToString("\n")
                    )
                }

                regularModuleNames += thisModuleName
            }
        }.compileToKlibsViaCli { _, successKlib -> generatedLibraries.add(successKlib.resultingArtifact.klibFile.canonicalPath) }

        assertEquals(regularLibsNumber + cInteropLibsNumber, generatedLibraries.size)

        patchLibrariesToDropOwnIndicesIfNecessary(generatedLibraries.map(::Path))

        return generatedLibraries
    }

    @OptIn(ExperimentalPathApi::class)
    context(mode: KlibDAGBuildingMode)
    private fun benchmark(
        testName: String,
        extraLibraryPaths: Set<String>,
        isRoot: (KotlinLibrary) -> Boolean,
        expectedRootsNumber: Int, // Sanity check.
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

        // Drop external indices if there are any.
        externalSignatureIndicesDir.deleteRecursively()

        var latestDag: KlibDAG? = null

        // Run the benchmark.
        runBenchWithWarmup(
            name = "$testName ($target, ${allLibraries.size} libraries)",
            warmupRounds = 10,
            benchmarkRounds = 20,
            pre = System::gc,
            post = {
                println("The computed DAG size is: ${latestDag!!.librariesReverseTopoSorted.size}")
            },
        ) {
            latestDag = KlibDAGBuilder(buildParams(allLibraries) { it in roots }).build()
        }
    }

    @Suppress("SameParameterValue")
    private fun runBenchWithWarmup(
        name: String,
        warmupRounds: Int,
        benchmarkRounds: Int,
        pre: () -> Unit,
        post: () -> Unit,
        bench: () -> Unit,
    ) {
        println("Run [$name] benchmark")
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

        val averageDuration = measurements.fold(Duration.ZERO) { a, b -> a + b } / benchmarkRounds
        val medianDuration = measurements.sorted()[benchmarkRounds / 2]

        println("[$name] average duration is $averageDuration")
        println("[$name] median duration is $medianDuration")
    }
}
