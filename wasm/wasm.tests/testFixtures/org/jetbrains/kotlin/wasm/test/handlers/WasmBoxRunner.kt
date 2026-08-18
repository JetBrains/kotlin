/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.impl.shouldIsolateTestInGroupingConfiguration
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.WasmCompilationSet
import org.jetbrains.kotlin.test.model.WasmCompilationSetsBinaryArtifact
import org.jetbrains.kotlin.test.model.WasmFolderBinaryArtifact
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator.Companion.WASM_BASE_FILE_NAME
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.testInfraError
import java.io.File

internal fun WasmCompilerResult.writeTo(outputDir: File, outputFilenameBase: String, debugMode: DebugMode, mode: String = "") {
    writeCompilationResult(this, outputDir, outputFilenameBase)
    if (debugMode >= DebugMode.DEBUG) {
        val outputPath = outputDir.absolutePath
        println(" ------ $mode Wat  file://$outputPath/$outputFilenameBase.wat")
        println(" ------ $mode Wasm file://$outputPath/$outputFilenameBase.wasm")
        println(" ------ $mode JS   file://$outputPath/$outputFilenameBase.import-object.mjs")
        println(" ------ $mode JS   file://$outputPath/$outputFilenameBase.mjs")
    }
}

open class WasmBoxRunner(
    testServices: TestServices,
    executeWithV8Only: Boolean = false,
) : WasmBoxRunnerBase(testServices, executeWithV8Only) {

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmCode(modulesToArtifact.values.single() as WasmCompilationSetsBinaryArtifact)
        }
    }

    fun runWasmCode(
        artifacts: WasmCompilationSetsBinaryArtifact,
        useUnitTestRunnerOnly: Boolean = false,
        outputCollector: MutableList<String>? = null,
        // Whether the collected exceptions should be thrown inline here. The grouping handlers set this
        // to `false` so they can re-attribute failures to the specific per-test grouping input. By default
        // it follows `useUnitTestRunnerOnly` (the unit-test grouping path collects and re-attributes; the
        // standalone box-export path throws directly).
        throwOnExceptions: Boolean = !useUnitTestRunnerOnly,
    ): List<Throwable> {
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
        val testFileText = originalFile.readText()

        fun writeToFilesAndRunTest(mode: String, result: WasmCompilationSet): List<Throwable> {
            val outputDir = testServices.getWasmTestOutputDirectoryForMode(mode)
            outputDir.mkdirs()

            result.compilerResult.writeTo(outputDir, WASM_BASE_FILE_NAME, debugMode, mode)
            result.compilationDependencies.forEach {
                it.compilerResult.writeTo(outputDir, it.compilerResult.baseFileName, debugMode, mode)
            }

            val filesToIgnoreInSizeChecks = mutableSetOf<File>()
            val exceptions = saveAdditionalFilesAndRun(
                outputDir = outputDir,
                mark = mode,
                filesToIgnoreInSizeChecks = filesToIgnoreInSizeChecks,
                useUnitTestRunnerOnly = useUnitTestRunnerOnly,
                outputCollector = outputCollector,
            )

            return exceptions + when (mode) {
                "dce" -> checkExpectedDceOutputSize(debugMode, testFileText, outputDir, filesToIgnoreInSizeChecks)
                "optimized" -> checkExpectedOptimizedOutputSize(debugMode, testFileText, outputDir, filesToIgnoreInSizeChecks)
                "dev" -> emptyList() // no additional checks required
                else -> testInfraError("Unknown mode: $mode")
            }
        }

        val allExceptions = mutableListOf<Throwable>()

        allExceptions.addAll(writeToFilesAndRunTest("dev", artifacts.compilation))

        artifacts.dceCompilation?.let {
            allExceptions.addAll(writeToFilesAndRunTest("dce", it))
        }

        artifacts.optimisedCompilation?.let {
            allExceptions.addAll(writeToFilesAndRunTest("optimized", it))
        }

        if (throwOnExceptions) {
            processExceptions(allExceptions)
        }

        return allExceptions
    }
}

class WasmFolderBoxRunner(
    testServices: TestServices,
    executeWithV8Only: Boolean,
) : WasmBoxRunnerBase(testServices, executeWithV8Only) {

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmFolder(modulesToArtifact.values.single() as WasmFolderBinaryArtifact)
        }
    }

    fun runWasmFolder(artifacts: WasmFolderBinaryArtifact) {
        val throwables = saveAdditionalFilesAndRun(artifacts.folder, "dev", mutableSetOf())
        if (throwables.isNotEmpty())
            throw throwables.first()
    }
}

open class WasmFolderGroupingStageBoxRunner(
    testServices: TestServices
) : AbstractWasmGroupingStageBoxRunner(testServices) {
    // Whether the box should be executed on V8 only. Some test runners (e.g. klib compatibility tests) set up
    // only the V8 engine, so the other engines (SpiderMonkey, JavaScriptCore) must not be referenced there.
    protected open val executeWithV8Only: Boolean = false
    private val firstNonGroupingTestServices: TestServices
        get() = testServices.groupingStageInputs.first().testServices
    private val wasmFolderBoxRunner: WasmFolderBoxRunner
        get() = WasmFolderBoxRunner(firstNonGroupingTestServices, executeWithV8Only = executeWithV8Only)

    override fun shouldUseBoxExportMode(): Boolean =
        firstNonGroupingTestServices.shouldIsolateTestInGroupingConfiguration(fileGenerationPhase = true) &&
                RUN_UNIT_TESTS !in firstNonGroupingTestServices.moduleStructure.allDirectives &&
                hasBoxMethod(testServices.groupingStageInputs.first())

    override fun runTestCode(
        artifact: BinaryArtifacts.Wasm,
        useUnitTestRunnerOnly: Boolean,
        outputCollector: MutableList<String>?,
    ): List<Throwable> {
        val folder = (artifact as WasmFolderBinaryArtifact).folder
        return wasmFolderBoxRunner.saveAdditionalFilesAndRun(
            folder, "dev", mutableSetOf(),
            useUnitTestRunnerOnly = useUnitTestRunnerOnly,
            outputCollector = outputCollector,
        )
    }
}

class WasmStackSwitchingRunner(
    testServices: TestServices,
) : WasmBoxRunner(testServices, executeWithV8Only = true)
