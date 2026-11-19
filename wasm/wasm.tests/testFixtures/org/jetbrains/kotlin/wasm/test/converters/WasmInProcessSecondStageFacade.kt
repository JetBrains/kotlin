/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.model.WasmCompilationSetsBinaryArtifact
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.services.targetPlatform
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.wasm.config.wasmTarget
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.js.config.friendLibraries
import org.jetbrains.kotlin.js.config.includes
import org.jetbrains.kotlin.js.config.libraries
import org.jetbrains.kotlin.platform.wasm.WasmPlatformWithTarget
import org.jetbrains.kotlin.test.isSingleTestBatch
import org.jetbrains.kotlin.test.klib.getTransitivesAndFriendsWithoutReflectionPackageNameHelper
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlin.wasm.test.blackbox.AbstractWasmSecondStageGroupingFacade
import org.jetbrains.kotlin.wasm.test.blackbox.DependencyPaths
import org.jetbrains.kotlin.wasm.test.handlers.getWasmTestOutputDirectory
import java.io.File

/**
 * An in-process grouping stage facade that invokes [WasmBackendFacade] (deserialize + lower + compile)
 * for each test in the batch, producing a [org.jetbrains.kotlin.test.model.WasmCompilationSetsBinaryArtifact].
 *
 * This facade is the in process counterpart to [org.jetbrains.kotlin.wasm.test.blackbox.CustomWasmSecondStageFacade.Grouping] (which uses CLI).
 *
 * @see org.jetbrains.kotlin.wasm.test.blackbox.CustomWasmSecondStageFacade.Grouping for the CLI-based counterpart
 * @see WasmBackendFacade for the underlying in-process compilation pipeline
 */
class WasmInProcessSecondStageFacade {
    class Grouping(
        testServices: TestServices,
    ) : AbstractWasmSecondStageGroupingFacade(testServices) {
        override fun transform(inputArtifact: GroupingStageInputArtifact): BinaryArtifacts.Wasm? {
            val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("combined-sources")
            val context = buildBatchExecutionContext(inputArtifact, CompilationStage.SECOND)

            // Dispatch on `isSingleTestBatch`, not `isIsolatedBatch`:
            //   a single-test batch must always be compiled as a standalone box-export test, since
            //   GenerateWasmTests.lower() never emits a grouped/unit-test-runner JUnit case for a single test,
            //   no matter it either was isolated or merely got a unique batch token.
            // `CustomWasmSecondStageFacade.Grouping` must use `isIsolatedBatch()`,
            //    since there `box()` is `@JsExport`-gated at first-stage compile time, before batch size is known.
            // But here, the in-process pipeline decides `box` export at the actual second-stage compilation (`wasmTestBoxFunctionToExport`),
            // so `isSingleTestBatch` is the correct, more precise criterion here.
            return if (inputArtifact.nonGroupingStageOutputs.isSingleTestBatch()) {
                doIsolated(context)
            } else {
                groupedBatch(inputArtifact, context, tempDir)
            }
        }

        private fun groupedBatch(
            inputArtifact: GroupingStageInputArtifact,
            context: BatchExecutionContext,
            tempDir: File,
        ): BinaryArtifacts.Wasm {
            val services = inputArtifact.nonGroupingStageOutputs.first().testServices
            val result = compileGroupedBatchLauncher(services, context, tempDir) { launcherModule, launcherKlib ->
                WasmBackendFacade(services).transform(launcherModule, launcherKlib)
            }

            val outputDir = services.getWasmTestOutputDirectory()
            outputDir.mkdirs()
            copyJsFilesToOutputDir(context.filteredOutputs.map { it.testModule }, outputDir)

            val artifact = result as? WasmCompilationSetsBinaryArtifact
                ?: testInfraError("WasmInProcessSecondStageFacade: groupedBatch produced no Wasm compilation sets, but $result")
            return artifact.withGroupedTestsDriver()
        }

        private fun doIsolated(context: BatchExecutionContext): BinaryArtifacts.Wasm {
            val filteredOutputs = context.filteredOutputs
            val services = filteredOutputs.first().testServices
            val testModules = filteredOutputs.map { it.testModule }
            val mainModule = testModules.mainModuleOfTest()

            val deps = mainModule.collectDependencies(services, CompilationStage.SECOND)
            val perTestKlibPathsIsolated = filteredOutputs.map { it.klib.outputFile.absolutePath }.reversed()

            val includedLibrary = perTestKlibPathsIsolated.first()
            val libraries = perTestKlibPathsIsolated.drop(1)
            val configuration = services.compilerConfigurationProvider.getCompilerConfiguration(mainModule, CompilationStage.SECOND)
            configuration.includes = includedLibrary
            configuration.friendLibraries = deps.friend.toList()
            val runtimeKlibs = WasmEnvironmentConfigurator.getRuntimePathsForModule(configuration.wasmTarget, services)
            configuration.libraries = runtimeKlibs + deps.regular.toList() + deps.friend.toList() + libraries + listOf(includedLibrary)
            val outputDir = services.getWasmTestOutputDirectory()
            outputDir.mkdirs()
            copyJsFilesToOutputDir(testModules, outputDir)
            val klibArtifact = BinaryArtifacts.KLib(File(includedLibrary), configuration.diagnosticsCollector)
            return WasmBackendFacade(services).transform(mainModule, klibArtifact)
                ?: testInfraError("WasmInProcessSecondStageFacade.doIsolatedInProcess() produced no Wasm artifact")
        }

        override fun TestModule.collectDependencies(
            testServices: TestServices,
            compilationStage: CompilationStage,
        ): DependencyPaths = collectInProcessDependencies(testServices)
    }
}

/**
 * The dependencies of a module linked in process: the in-process facades use the same compiler and stdlib for both stages,
 * so the compilation stage does not affect them.
 */
internal fun TestModule.collectInProcessDependencies(testServices: TestServices): DependencyPaths {
    val [transitiveLibraries: List<File>, friendLibraries: List<File>] =
        getTransitivesAndFriendsWithoutReflectionPackageNameHelper(testServices)

    val regularDependencies: Set<String> = buildSet {
        val wasmTarget = (targetPlatform(testServices).single() as WasmPlatformWithTarget).target
        add(WasmEnvironmentConfigurator.stdlibPath(wasmTarget, testServices))
        add(WasmEnvironmentConfigurator.kotlinTestPath(wasmTarget, testServices))
        transitiveLibraries.mapTo(this) { it.absolutePath }
    }

    val friendDependencies: Set<String> = friendLibraries.mapToSetOrEmpty { it.absolutePath }

    return DependencyPaths(regularDependencies, friendDependencies)
}
