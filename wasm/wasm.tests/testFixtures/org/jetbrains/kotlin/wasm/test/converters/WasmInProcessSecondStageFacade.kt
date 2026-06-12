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
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.wasm.config.wasmTarget
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.platform.wasm.WasmPlatformWithTarget
import org.jetbrains.kotlin.test.frontend.fir.getTransitivesAndFriends
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlin.wasm.test.blackbox.AbstractWasmSecondStageGroupingFacade
import org.jetbrains.kotlin.wasm.test.handlers.getWasmTestOutputDirectory
import java.io.File

/**
 * An in-process grouping stage facade that invokes [WasmBackendFacade] (deserialize + lower + compile)
 * for each test in the batch, producing a [org.jetbrains.kotlin.test.model.WasmCompilationSetsBinaryArtifact].
 *
 * This facade is the IN_PROCESS counterpart to [org.jetbrains.kotlin.wasm.test.blackbox.CustomWasmSecondStageFacade.Grouping] (which uses CLI).
 *
 * @see org.jetbrains.kotlin.wasm.test.blackbox.SecondStageInvocationMode.IN_PROCESS
 * @see org.jetbrains.kotlin.wasm.test.blackbox.CustomWasmSecondStageFacade.Grouping for the CLI-based counterpart
 * @see WasmBackendFacade for the underlying in-process compilation pipeline
 */
class WasmInProcessSecondStageFacade {
    class Grouping(
        testServices: TestServices,
    ) : AbstractWasmSecondStageGroupingFacade(testServices) {
        override fun transform(inputArtifact: GroupingStageInputArtifact): BinaryArtifacts.Wasm? {
            val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("combined-sources")
            val filteredOutputs = collectFilteredOutputs(inputArtifact)

            // A batch of a single test is always compiled as a standalone box-export test (see
            // AbstractWasmSecondStageGroupingFacade.isSingleTestBatch), whether it ended up alone
            // because it was isolated or merely because it carried a unique batch token.
            return if (isSingleTestBatch(inputArtifact)) {
                doIsolated(inputArtifact, filteredOutputs)
            } else {
                groupedBatch(inputArtifact, filteredOutputs, tempDir)
            }
        }

        private fun groupedBatch(
            inputArtifact: GroupingStageInputArtifact,
            filteredOutputs: List<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>,
            tempDir: File,
        ): BinaryArtifacts.Wasm {
            val someModule = inputArtifact.nonGroupingStageOutputs.first().testServices.moduleStructure.modules.last()
            val isWasiTarget = someModule.targetPlatform(testServices).isWasmWasi()

            val batchLauncherFile = generateGroupedBatchLauncherSource(filteredOutputs, someModule, tempDir, isWasiTarget)
            // The in-process facade uses the same compiler/stdlib for both stages, so the
            // compilation stage does not affect the collected dependencies here.
            val settings = aggregateBatchSettings(filteredOutputs, CompilationStage.SECOND)
            val perTestKlibPaths = deduplicateHelperKlibPaths(filteredOutputs)
            val cleanedRegularDependencies = filterOutDuplicateHelperKlibs(filteredOutputs, settings.regularDependencies, perTestKlibPaths)

            // Step 1: Compile ONLY the launcher into a small KLIB.
            val launcherKlibFile = tempDir.resolve("launcher.klib")
            val launcherModule = someModule.copy(files = listOf(batchLauncherFile))

            val firstStageFacade = WasmFirstStageFacade(testServices)
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
            configuration.libraries = runtimeKlibs + cleanedRegularDependencies.toList() + settings.friendDependencies.toList() + perTestKlibPaths + listOf(launcherKlibFile.absolutePath)

            val dummyModuleStructure = object : TestModuleStructure() {
                override val modules: List<TestModule> = listOf(launcherModule)
                override val allDirectives: org.jetbrains.kotlin.test.directives.model.RegisteredDirectives = services.moduleStructure.allDirectives
                override val originalTestDataFiles: List<File> = services.moduleStructure.originalTestDataFiles
            }

            val originalModuleStructure = services.moduleStructure
            services.register(TestModuleStructure::class, dummyModuleStructure)

            val launcherKlibArtifact = BinaryArtifacts.KLib(launcherKlibFile, DiagnosticsCollectorStub())
            val result = try {
                val backendFacade = WasmBackendFacade(services)
                backendFacade.transform(launcherModule, launcherKlibArtifact)
            } finally {
                services.register(TestModuleStructure::class, originalModuleStructure)
            }

            val outputDir = services.getWasmTestOutputDirectory()
            outputDir.mkdirs()
            for (filteredOutput in filteredOutputs) {
                val itemServices = filteredOutput.first
                val module = filteredOutput.second
                for (file in module.files) {
                    if (file.name.endsWith(".mjs") || file.name.endsWith(".js")) {
                        val content = itemServices.sourceFileProvider.getContentOfSourceFile(file)
                        outputDir.resolve(file.name).writeText(content)
                    }
                }
            }

            return result ?: error("WasmInProcessSecondStageFacade: groupedBatch produced no Wasm artifact")
        }

        private fun doIsolated(
            inputArtifact: GroupingStageInputArtifact,
            filteredOutputs: List<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>,
        ): BinaryArtifacts.Wasm {
            val services = inputArtifact.nonGroupingStageOutputs.first().testServices
            val testModules = filteredOutputs.map { it.second }
            val mainModule = testModules.lastOrNull { it.name != org.jetbrains.kotlin.wasm.test.WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME }
                    ?: testModules.last()

            val deps = mainModule.collectDependencies(services, CompilationStage.SECOND)
            val regularDependencies = deps.first
            val friendDependencies = deps.second
            val perTestKlibPathsIsolated = filteredOutputs.map { it.third.outputFile.absolutePath }.reversed()

            val fileWithBox = testModules.firstNotNullOfOrNull { module ->
                module.files.firstOrNull {
                    val content = services.sourceFileProvider.getContentOfSourceFile(it)
                    org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(content)
                }
            }

            return if (fileWithBox != null) {
                isolatedWithBox(
                    mainModule,
                    perTestKlibPathsIsolated,
                    regularDependencies,
                    friendDependencies,
                    testModules,
                    services
                )
            } else {
                isolatedWithoutBox(
                    mainModule,
                    perTestKlibPathsIsolated,
                    regularDependencies,
                    friendDependencies,
                    testModules,
                    services
                )
            }
        }

        private fun isolatedWithoutBox(
            mainModule: TestModule,
            perTestKlibPathsIsolated: List<String>,
            regularDependencies: Set<String>,
            friendDependencies: Set<String>,
            testModules: List<TestModule>,
            services: TestServices,
        ): BinaryArtifacts.Wasm {
            val includedLibrary = perTestKlibPathsIsolated.first()
            val libraries = perTestKlibPathsIsolated.drop(1)

            val configuration = services.compilerConfigurationProvider.getCompilerConfiguration(mainModule, CompilationStage.SECOND)
            configuration.includes = includedLibrary
            configuration.friendLibraries = friendDependencies.toList()

            val runtimeKlibs = WasmEnvironmentConfigurator.getRuntimePathsForModule(configuration.wasmTarget, services)
            configuration.libraries = runtimeKlibs + regularDependencies.toList() + friendDependencies.toList() + libraries + listOf(includedLibrary)

            val outputDir = services.getWasmTestOutputDirectory()
            outputDir.mkdirs()
            for (testModule in testModules) {
                for (file in testModule.files) {
                    if (file.name.endsWith(".mjs") || file.name.endsWith(".js")) {
                        val content = services.sourceFileProvider.getContentOfSourceFile(file)
                        outputDir.resolve(file.name).writeText(content)
                    }
                }
            }

            val klibArtifact = BinaryArtifacts.KLib(File(includedLibrary), configuration.diagnosticsCollector)
            val backendFacade = WasmBackendFacade(services)
            val result = backendFacade.transform(mainModule, klibArtifact)
            return result ?: error("WasmInProcessSecondStageFacade: isolatedWithoutBox produced no Wasm artifact")
        }

        private fun isolatedWithBox(
            mainModule: TestModule,
            perTestKlibPathsIsolated: List<String>,
            regularDependencies: Set<String>,
            friendDependencies: Set<String>,
            testModules: List<TestModule>,
            services: TestServices,
        ): BinaryArtifacts.Wasm {
            val includedLibrary = perTestKlibPathsIsolated.first()
            val libraries = perTestKlibPathsIsolated.drop(1)

            val configuration = services.compilerConfigurationProvider.getCompilerConfiguration(mainModule, CompilationStage.SECOND)
            configuration.includes = includedLibrary
            configuration.friendLibraries = friendDependencies.toList()

            val runtimeKlibs = WasmEnvironmentConfigurator.getRuntimePathsForModule(configuration.wasmTarget, services)
            configuration.libraries = runtimeKlibs + regularDependencies.toList() + friendDependencies.toList() + libraries + listOf(includedLibrary)

            val outputDir = services.getWasmTestOutputDirectory()
            outputDir.mkdirs()
            for (testModule in testModules) {
                for (file in testModule.files) {
                    if (file.name.endsWith(".mjs") || file.name.endsWith(".js")) {
                        val content = services.sourceFileProvider.getContentOfSourceFile(file)
                        outputDir.resolve(file.name).writeText(content)
                    }
                }
            }

            val klibArtifact = BinaryArtifacts.KLib(File(includedLibrary), configuration.diagnosticsCollector)
            val backendFacade = WasmBackendFacade(services)
            val result = backendFacade.transform(mainModule, klibArtifact)
            return result ?: error("WasmInProcessSecondStageFacade: isolatedWithBox produced no Wasm artifact")
        }

        /*
         * The in-process facade uses the same compiler/stdlib for both stages, so the `compilationStage` does not affect the collected dependencies here.
         */
        override fun TestModule.collectDependencies(
            testServices: TestServices,
            compilationStage: CompilationStage,
        ): Pair<Set<String>, Set<String>> {
            val [transitiveLibraries: List<File>, friendLibraries: List<File>] = getTransitivesAndFriends(module = this, testServices)

            val regularDependencies: Set<String> = buildSet {
                val wasmTarget = (targetPlatform(testServices).single() as WasmPlatformWithTarget).target
                add(WasmEnvironmentConfigurator.stdlibPath(wasmTarget, testServices))
                add(WasmEnvironmentConfigurator.kotlinTestPath(wasmTarget, testServices))
                transitiveLibraries.mapTo(this) { it.absolutePath }
            }

            val friendDependencies: Set<String> = friendLibraries.mapToSetOrEmpty { it.absolutePath }

            return regularDependencies to friendDependencies
        }
    }
}
