/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.WasmCompilationSet
import org.jetbrains.kotlin.test.model.WasmCompilationSetsBinaryArtifact
import org.jetbrains.kotlin.test.model.WasmFolderBinaryArtifact
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator.Companion.WASM_BASE_FILE_NAME
import org.jetbrains.kotlin.test.services.configuration.useNewExceptionHandling
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import org.jetbrains.kotlin.wasm.test.tools.WASI_BOX_ENTRY_EXPORT
import org.jetbrains.kotlin.wasm.test.tools.WASI_UNIT_TESTS_ENTRY_EXPORT
import java.io.File

/**
 * The `test.mjs` launcher script for running WASI tests under Node.js, exiting with code 1 on any uncaught
 * exception (e.g. a hard VM trap). WasmEdge and Wasmtime bypass this script and invoke one export of the artifact
 * directly, chosen by [wasiStandaloneEntryExport] (see [WasmVM.WasmEdge] and [WasmVM.Wasmtime]).
 *
 * [callGroupedTestsDriver] must come from the artifact the stage-2 facade produced, not from probing the exports:
 * `wasiBoxTestRun.kt` exports a `startTest()` of its own that merely runs `box()`, so a probe would run `box()` in
 * place of `startUnitTests()` for an isolated `// RUN_UNIT_TESTS` test that also has a `box()`.
 */
internal fun startUnitTestsWasiScript(callGroupedTestsDriver: Boolean): String = """
    try {
        let jsModule = await import('./$WASM_BASE_FILE_NAME.mjs');
        ${if (callGroupedTestsDriver) "jsModule.startTest();" else "jsModule.startUnitTests();"}
    } catch(e) {
        console.log('Failed with exception!');
        console.log(e);
        process.exit(1);
    }
    """.trimIndent()

/**
 * The export the standalone WASI VMs invoke. In a driver-linked batch, `startTest` is the result-collecting driver.
 * Otherwise a `// RUN_UNIT_TESTS` test is isolated, and its `startTest` export is `wasiBoxTestRun.kt`'s glue that runs
 * `box()` alone. Such a test runs `startUnitTests` instead: the compiler exports it from every binary with
 * `@kotlin.test.Test` functions, and the `@Test` launcher of `WasmJsLauncherAdditionalSourceProvider` calls `box()`
 * inside the suite, so that one export covers the unit tests and the box verdict alike.
 */
internal fun wasiStandaloneEntryExport(hasGroupedTestsDriver: Boolean, runUnitTests: Boolean): String = when {
    hasGroupedTestsDriver -> WASI_BOX_ENTRY_EXPORT
    runUnitTests -> WASI_UNIT_TESTS_ENTRY_EXPORT
    else -> WASI_BOX_ENTRY_EXPORT
}

// TODO reduce amount of duplicated code between this class and WasmBoxRunner
class WasiBoxRunner(
    testServices: TestServices,
    executeWithNodeJsOnly: Boolean = false, // Klib backward compatibility testsuite needs only one best Wasi runner
) : AbstractWasmArtifactsCollector(testServices) {
    internal val vmsToCheck: List<WasmVM> = if (executeWithNodeJsOnly) {
        listOf(WasmVM.NodeJs)
    } else {
        listOf(WasmVM.NodeJs, WasmVM.WasmEdge, WasmVM.Wasmtime)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmCode(modulesToArtifact.values.single() as WasmCompilationSetsBinaryArtifact)
        }
    }

    /**
     * Runs the WASI compilation set on the WASI VMs (NodeJs / WasmEdge / Wasmtime).
     *
     * Mirrors [WasmBoxRunner.runWasmCode] so that the IN_PROCESS grouping stage
     * ([WasmCompilationSetsGroupingStageBoxRunner]) can delegate WASI runs here instead of to the
     * JS-only [WasmBoxRunner] (whose V8/SpiderMonkey/JSC engines cannot resolve the WASI `wasi`
     * import emitted into `index.mjs`).
     */
    fun runWasmCode(
        artifacts: WasmCompilationSetsBinaryArtifact,
        useUnitTestRunnerOnly: Boolean = false,
        outputCollector: MutableList<WasmVMOutput>? = null,
        throwOnExceptions: Boolean = !useUnitTestRunnerOnly,
    ): List<Throwable> {
        val outputDirBase = testServices.getWasmTestOutputDirectory()

        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val runUnitTestsDirective = RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives
        val startUnitTests = useUnitTestRunnerOnly || runUnitTestsDirective
        val callGroupedTestsDriver = artifacts.hasGroupedTestsDriver
        val standaloneEntryExport = wasiStandaloneEntryExport(callGroupedTestsDriver, runUnitTests = startUnitTests)

        val testWasiQuiet = if (useUnitTestRunnerOnly) startUnitTestsWasiScript(callGroupedTestsDriver)
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

        fun writeToFilesAndRunTest(mode: String, set: WasmCompilationSet): List<Throwable> {
            val dir = File(outputDirBase, mode)
            dir.mkdirs()

            set.compilerResult.writeTo(dir, WASM_BASE_FILE_NAME, debugMode)

            File(dir, "test.mjs").writeText(testWasi)
            val collectedJsArtifacts = collectJsArtifacts(originalFile, mode)
            val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(dir)
            if (debugMode >= DebugMode.DEBUG) {
                println(" ------ $mode Test file://${dir.absolutePath}/test.mjs")
            }

            val useNewExceptionProposal = testServices.useNewExceptionHandling(WasmTarget.WASI)

            val exceptions = vmsToCheck.mapNotNull { vm ->
                vm.runWithCaughtExceptions(
                    debugMode = debugMode,
                    useNewExceptionHandling = useNewExceptionProposal,
                    useStackSwitching = false,
                    entryFile = if (!vm.entryPointIsJsFile) "$WASM_BASE_FILE_NAME.wasm" else collectedJsArtifacts.entryPath ?: "test.mjs",
                    jsFilePaths = jsFilePaths,
                    workingDirectory = dir,
                    executionName = formatWasmExecutionName(vm.vmName, mode),
                    outputCollector = outputCollector,
                    wasiEntryExport = standaloneEntryExport,
                    expectUnitTestReport = runUnitTestsDirective && !callGroupedTestsDriver && !vm.entryPointIsJsFile,
                )
            }

            // TODO KT-71504: support ignoring utility files for WASI target size tests
            // The `WASM_*_EXPECTED_OUTPUT_SIZE` expectations of a test describe its wasm-js artifacts;
            // WASI has no expectations of its own yet, so its sizes are not checked.
            // TODO: KT-71504: support size tests for the WASI target.
            return exceptions
        }

        val allExceptions = wasmCompilationModes(
            compilation = artifacts.compilation,
            dceCompilation = artifacts.dceCompilation,
            optimisedCompilation = artifacts.optimisedCompilation,
        ).flatMap { (directoryName, compilation) ->
            writeToFilesAndRunTest(directoryName, compilation)
        }

        if (throwOnExceptions) {
            processExceptions(allExceptions)
        }

        return allExceptions
    }
}

/*
 * This Wasi folder runner is intended for the future use in "WasmWasi Klib forward compatibility tests", should it be ever needed,
 * similar to [CustomWasmJsCompilerSecondStageTestGenerated]
 */
open class WasmWasiFolderGroupingStageBoxRunner(
    testServices: TestServices
) : AbstractWasmGroupingStageBoxRunner(testServices), WasmArtifactsCollector {
    private val firstNonGroupingTestServices: TestServices
        get() = testServices.groupingStageInputs.first().testServices
    private val vmsToCheck: List<WasmVM> = listOf(WasmVM.NodeJs, WasmVM.WasmEdge, WasmVM.Wasmtime)

    override fun shouldUseBoxExportModeWhenDriverless(): Boolean {
        // WASI tests always use the unit-test runner, never box export mode
        return false
    }

    override fun runTestCode(
        artifact: BinaryArtifacts.Wasm,
        useUnitTestRunnerOnly: Boolean,
        outputCollector: MutableList<WasmVMOutput>?,
    ): List<Throwable> {
        val folderArtifact = artifact as WasmFolderBinaryArtifact
        val folder = folderArtifact.folder
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val callGroupedTestsDriver = folderArtifact.hasGroupedTestsDriver
        val runUnitTestsDirective = RUN_UNIT_TESTS in firstNonGroupingTestServices.moduleStructure.allDirectives
        val standaloneEntryExport = wasiStandaloneEntryExport(
            callGroupedTestsDriver,
            runUnitTests = useUnitTestRunnerOnly || runUnitTestsDirective,
        )

        val testWasi = startUnitTestsWasiScript(callGroupedTestsDriver)
        File(folder, "test.mjs").writeText(testWasi)

        val collectedOutputs = outputCollector ?: mutableListOf()
        return vmsToCheck.mapNotNull { vm ->
            vm.runWithCaughtExceptions(
                debugMode = debugMode,
                firstNonGroupingTestServices.useNewExceptionHandling(WasmTarget.WASI),
                useStackSwitching = false,
                entryFile = if (!vm.entryPointIsJsFile) "$WASM_BASE_FILE_NAME.wasm" else "test.mjs",
                jsFilePaths = emptyList(),
                workingDirectory = folder,
                executionName = formatWasmExecutionName(vm.vmName, "dev"),
                outputCollector = collectedOutputs,
                wasiEntryExport = standaloneEntryExport,
                expectUnitTestReport = runUnitTestsDirective && !callGroupedTestsDriver && !vm.entryPointIsJsFile,
            )
        }
    }
}
