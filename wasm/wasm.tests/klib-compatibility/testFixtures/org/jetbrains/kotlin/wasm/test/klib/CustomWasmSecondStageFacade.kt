/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.js.test.klib.CustomWebCompilerSettings
import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.platform.wasm.WasmPlatformWithTarget
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_OLD_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.frontend.fir.getTransitivesAndFriends
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.model.WasmFolderBinaryArtifact
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator.Companion.WASM_BASE_FILE_NAME
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlin.wasm.test.WasmCoroutineHelpersModuleTransformer
import org.jetbrains.kotlin.wasm.test.converters.WasmFirstStageInvoker
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerSecondStageFacade] for WasmJs and WasmWasi, invoking the current version of the K/Wasm backend.
 *
 * Many independent tests are batched into a single WASM executable (instead of one executable per test) for throughput, using
 * separate `@kotlin.test.Test`-annotated functions. This is the **second stage** of a two-stage pipeline:
 * Stage 1 (`NonGroupingStage`) compiles each test independently into a per-test KLIB;
 * then this facade ([Grouping.transform]) links a *batch* of those KLIBs into one [BinaryArtifacts.Wasm] executable.
 *
 * [Grouping.transform] dispatches to one of two linking strategies depending on the batch's structure:
 * - `groupedBatch` for non-isolated (regular) batches,
 * - `doIsolated` for isolated batches (with or without `box()`).
 * The strategy split exists since JS/Wasm allows only **one** `-Xinclude` library per link.
 *
 * @see org.jetbrains.kotlin.wasm.test.handlers.AbstractWasmGroupingStageBoxRunner for the run/verify side
 *      of the pipeline that consumes the [BinaryArtifacts.Wasm] produced here.
 */
class CustomWasmSecondStageFacade internal constructor(
    val testServices: TestServices,
    private val customWebCompilerSettings: CustomWebCompilerSettings
) {
    class Grouping(
        testServices: TestServices,
        val customWebCompilerSettings: CustomWebCompilerSettings
    ) : AbstractWasmSecondStageGroupingFacade(testServices) {
        override fun transform(inputArtifact: GroupingStageInputArtifact): BinaryArtifacts.Wasm {
            val facade = CustomWasmSecondStageFacade(testServices, customWebCompilerSettings)
            val filteredOutputs = collectFilteredOutputs(inputArtifact)
            val secondStageContext = buildBatchExecutionContext(filteredOutputs, CompilationStage.SECOND)

            // === Why this facade dispatches on `isIsolatedBatch`, NOT `isSingleTestBatch` ===
            //
            // An isolated batch (`BatchToken.Isolated`) is compiled as a standalone box-export test:
            // `box()` is exported via `@JsExport` (see `WasmJsExportBoxPreprocessor`) and invoked
            // directly by the runner. Everything else — real multi-test batches AND tests that merely
            // carried a unique batch token (e.g. `BatchToken.Custom` from a one-off `// LANGUAGE:`
            // feature) yet ended up alone in their batch — goes through the grouped path: a
            // `ProxyBatchLauncher` reaches `box()` via its FQN, so no per-test `box` export is needed
            // (which would otherwise clash across the batch).
            //
            // It is tempting to align this with the in-process facade and use `isSingleTestBatch` here,
            // but that is NOT possible for this CLI / KLIB-compatibility pipeline. The reason is timing:
            //
            //  * `box()` is exported as `@JsExport` by `WasmJsExportBoxPreprocessor`, which runs at
            //    FIRST-stage compile time and gates the export on
            //    `shouldIsolateTestInGroupingConfiguration(fileGenerationPhase = true)` — i.e. on the
            //    *isolation* decision alone.
            //  * At that moment the grouping engine has not yet decided which tests will share a batch,
            //    so the only signal available is isolation, not final batch size. A non-isolated test
            //    that merely *ends up alone* in a batch (a unique `BatchToken.Custom`) is therefore
            //    compiled WITHOUT `@JsExport box`.
            //
            // Consequently, if the second stage routed such a single-but-non-isolated test to the
            // box-export (`doIsolated`) path, the runner would call `jsModule.box()` but `box` was never
            // exported → every such test would fail, passing or not (verified: switching to
            // `isSingleTestBatch` here makes the `CustomWasmJsCompilerSecondStageSanity` suite fail).
            // Exporting `box` unconditionally at first stage is not an option either: it re-introduces
            // the clashing-`box`-export problem (`Identifier 'box' has already been declared`) when
            // several such KLIBs are linked into one grouped batch.
            //
            // The in-process facade (`WasmInProcessSecondStageFacade.Grouping`) legitimately uses
            // `isSingleTestBatch` because it does NOT depend on this first-stage `@JsExport` gating:
            // `WasmLoweringFacade.transform()` sets `wasmTestBoxFunctionToExport` per compilation, so
            // `box` availability is not tied to the early isolation decision there. See that facade's
            // counterpart comment for the other side of this divergence.
            return if (isIsolatedBatch(inputArtifact)) {
                doIsolated(secondStageContext, facade)
            } else {
                val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("combined-sources")
                val firstStageContext = buildBatchExecutionContext(filteredOutputs, CompilationStage.FIRST)
                groupedBatch(inputArtifact, firstStageContext, secondStageContext, tempDir, facade)
            }
        }

        /**
         * groupedBatch — Non-isolated grouped batch: the common case, and the path that makes batching pay off.
         *
         * Generates a small `ProxyBatchLauncher.kt` containing one `ProxyLauncher_<hash>` `@Test` class per test in
         * the batch (each calling its `box()` via the per-test FQN, computed from [BatchingPackageInserter.computePackage]
         * + [MainFunctionForBlackBoxTestsSourceProvider.detectPackage]), plus (on WASI) a `@WasmExport fun startTest()`
         * driving every `ProxyLauncher_*.runTest()` sequentially. Only that launcher source is compiled fresh, into a
         * small `launcher.klib`, which is then linked as `-Xinclude` together with all per-test KLIBs passed as ordinary
         * `-libraries` (deduplicated against shared `helpers.klib` artifacts from [WasmCoroutineHelpersModuleTransformer],
         * since all helper KLIBs in a batch share `unique_name=helpers`) — everything else is reused as-is from Stage 1.
         *
         * Since `GenerateWasmTests` only visits the `launcher.klib` main module here, the per-test `Launcher_<hash>`
         * class is unused, so `WasmJsLauncherAdditionalSourceProvider.produceAdditionalFiles()` short-circuits to an empty list for this path.
         * Aggregated batch settings (max `LANGUAGE_VERSION`, union of `OPT_IN`s, `ALLOW_KOTLIN_PACKAGE` if requested by any test)
         * are applied to both the launcher KLIB compilation and the final link, since all tests in the batch share one compiler invocation.
         */
        private fun groupedBatch(
            inputArtifact: GroupingStageInputArtifact,
            firstStageContext: BatchExecutionContext,
            secondStageContext: BatchExecutionContext,
            tempDir: File,
            facade: CustomWasmSecondStageFacade,
        ): BinaryArtifacts.Wasm {
            val someModule = inputArtifact.nonGroupingStageOutputs.first().testServices.moduleStructure.modules.last()
            val isWasiTarget = someModule.targetPlatform(testServices).isWasmWasi()

            val filteredOutputs = secondStageContext.filteredOutputs
            val firstStageSettings = firstStageContext.settings
            val secondStageSettings = secondStageContext.settings
            val perTestKlibPaths = secondStageContext.perTestKlibPaths
            val cleanedFirstStageRegularDependencies = firstStageContext.cleanedRegularDependencies
            val cleanedSecondStageRegularDependencies = secondStageContext.cleanedRegularDependencies

            val batchLauncherFile = generateGroupedBatchLauncherSource(filteredOutputs, someModule, tempDir, isWasiTarget)

            // Step 1: Compile ONLY the launcher into a small KLIB (a few lines of source, no test sources merged).
            val launcherKlibFile = tempDir.resolve("launcher.klib")
            val launcherModule = someModule.copy(files = listOf(batchLauncherFile))
            WasmFirstStageInvoker(testServices).compileSourcesToKlib(
                launcherModule,
                listOf(batchLauncherFile.originalFile),
                launcherKlibFile,
                languageVersion = firstStageSettings.maxLanguageVersion,
                customOptIns = firstStageSettings.allOptIns,
                allowKotlinPackage = firstStageSettings.allAllowKotlinPackage,
                cleanedFirstStageRegularDependencies + perTestKlibPaths,
                firstStageSettings.friendDependencies,
            )

            // Step 2: Link the launcher KLIB (as the included "main" module) together with all per-test KLIBs (passed as ordinary -libraries)
            // into a WASM executable, using the SECOND-stage (custom/previously-released) standard libraries.
            val executableFolder = facade.runCli(
                module = someModule.copy(files = emptyList()),
                dirName = someModule.name.hashCode().toHexString(),
                customOptIns = secondStageSettings.allOptIns,
                allowKotlinPackage = secondStageSettings.allAllowKotlinPackage,
                includedLibrary = launcherKlibFile.absolutePath,
                libraries = perTestKlibPaths,
                regularDependencies = cleanedSecondStageRegularDependencies,
                friendDependencies = secondStageSettings.friendDependencies,
            )
            // Copy additional non-Kotlin files (e.g. *.mjs, *.js) from per-test modules to the executable folder.
            copyJsFilesToOutputDir(filteredOutputs.map { it.testServices to it.testModule }, executableFolder)
            return WasmFolderBinaryArtifact(executableFolder)
        }

        private fun doIsolated(
            context: BatchExecutionContext,
            facade: CustomWasmSecondStageFacade,
        ): BinaryArtifacts.Wasm {
            val filteredOutputs = context.filteredOutputs
            val services = filteredOutputs.first().testServices
            val testModules = filteredOutputs.map { it.testModule }
            // Pick the last non-helpers module as the main module. The helpers module is
            // synthesized by WasmCoroutineHelpersModuleTransformer and contains only the
            // synthetic `helpers` package files, never `box()`.
            val mainModule = testModules.lastOrNull { it.name != WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME }
                ?: testModules.last()

            val [regularDependencies, friendDependencies] = mainModule.collectDependencies(services, CompilationStage.SECOND)

            // Per-test KLIB paths (the artifacts produced by the NonGroupingStage for this isolated batch).
            val perTestKlibPathsIsolated = filteredOutputs.map { it.klib.outputFile.absolutePath }.reversed()

            val fileWithBox = testModules.firstNotNullOfOrNull { module ->
                module.files.firstOrNull {
                    val content = services.sourceFileProvider.getContentOfSourceFile(it)
                    MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(content)
                }
            }

            // The per-test main KLIB is used as `-Xinclude`, preserving any `-Xfriend-modules` friendship with sibling KLIBs.
            // Sources files are removed from mainModule in case no `box()` was found, since a custom `.mjs`/`.js` entry point drives the test instead
            val module = if (fileWithBox != null) mainModule else mainModule.copy(files = emptyList())
            val executableFolder = facade.runCli(
                module,
                dirName = module.name.hashCode().toHexString(),
                customOptIns = mainModule.directives[LanguageSettingsDirectives.OPT_IN],
                allowKotlinPackage = LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in mainModule.directives,
                includedLibrary = perTestKlibPathsIsolated.first(),
                libraries = perTestKlibPathsIsolated.drop(1),
                regularDependencies = regularDependencies,
                friendDependencies = friendDependencies,
            )
            // Copy all additional files to the executable folder
            copyJsFilesToOutputDir(testModules.map { services to it }, executableFolder)
            return WasmFolderBinaryArtifact(executableFolder)
        }

        override fun TestModule.collectDependencies(
            testServices: TestServices,
            compilationStage: CompilationStage,
        ): DependencyPaths {
            val [transitiveLibraries: List<File>, friendLibraries: List<File>] = getTransitivesAndFriends(module = this, testServices)

            val regularDependencies: Set<String> = buildSet {
                val wasmTarget = (targetPlatform(testServices).single() as WasmPlatformWithTarget).target
                if (compilationStage == CompilationStage.FIRST) { // stdlib and kotlin-test must be of current version
                    add(testServices.standardLibrariesPathProvider.fullWasmStdlib(wasmTarget).absolutePath)
                    add(testServices.standardLibrariesPathProvider.kotlinTestWasmKLib(wasmTarget).absolutePath)
                } else { // SECOND stage uses old compiler version, so standard libs must match it.
                    when (wasmTarget) {
                        WasmTarget.JS -> {
                            add(customWasmJsCompilerSettings.stdlib.absolutePath)
                            add(customWasmJsCompilerSettings.kotlinTest.absolutePath)
                        }
                        WasmTarget.WASI -> testInfraError("WASI target is not supported yet")
                    }
                }
                transitiveLibraries.mapTo(this) { it.absolutePath }
            }

            val friendDependencies: Set<String> = friendLibraries.mapToSetOrEmpty { it.absolutePath }

            return DependencyPaths(regularDependencies, friendDependencies)
        }
    }

    /*
     * Returns Wasm executableFolder
     */
    fun runCli(
        module: TestModule,
        dirName: String,
        customOptIns: List<String>,
        allowKotlinPackage: Boolean,
        includedLibrary: String, // will be passed as `-Xinclude`, so its tests functions will be processed by GenerateWasmTests.
        libraries: List<String>, // will be passed in a list within `-libraries`
        regularDependencies: Set<String>, // will be passed in a list within `-libraries`
        friendDependencies: Set<String>, // will be passed as `-Xfriend-modules`, indicating a friendship with `includedLibrary`
    ): File {
        val wasmArtifactFile = testServices.temporaryDirectoryManager.getOrCreateTempDirectory(dirName).resolve("$WASM_BASE_FILE_NAME.wasm")
        val compilerXmlOutput = ByteArrayOutputStream()
        val isWasmWasi = module.targetPlatform(testServices).isWasmWasi()

        val groupingStageInputs = try {
            testServices.groupingStageInputs
        } catch (_: IllegalStateException) {
            emptyList() // service GroupingStageInputsHolder is not registered
        }
        val allDirectivesOfFirstModule = groupingStageInputs.firstOrNull()?.testServices?.moduleStructure?.allDirectives
            ?: testServices.moduleStructure.allDirectives
        val isNewEH = USE_NEW_EXCEPTION_HANDLING_PROPOSAL in allDirectivesOfFirstModule
        val isOldEH = USE_OLD_EXCEPTION_HANDLING_PROPOSAL in allDirectivesOfFirstModule
        for (input in groupingStageInputs) {
            if (isNewEH) checkTestInfrastructure(USE_NEW_EXCEPTION_HANDLING_PROPOSAL in input.testServices.moduleStructure.allDirectives) {
                "Malformed group: all tests in group must have same USE_NEW_EXCEPTION_HANDLING_PROPOSAL setting"
            }
            if (isOldEH) checkTestInfrastructure(USE_OLD_EXCEPTION_HANDLING_PROPOSAL in input.testServices.moduleStructure.allDirectives) {
                "Malformed group: all tests in group must have same USE_OLD_EXCEPTION_HANDLING_PROPOSAL setting"
            }
        }

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val regularAndFriendDependencies = regularDependencies + friendDependencies + libraries
            customWebCompilerSettings.customKlibCompiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    runIf(isWasmWasi) {
                        KotlinWasmCompilerArguments::wasmTarget.cliArgument(WasmTarget.WASI.alias)
                    },
                    CommonJsAndWasmCompilerArguments::irProduceJs.cliArgument,
                    KotlinWasmCompilerArguments::includes.cliArgument(includedLibrary),
                    CommonJsAndWasmCompilerArguments::outputDir.cliArgument, wasmArtifactFile.parentFile.path,
                    CommonJsAndWasmCompilerArguments::moduleName.cliArgument, WASM_BASE_FILE_NAME,
                    KotlinWasmCompilerArguments::wasmEnableArrayRangeChecks.cliArgument,
                    CommonCompilerArguments::disableDefaultScriptingPlugin.cliArgument,
                    runIf(allowKotlinPackage) {
                        CommonCompilerArguments::allowKotlinPackage.cliArgument
                    }
                ),
                runIf(regularAndFriendDependencies.isNotEmpty()) {
                    listOf(CommonJsAndWasmCompilerArguments::libraries.cliArgument(regularAndFriendDependencies.joinToString(File.pathSeparator)))
                },
                runIf(friendDependencies.isNotEmpty()) {
                    listOf(CommonJsAndWasmCompilerArguments::friendModules.cliArgument(friendDependencies.joinToString(File.pathSeparator)))
                },
                runIf(isNewEH) {
                    listOf(KotlinWasmCompilerArguments::wasmUseNewExceptionProposal.cliArgument)
                },
                runIf(isOldEH) {
                    listOf(KotlinWasmCompilerArguments::wasmUseNewExceptionProposal.cliArgument("false"))
                },
                customOptIns.map { CommonCompilerArguments::optIn.cliArgument + "=$it" },
            )
        }

        if (exitCode == ExitCode.OK) {
            // Successfully compiled. Return the artifact.
            checkTestInfrastructure(wasmArtifactFile.exists()) {
                "Internal testinfra error: Couldn't find expected generated wasm artifact ${wasmArtifactFile.absolutePath}"
            }

            return wasmArtifactFile.parentFile
        } else {
            // Throw an exception to abort further test execution.
            throw CustomKlibCompilerException(exitCode, compilerXmlOutput.toString(Charsets.UTF_8.name()))
        }
    }
}
