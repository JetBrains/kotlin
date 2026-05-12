/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.services.targetPlatform
import org.jetbrains.kotlin.test.diagnostics.DiagnosticsCollectorStub
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import org.jetbrains.kotlin.wasm.config.wasmTarget
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.js.config.friendLibraries
import org.jetbrains.kotlin.js.config.includes
import org.jetbrains.kotlin.js.config.libraries
import org.jetbrains.kotlin.platform.wasm.WasmPlatformWithTarget
import org.jetbrains.kotlin.test.frontend.fir.getTransitivesAndFriends
import org.jetbrains.kotlin.test.isSingleTestBatch
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
            val someModule = inputArtifact.nonGroupingStageOutputs.first().testServices.moduleStructure.modules.last()
            val isWasiTarget = someModule.targetPlatform(testServices).isWasmWasi()

            val batchLauncherFile = generateGroupedBatchLauncherSource(context.filteredOutputs, someModule, tempDir, isWasiTarget)
            val settings = context.settings
            val perTestKlibPaths = context.perTestKlibPaths
            val cleanedRegularDependencies = context.cleanedRegularDependencies

            // Step 1: Compile ONLY the launcher into a small KLIB.
            val launcherKlibFile = tempDir.resolve("launcher.klib")
            val launcherModule = someModule.copy(files = listOf(batchLauncherFile))

            val firstStageFacade = WasmFirstStageInvoker(testServices)
            firstStageFacade.compileSourcesToKlib(
                launcherModule,
                listOf(batchLauncherFile.originalFile),
                launcherKlibFile,
                languageVersion = settings.maxLanguageVersion.versionString,
                customLanguageFeatures = settings.allLanguageFeatures,
                customOptIns = settings.allOptIns,
                allowKotlinPackage = settings.allAllowKotlinPackage,
                cleanedRegularDependencies + perTestKlibPaths,
                settings.friendDependencies,
            )

            // Step 2: Link and lower in-process
            val services = inputArtifact.nonGroupingStageOutputs.first().testServices
            val configuration = services.compilerConfigurationProvider.getCompilerConfiguration(launcherModule, CompilationStage.SECOND)
            configuration.includes = launcherKlibFile.absolutePath
            configuration.friendLibraries = settings.friendDependencies.toList()

            val runtimeKlibs = WasmEnvironmentConfigurator.getRuntimePathsForModule(configuration.wasmTarget, services)
            configuration.libraries = runtimeKlibs + cleanedRegularDependencies.toList() + settings.friendDependencies.toList() +
                    perTestKlibPaths + listOf(launcherKlibFile.absolutePath)

            val launcherKlibArtifact = BinaryArtifacts.KLib(launcherKlibFile, DiagnosticsCollectorStub())
            val result = withTemporarySingleModuleStructure(services, launcherModule) {
                // WasmLoweringFacade accesses module structure, expecting main module (proxy launcher in this grouped usecase) as first module.
                WasmBackendFacade(services).transform(launcherModule, launcherKlibArtifact)
            }

            val outputDir = services.getWasmTestOutputDirectory()
            outputDir.mkdirs()
            copyJsFilesToOutputDir(context.filteredOutputs.map { it.testServices to it.testModule }, outputDir)

            return result ?: testInfraError("WasmInProcessSecondStageFacade: groupedBatch produced no Wasm artifact")
        }

        private inline fun <T> withTemporarySingleModuleStructure(
            services: TestServices,
            module: TestModule,
            action: () -> T,
        ): T {
            // Keep `TestServices` mutation tightly scoped to only the backend transform call.
            // Grouped second-stage compilation uses a synthetic launcher module that should be visible
            // through `moduleStructure` for any downstream service that consults it.
            val originalModuleStructure = services.moduleStructure
            val temporaryModuleStructure = object : TestModuleStructure() {
                override val modules: List<TestModule> = listOf(module)
                override val allDirectives = originalModuleStructure.allDirectives
                override val originalTestDataFiles: List<File> = originalModuleStructure.originalTestDataFiles
            }

            services.register(TestModuleStructure::class, temporaryModuleStructure)
            return try {
                action()
            } finally {
                services.register(TestModuleStructure::class, originalModuleStructure)
            }
        }

        private fun doIsolated(context: BatchExecutionContext): BinaryArtifacts.Wasm {
            val filteredOutputs = context.filteredOutputs
            val services = filteredOutputs.first().testServices
            val testModules = filteredOutputs.map { it.testModule }
            val mainModule = testModules.lastOrNull { it.name != org.jetbrains.kotlin.wasm.test.WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME }
                    ?: testModules.last()

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
            copyJsFilesToOutputDir(testModules.map { services to it }, outputDir)
            val klibArtifact = BinaryArtifacts.KLib(File(includedLibrary), configuration.diagnosticsCollector)
            return WasmBackendFacade(services).transform(mainModule, klibArtifact)
                ?: testInfraError("WasmInProcessSecondStageFacade.doIsolatedInProcess() produced no Wasm artifact")
        }

        /*
         * The in-process facade uses the same compiler/stdlib for both stages, so the `compilationStage` does not affect the collected dependencies here.
         */
        override fun TestModule.collectDependencies(
            testServices: TestServices,
            compilationStage: CompilationStage,
        ): DependencyPaths {
            val [transitiveLibraries: List<File>, friendLibraries: List<File>] = getTransitivesAndFriends(module = this, testServices)

            val regularDependencies: Set<String> = buildSet {
                val wasmTarget = (targetPlatform(testServices).single() as WasmPlatformWithTarget).target
                add(WasmEnvironmentConfigurator.stdlibPath(wasmTarget, testServices))
                add(WasmEnvironmentConfigurator.kotlinTestPath(wasmTarget, testServices))
                transitiveLibraries.mapTo(this) { it.absolutePath }
            }

            val friendDependencies: Set<String> = friendLibraries.mapToSetOrEmpty { it.absolutePath }

            return DependencyPaths(regularDependencies, friendDependencies)
        }
    }
}
