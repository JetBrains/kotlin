/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_OLD_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.WasmCompilationSetsBinaryArtifact
import org.jetbrains.kotlin.test.model.WasmFolderBinaryArtifact
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator.Companion.WASM_BASE_FILE_NAME
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File

// TODO reduce amount of duplicated code between this class and WasmBoxRunner
class WasiBoxRunner(
    testServices: TestServices
) : AbstractWasmArtifactsCollector(testServices) {
    internal val vmsToCheck: List<WasmVM> = listOf(WasmVM.NodeJs, WasmVM.WasmEdge, WasmVM.Wasmtime)

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmCode(modulesToArtifact.values.single() as WasmCompilationSetsBinaryArtifact)
        }
    }

    /**
     * Runs the WASI compilation set on the WASI VMs (NodeJs / WasmEdge / Wasmtime).
     *
     * Mirrors [WasmBoxRunner.runWasmCode] so that the IN_PROCESS grouping stage
     * ([WasmCompilationSetsBoxRunnerGroupingStage]) can delegate WASI runs here instead of to the
     * JS-only [WasmBoxRunner] (whose V8/SpiderMonkey/JSC engines cannot resolve the WASI `wasi`
     * import emitted into `index.mjs`).
     */
    fun runWasmCode(
        artifacts: WasmCompilationSetsBinaryArtifact,
        useUnitTestRunnerOnly: Boolean = false,
        outputCollector: MutableList<String>? = null,
        throwOnExceptions: Boolean = !useUnitTestRunnerOnly,
    ): List<Throwable> {
        val outputDirBase = testServices.getWasmTestOutputDirectory()

        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val startUnitTests = useUnitTestRunnerOnly || RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives

        val testWasiQuiet = if (useUnitTestRunnerOnly) """
            try {
                let jsModule = await import('./$WASM_BASE_FILE_NAME.mjs');
                jsModule.startUnitTests();
            } catch(e) {
                console.log('Failed with exception!');
                console.log(e);
                process.exit(1);
            }
            """.trimIndent()
        else """
            let boxTestPassed = false;
            try {
                let jsModule = await import('./$WASM_BASE_FILE_NAME.mjs');
                ${if (startUnitTests) "jsModule.startUnitTests();" else ""}
                boxTestPassed = jsModule.runBoxTest();
            } catch(e) {
                console.log('Failed with exception!');
                console.log(e);
            }

            if (!boxTestPassed)
                process.exit(1);
            """.trimIndent()

        val testWasiVerbose = testWasiQuiet + """
            
            
                    console.log('test passed');
                """.trimIndent()

        val testWasi = if (debugMode >= DebugMode.DEBUG) testWasiVerbose else testWasiQuiet

        fun writeToFilesAndRunTest(mode: String, res: WasmCompilerResult): List<Throwable> {
            val dir = File(outputDirBase, mode)
            dir.mkdirs()

            res.writeTo(dir, WASM_BASE_FILE_NAME, debugMode)

            File(dir, "test.mjs").writeText(testWasi)
            val collectedJsArtifacts = collectJsArtifacts(originalFile, mode)
            val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(dir)
            if (debugMode >= DebugMode.DEBUG) {
                println(" ------ $mode Test file://${dir.absolutePath}/test.mjs")
            }

            val testFileText = originalFile.readText()

            // Determine the exception-handling proposal directly from the test directives instead of
            // building a second-stage compiler configuration. The latter is not possible when this
            // runner is driven by the grouping stage (`modulesToArtifact` is empty), because creating
            // a SECOND-stage configuration eagerly requires a registered `KLib` artifact for the
            // queried module — absent for the `common`/metadata module of HMPP tests. This mirrors the
            // logic in `WasmSecondStageEnvironmentConfigurator` (defaulting to `true` for the WASI
            // target).
            val directives = testServices.moduleStructure.allDirectives
            val useNewExceptionProposal = when {
                USE_NEW_EXCEPTION_HANDLING_PROPOSAL in directives -> true
                USE_OLD_EXCEPTION_HANDLING_PROPOSAL in directives -> false
                else -> true
            }

            val exceptions = vmsToCheck.mapNotNull { vm ->
                vm.runWithCaughtExceptions(
                    debugMode = debugMode,
                    useNewExceptionHandling = useNewExceptionProposal,
                    entryFile = if (!vm.entryPointIsJsFile) "$WASM_BASE_FILE_NAME.wasm" else collectedJsArtifacts.entryPath ?: "test.mjs",
                    jsFilePaths = jsFilePaths,
                    workingDirectory = dir,
                    outputCollector = outputCollector,
                )
            }

            // TODO KT-71504: support size tests for WASI target and ignoring utility files
            val filesToIgnoreInSizeChecks = emptySet<File>()
            when (mode) {
                "dce" -> checkExpectedDceOutputSize(debugMode, testFileText, dir, filesToIgnoreInSizeChecks)
                "optimized" -> checkExpectedOptimizedOutputSize(debugMode, testFileText, dir, filesToIgnoreInSizeChecks)
            }
            return exceptions
        }

        val allExceptions = mutableListOf<Throwable>()
        allExceptions += writeToFilesAndRunTest("dev", artifacts.compilation.compilerResult)
        artifacts.dceCompilation?.let {
            allExceptions += writeToFilesAndRunTest("dce", it.compilerResult)
        }
        artifacts.optimisedCompilation?.let {
            allExceptions += writeToFilesAndRunTest("optimized", it.compilerResult)
        }

        if (throwOnExceptions) {
            processExceptions(allExceptions)
        }

        return allExceptions
    }
}

class WasmWasiFolderBoxRunnerGroupingStage(
    testServices: TestServices
) : AbstractWasmBoxRunnerGroupingStage(testServices), WasmArtifactsCollector {
    private val firstNonGroupingTestServices: TestServices
        get() = testServices.groupingStageInputs.first().testServices
    private val vmsToCheck: List<WasmVM> = listOf(WasmVM.NodeJs, WasmVM.WasmEdge, WasmVM.Wasmtime)

    override fun processArtifact(artifact: BinaryArtifacts.Wasm) {
        val folder = (artifact as WasmFolderBinaryArtifact).folder
        val runResult = runOnFolder(folder)
        handleRunResult(runResult)
    }

    private fun runOnFolder(folder: File): RunResult {
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

        val testWasi = """
            try {
                let jsModule = await import('./$WASM_BASE_FILE_NAME.mjs');
                jsModule.startUnitTests();
            } catch(e) {
                console.log('Failed with exception!');
                console.log(e);
                exit(1);
            }
        """.trimIndent()

        File(folder, "test.mjs").writeText(testWasi)

        val allDirectives = firstNonGroupingTestServices.moduleStructure.allDirectives
        val useNewExceptionHandling = USE_NEW_EXCEPTION_HANDLING_PROPOSAL in allDirectives &&
                USE_OLD_EXCEPTION_HANDLING_PROPOSAL !in allDirectives

        val collectedOutputs = mutableListOf<String>()
        val exceptions = vmsToCheck.mapNotNull { vm ->
            vm.runWithCaughtExceptions(
                debugMode = debugMode,
                useNewExceptionHandling,
                entryFile = if (!vm.entryPointIsJsFile) "$WASM_BASE_FILE_NAME.wasm" else "test.mjs",
                jsFilePaths = emptyList(),
                workingDirectory = folder,
                outputCollector = collectedOutputs,
            )
        }
        processExceptions(exceptions)

        return RunResult(collectedOutputs, exceptions)
    }
}
