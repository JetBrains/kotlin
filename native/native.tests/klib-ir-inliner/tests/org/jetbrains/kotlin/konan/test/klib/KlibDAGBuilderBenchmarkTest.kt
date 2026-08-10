/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.backend.konan.library.KlibDAGBuilder
import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.time.Duration
import kotlin.time.measureTime

@Disabled // The test is disabled by default because it's not intended to be executed on every CI run.
@Tag("klib")
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class KlibDAGBuilderBenchmarkTest : AbstractNativeSimpleTest() {

    /**
     * Benchmarking results (Apple M2 Max):
     * - target: macos_arm64
     * - number of libraries: 177 (stdlib + platform libs)
     * - average duration is 2.97s
     * - median duration is 2.97s
     */
    @Test
    fun `stdlib and platform libraries only`(testInfo: TestInfo) {
        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = emptyList(),
        )
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - target: macos_arm64
     * - number of libraries: 197 (stdlib + platform libs + 20 user libs)
     * - average duration is 3.17s
     * - median duration is 3.17s
     */
    @Test
    fun `stdlib and platform libraries with 20 user libraries`(testInfo: TestInfo) {
        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = generateUserLibraries(regularLibsNumber = 15, cInteropLibsNumber = 5),
        )
    }

    /**
     * Benchmarking results (Apple M2 Max):
     * - target: macos_arm64
     * - number of libraries: 277 (stdlib + platform libs + 100 user libs)
     * - average duration is 6.48s
     * - median duration is 6.49s
     */
    @Test
    fun `stdlib and platform libraries with 100 user libraries`(testInfo: TestInfo) {
        benchmark(
            testName = testInfo.testMethod.get().name,
            extraLibraryPaths = generateUserLibraries(regularLibsNumber = 75, cInteropLibsNumber = 25),
        )
    }

    private fun generateUserLibraries(regularLibsNumber: Int, cInteropLibsNumber: Int): List<Path> {
        require(regularLibsNumber > 0)
        require(cInteropLibsNumber > 0)

        val generatedLibraries = mutableListOf<Path>()

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
        }.compileToKlibsViaCli { _, successKlib -> generatedLibraries.add(successKlib.resultingArtifact.klibFile.toPath()) }

        assertEquals(regularLibsNumber + cInteropLibsNumber, generatedLibraries.size)

        return generatedLibraries
    }

    private fun benchmark(testName: String, extraLibraryPaths: List<Path>) {
        // Load libraries.
        val target = testRunSettings.get<KotlinNativeTargets>().testTarget

        val loadingResult = KlibLoader {
            libraryProviders(
                KlibNativeDistributionLibraryProvider(nativeHome = testRunSettings.get<KotlinNativeHome>().dir) {
                    withStdlib()
                    withPlatformLibs(target)
                }
            )
            libraryPaths(extraLibraryPaths.map { it.pathString })
        }.load()

        loadingResult.reportLoadingProblemsIfAny { _, message -> fail { message } }
        assertFalse(loadingResult.hasProblems)

        val libraries = loadingResult.librariesStdlibFirst

        // Run the benchmark.
        runBenchWithWarmup(
            name = "$testName ($target, ${libraries.size} libraries)",
            warmupRounds = 10,
            benchmarkRounds = 5,
            pre = System::gc,
            post = {},
        ) {
            KlibDAGBuilder.build(libraries)
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
