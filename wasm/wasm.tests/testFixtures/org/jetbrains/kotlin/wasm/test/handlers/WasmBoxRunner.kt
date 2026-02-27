/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
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

class WasmBoxRunner(
    testServices: TestServices,
    executeWithV8Only: Boolean = false,
) : WasmBoxRunnerBase(testServices, executeWithV8Only) {

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmCode()
        }
    }

    private fun runWasmCode() {
        val artifacts = modulesToArtifact.values.single() as BinaryArtifacts.Wasm.CompilationSets
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
        val testFileText = originalFile.readText()
        val failsIn = InTextDirectivesUtils.findListWithPrefixes(testFileText, "// WASM_FAILS_IN: ")

        fun writeToFilesAndRunTest(mode: String, result: BinaryArtifacts.WasmCompilationSet): List<Throwable> {
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
                failsIn = failsIn,
                filesToIgnoreInSizeChecks = filesToIgnoreInSizeChecks
            )

            return exceptions + when (mode) {
                "dce" -> checkExpectedDceOutputSize(debugMode, testFileText, outputDir, filesToIgnoreInSizeChecks)
                "optimized" -> checkExpectedOptimizedOutputSize(debugMode, testFileText, outputDir, filesToIgnoreInSizeChecks)
                "dev" -> emptyList() // no additional checks required
                else -> error("Unknown mode: $mode")
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

        processExceptions(allExceptions)
    }
}
