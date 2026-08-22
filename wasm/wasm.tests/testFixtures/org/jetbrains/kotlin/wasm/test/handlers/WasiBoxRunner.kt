/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.WasmCompilationSetsBinaryArtifact
import org.jetbrains.kotlin.test.model.WasmFolderBinaryArtifact
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator.Companion.WASM_BASE_FILE_NAME
import org.jetbrains.kotlin.test.services.configuration.useNewExceptionHandling
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File

/**
 * The `test.mjs` launcher script for running WASI tests under Node.js, exiting with code 1 on any uncaught
 * exception (e.g. a hard VM trap).
 *
 * [callGroupedTestsDriver] must come from the artifact the stage-2 facade produced, not from probing the exports:
 * `wasiBoxTestRun.kt` exports a `startTest()` of its own that merely runs `box()`, so a probe would run `box()` in
 * place of `startUnitTests()` for an isolated `// RUN_UNIT_TESTS` test that also has a `box()`.
 */
fun startUnitTestsWasiScript(callGroupedTestsDriver: Boolean): String = """
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
 * Turns the linker-level invariant the bare `startTest` export relies on into a loud failure: should a
 * `wasiBoxTestRun.kt` helper ever reach a grouped binary, `startTest` may no longer resolve to the driver and the
 * batch would run a single `box()` instead. See `WasmWasiGroupedTestsExportedEntryPointGenerator`.
 */
internal fun assertDriverOwnsStartTestExport(dir: File) {
    val glue = dir.resolve("$WASM_BASE_FILE_NAME.mjs").takeIf(File::exists) ?: return
    val exportedNames = glue.readText()
    if (Regex("\\brunBoxTest\\b") in exportedNames) {
        testInfraError(
            "The linked binary of a driver-linked grouped batch exports `runBoxTest` from a per-test " +
                    "`wasiBoxTestRun.kt` helper (${glue.absolutePath}). Helper exports are expected to never reach a " +
                    "grouped link, and the standalone WASI VMs invoke the bare `startTest` export — which is now " +
                    "ambiguous between the driver and the helper, so the batch may run a single `box()` instead of " +
                    "the result-collecting driver."
        )
    }
    if (Regex("\\bstartTest\\b") !in exportedNames) {
        testInfraError(
            "The linked binary of a driver-linked grouped batch does not export `startTest` " +
                    "(${glue.absolutePath}), so no WASI VM can invoke the result-collecting driver."
        )
    }
}

/**
 * Rejects unit-test runs that would invoke the per-test `box()` helper on standalone WASI VMs instead of the unit-test runner,
 * because the artifact has no grouped-tests driver to provide the correct entry point.
 */
private fun checkUnitTestRunnerSupport(
    hasGroupedTestsDriver: Boolean,
    hasRunUnitTestsDirective: Boolean,
    vmsToCheck: List<WasmVM>,
) {
    if (hasGroupedTestsDriver || !hasRunUnitTestsDirective) return

    val standaloneVms = vmsToCheck.filter { !it.entryPointIsJsFile }
    if (standaloneVms.isEmpty()) return

    testInfraError(
        "A `// RUN_UNIT_TESTS` WASI test cannot report its results on ${standaloneVms.map { it.vmName }}: " +
                "those VMs invoke the bare `startTest` export, which is `wasiBoxTestRun.kt`'s `box()` " +
                "helper rather than the unit-test runner. Run such a test on Node.js only, or export a " +
                "unit-test entry point for the standalone VMs to invoke."
    )
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
        outputCollector: MutableList<String>? = null,
        throwOnExceptions: Boolean = !useUnitTestRunnerOnly,
    ): List<Throwable> {
        val outputDirBase = testServices.getWasmTestOutputDirectory()

        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val startUnitTests = useUnitTestRunnerOnly || RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives
        val callGroupedTestsDriver = artifacts.hasGroupedTestsDriver

        if (useUnitTestRunnerOnly) {
            checkUnitTestRunnerSupport(
                hasGroupedTestsDriver = callGroupedTestsDriver,
                hasRunUnitTestsDirective = RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives,
                vmsToCheck = vmsToCheck,
            )
        }

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

        fun writeToFilesAndRunTest(mode: String, res: WasmCompilerResult): List<Throwable> {
            val dir = File(outputDirBase, mode)
            dir.mkdirs()

            res.writeTo(dir, WASM_BASE_FILE_NAME, debugMode)

            if (callGroupedTestsDriver) assertDriverOwnsStartTestExport(dir)

            File(dir, "test.mjs").writeText(testWasi)
            val collectedJsArtifacts = collectJsArtifacts(originalFile, mode)
            val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(dir)
            if (debugMode >= DebugMode.DEBUG) {
                println(" ------ $mode Test file://${dir.absolutePath}/test.mjs")
            }

            val testFileText = originalFile.readText()
            val useNewExceptionProposal = testServices.useNewExceptionHandling(WasmTarget.WASI)

            val exceptions = vmsToCheck.mapNotNull { vm ->
                vm.runWithCaughtExceptions(
                    debugMode = debugMode,
                    useNewExceptionHandling = useNewExceptionProposal,
                    useStackSwitching = false,
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

    override fun shouldUseBoxExportMode(): Boolean {
        // WASI tests always use the unit-test runner, never box export mode
        return false
    }

    override fun runTestCode(
        artifact: BinaryArtifacts.Wasm,
        useUnitTestRunnerOnly: Boolean,
        outputCollector: MutableList<String>?,
    ): List<Throwable> {
        val folderArtifact = artifact as WasmFolderBinaryArtifact
        val folder = folderArtifact.folder
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

        checkUnitTestRunnerSupport(
            hasGroupedTestsDriver = folderArtifact.hasGroupedTestsDriver,
            hasRunUnitTestsDirective = RUN_UNIT_TESTS in firstNonGroupingTestServices.moduleStructure.allDirectives,
            vmsToCheck = vmsToCheck,
        )

        val callGroupedTestsDriver = folderArtifact.hasGroupedTestsDriver
        if (callGroupedTestsDriver) assertDriverOwnsStartTestExport(folder)

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
                outputCollector = collectedOutputs,
            )
        }
    }
}
