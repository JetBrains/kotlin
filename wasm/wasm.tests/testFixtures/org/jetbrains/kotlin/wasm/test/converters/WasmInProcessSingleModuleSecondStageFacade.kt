/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.isSingleTestBatch
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.model.WasmCompilationSet
import org.jetbrains.kotlin.test.model.WasmCompilationSetsBinaryArtifact
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.wasm.test.blackbox.AbstractWasmSecondStageGroupingFacade
import org.jetbrains.kotlin.wasm.test.blackbox.DependencyPaths
import org.jetbrains.kotlin.wasm.test.blackbox.PerTestOutput
import org.jetbrains.kotlin.wasm.test.handlers.DEV_MODE_DIRECTORY_NAME
import org.jetbrains.kotlin.wasm.test.handlers.getWasmTestOutputDirectoryForMode
import java.io.File

/**
 * The grouping stage facade of the single-module mode (`WASM_INCLUDED_MODULE_ONLY`): it compiles every module of every test
 * of a batch alone, as the non-grouped runs do, and then the launcher alone against all of them. The runner writes them into
 * one directory, where they import each other by their unique output names (see `WasmFirstStageEnvironmentConfigurator`),
 * and the precompiled stdlib and kotlin-test through the dependency resolution map.
 */
class WasmInProcessSingleModuleSecondStageFacade {
    class Grouping(testServices: TestServices) : AbstractWasmSecondStageGroupingFacade(testServices) {
        override fun transform(inputArtifact: GroupingStageInputArtifact): BinaryArtifacts.Wasm {
            val context = buildBatchExecutionContext(inputArtifact, CompilationStage.SECOND)
            val services = inputArtifact.nonGroupingStageOutputs.first().testServices
            // The resolution maps are relative to the directory which `WasmBoxRunner` writes the whole batch into.
            val artifactsDir = services.getWasmTestOutputDirectoryForMode(DEV_MODE_DIRECTORY_NAME)

            // The helpers KLIBs of a batch are identical, so only the first one is compiled, see `deduplicateHelperKlibPaths`.
            val compiledKlibPaths = context.perTestKlibPaths.toSet()
            val perTestOutputs = context.filteredOutputs.filter { it.klib.outputFile.absolutePath in compiledKlibPaths }
            val perTestSets = perTestOutputs.map { it.testModule to compileAlone(it, artifactsDir) }

            // As in `WasmInProcessSecondStageFacade`, a single-test batch is run through the exported `box()` of its main module.
            if (inputArtifact.nonGroupingStageOutputs.isSingleTestBatch()) {
                val mainModule = perTestOutputs.map { it.testModule }.mainModuleOfTest()
                val [mainSets, dependencySets] = perTestSets.partition { [module, _] -> module == mainModule }
                return WasmCompilationSetsBinaryArtifact(
                    WasmCompilationSet(mainSets.single().second.compilerResult, dependencySets.map { it.second })
                )
            }

            val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("combined-sources")
            val launcherArtifact = compileGroupedBatchLauncher(services, context, tempDir) { launcherModule, launcherKlib ->
                WasmBackendSingleModuleFacade(services, artifactsDir).transform(launcherModule, launcherKlib)
            } ?: testInfraError("The launcher of the grouped batch produced no Wasm artifact")
            val launcherResult = (launcherArtifact as WasmCompilationSetsBinaryArtifact).compilation.compilerResult
            return WasmCompilationSetsBinaryArtifact(
                WasmCompilationSet(launcherResult, perTestSets.map { it.second })
            ).withGroupedTestsDriver()
        }

        /** Compiles one module of a test alone, with the test's second-stage configuration of that module. */
        private fun compileAlone(output: PerTestOutput, artifactsDir: File): WasmCompilationSet {
            val [services, module, klib] = output
            val artifact = WasmBackendSingleModuleFacade(services, artifactsDir).transform(module, klib)
                ?: testInfraError("Module `${module.name}` of test ${services.testInfo} produced no Wasm artifact")
            return (artifact as WasmCompilationSetsBinaryArtifact).compilation
        }

        override fun TestModule.collectDependencies(
            testServices: TestServices,
            compilationStage: CompilationStage,
        ): DependencyPaths = collectInProcessDependencies(testServices)
    }
}
