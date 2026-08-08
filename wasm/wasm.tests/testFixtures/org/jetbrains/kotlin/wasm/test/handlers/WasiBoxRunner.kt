/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.grouping.hasGroupedTestsDriver
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
 * [callGroupedTestsDriver] selects what to call, and has to be derived from whether the stage-2 facade actually
 * generated the driver rather than from probing the exports. A grouped batch exports `startTest()` — the
 * result-collecting driver, whose per-test results are attributed on the JVM side (see `GroupedTestsResultProtocol`),
 * so a test failure is reported through stdout instead of by throwing — but the `wasiBoxTestRun.kt` additional file
 * that every test with a `box()` gets exports a `startTest()` of its own, which merely runs `box()`. Probing for the
 * name would therefore run `box()` in place of the compiler-generated `startUnitTests()` for an isolated
 * `// RUN_UNIT_TESTS` test that also has a `box()`, silently skipping the unit tests such a test exists to run.
 *
 * Only Node.js runs this script, and hence only it can make that choice: the standalone WASI VMs (WasmEdge/Wasmtime)
 * invoke the `startTest` export directly (see [WasmVM.WasmEdge]/[WasmVM.Wasmtime]) and therefore both require it and
 * always run whichever `startTest` the binary exports. It is always exported — by the driver for a grouped batch, by
 * `wasiBoxTestRun.kt` for a box run.
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
 * Asserts that a driver-linked batch's binary exports the driver's `startTest` and nothing of the per-test
 * `wasiBoxTestRun.kt` helpers, by inspecting the export list of the generated JS glue.
 *
 * The bare `startTest` name is all the standalone VMs (WasmEdge/Wasmtime) can invoke, and it is unambiguous only
 * because the helpers — each exporting a `startTest`/`runBoxTest` of its own — never enter a grouped link: they live
 * in `-libraries` KLIBs, whose declarations are deserialized only when referenced, and nothing references them there
 * (the launcher reaches each `box()` by its FQN). This check turns that linker-level invariant into a loud failure:
 * should `runBoxTest` ever appear in a grouped binary's export surface — the helper got referenced, or the linker
 * started exporting library `@WasmExport`s — a bare `startTest` may no longer resolve to the driver, and the batch
 * would be executed through a single `box()` instead of the result-collecting driver.
 *
 * See `WasmWasiGroupedTestsExportedEntryPointGenerator` for the full account of why the shared name is safe.
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
        /** Whether this batch carries the generated result-collecting driver; see [startUnitTestsWasiScript]. */
        callGroupedTestsDriver: Boolean = false,
    ): List<Throwable> {
        val outputDirBase = testServices.getWasmTestOutputDirectory()

        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val startUnitTests = useUnitTestRunnerOnly || RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives

        // A driverless unit-test run has to report through `startUnitTests()`, and only the Node launcher can call it:
        // the standalone WASI VMs invoke a bare export name, which for a test with a `box()` resolves to
        // `wasiBoxTestRun.kt`'s `startTest` — that runs `box()` and says nothing about the unit tests, so the run would
        // pass on the box result while the unit tests never executed. There is no test data in this shape today; fail
        // loudly rather than silently the first time there is. Grouped batches are unaffected: their `startTest` is the
        // driver itself.
        if (useUnitTestRunnerOnly && !callGroupedTestsDriver && RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives) {
            val standaloneVms = vmsToCheck.filter { !it.entryPointIsJsFile }
            if (standaloneVms.isNotEmpty()) {
                testInfraError(
                    "A `// RUN_UNIT_TESTS` WASI test cannot report its results on ${standaloneVms.map { it.vmName }}: " +
                            "those VMs invoke the bare `startTest` export, which is `wasiBoxTestRun.kt`'s `box()` " +
                            "helper rather than the unit-test runner. Run such a test on Node.js only, or export a " +
                            "unit-test entry point for the standalone VMs to invoke."
                )
            }
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
        val folder = (artifact as WasmFolderBinaryArtifact).folder
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

        // Same trap as in [WasiBoxRunner.runWasmCode], mirrored here since this runner drives the same standalone VMs:
        // without a driver, only the Node launcher can reach `startUnitTests()` — WasmEdge/Wasmtime invoke the bare
        // `startTest` export, which for a test with a `box()` is `wasiBoxTestRun.kt`'s helper, so the unit tests such a
        // test exists for would silently not run there.
        if (!testServices.hasGroupedTestsDriver &&
            RUN_UNIT_TESTS in firstNonGroupingTestServices.moduleStructure.allDirectives
        ) {
            val standaloneVms = vmsToCheck.filter { !it.entryPointIsJsFile }
            if (standaloneVms.isNotEmpty()) {
                testInfraError(
                    "A `// RUN_UNIT_TESTS` WASI test cannot report its results on ${standaloneVms.map { it.vmName }}: " +
                            "those VMs invoke the bare `startTest` export, which is `wasiBoxTestRun.kt`'s `box()` " +
                            "helper rather than the unit-test runner. Run such a test on Node.js only, or export a " +
                            "unit-test entry point for the standalone VMs to invoke."
                )
            }
        }

        val callGroupedTestsDriver = testServices.hasGroupedTestsDriver
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
