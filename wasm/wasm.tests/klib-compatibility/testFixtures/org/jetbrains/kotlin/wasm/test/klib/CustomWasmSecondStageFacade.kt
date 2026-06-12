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
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.js.test.klib.CustomWebCompilerSettings
import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.platform.wasm.WasmPlatformWithTarget
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import org.jetbrains.kotlin.test.GroupingStageInputArtifact
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
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator.Companion.WASM_BASE_FILE_NAME
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.standardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.targetPlatform
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlin.wasm.test.WasmCoroutineHelpersModuleTransformer
import org.jetbrains.kotlin.wasm.test.converters.WasmFirstStageFacade
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerSecondStageFacade] for WasmJs and WasmWasi, to invoke current version of K/Wasm backend
 *
 * ## Overall approach
 *
 * Unlike the classic K/Wasm test approach with separate WASM executable per testcase, each verifying a single `box()` function return value,
 * multiple tests are grouped here into a single WASM executable using separate test functions annotated with @kotlin.test.Test.
 * The implementation is bulky, which is caused by the CLI restriction of a single "-Xinclude" argument, unlike K/Native
 * (which supports multiple "-Xinclude" arguments).
 *
 * The facade is invoked as the **second stage** of a two-stage test compilation pipeline:
 *
 * 1. **Stage 1 (`NonGroupingStage`)** compiles each individual test independently into a per-test KLIB.
 *    Each per-test KLIB contains the test sources (with their package optionally patched by
 *    [BatchingPackageInserter] to avoid name collisions between batched tests) plus the per-test
 *    `Launcher_<hash>` class synthesized by `WasmJsLauncherAdditionalSourceProvider` (a small `@Test`-annotated
 *    class that calls the test's `box()` and asserts the result equals `"OK"`).
 * 2. **Stage 2 (this facade, [Grouping.transform])** consumes a grouped *batch* of those per-test KLIBs and
 *    links them into one WASM executable, then ultimately into a [BinaryArtifacts.Wasm] folder containing
 *    `index.wasm`, `index.mjs`, etc. A custom `ProxyBatchLauncher.kt` is synthesized to provide the test
 *    entry point(s) used to drive the batched tests.
 *
 * The key constraint at the linker level is structural: on JS/Wasm, exactly **one** library can be passed as
 * `-Xinclude` (the "main" module whose IR is fed to second-stage lowerings such as `GenerateWasmTests`,
 * `WasmExport` lowering, DCE, etc.). All other KLIBs are loaded as ordinary `-libraries` and are *not*
 * re-lowered. This constraint shapes the three execution paths below.
 *
 * ## The three Stage-2 paths
 *
 * For each batch coming from the grouping engine, [Grouping.transform] dispatches to one of three paths,
 * depending on the batch's structure (isolated vs grouped; with or without `box()`). The paths are listed
 * in the order they appear in the code.
 *
 * ### groupedBatch — Non-isolated grouped batch (the common case)
 *
 * Triggered when [isIsolatedBatch] is `false`. This handles the bulk of WASM codegen-box tests, where many
 * independent tests are linked together into a single WASM executable for throughput. It is the path that
 * makes batching pay off: the structurally cheapest possible Stage 2 — only the synthetic launcher source
 * is compiled fresh; everything else (per-test sources) is reused as-is from the Stage 1 KLIB outputs.
 *
 *  - A single `ProxyBatchLauncher.kt` is synthesized containing one `ProxyLauncher_<hash>` `@Test` class per
 *    test in the batch (each calling its `box()` via the per-test FQN computed from
 *    [BatchingPackageInserter.computePackage] + [MainFunctionForBlackBoxTestsSourceProvider.detectPackage]),
 *    plus (on WASI) a `@WasmExport fun startTest()` that drives every `ProxyLauncher_*.runTest()` sequentially.
 *  - That launcher source is compiled into a small `launcher.klib` and linked as `-Xinclude` with all
 *    per-test KLIBs passed as ordinary `-libraries`. Per-test KLIBs are deduplicated against shared
 *    `helpers.klib` artifacts produced by [WasmCoroutineHelpersModuleTransformer] (all helper KLIBs in a
 *    batch declare the same `unique_name=helpers`, so only one copy can be passed to the linker).
 *  - In this path, `WasmJsLauncherAdditionalSourceProvider.produceAdditionalFiles()` short-circuits to an
 *    empty list — the per-test `Launcher_<hash>` is unused since `GenerateWasmTests` only visits the
 *    `launcher.klib` main module.
 *  - Aggregated batch settings (max `LANGUAGE_VERSION`, union of `LANGUAGE` features, union of `OPT_IN`s,
 *    `ALLOW_KOTLIN_PACKAGE` if any test requests it) are applied to both the launcher KLIB compilation and
 *    the final link, since all of these tests share one compiler invocation.
 *
 * ### isolatedWithBox — Isolated batch, has `box()`
 *
 * Triggered when [isIsolatedBatch] is `true` and `batchLauncherFile != null`. This unified path handles
 * both single-module isolated tests and multi-module isolated tests (with or without `-Xfriend-modules`
 * declarations such as `// MODULE: main()(lib1)`).
 *
 * Historically this was split into "Path B" (no friend dependencies, launcher KLIB used as `-Xinclude`)
 * and "Path C" (with friend dependencies, per-test KLIB used as `-Xinclude`). The two are now folded
 * into a single mechanism, because Path B's "launcher.klib as the included main module" approach
 * **breaks** for tests that declare friend modules: `-Xfriend-modules` declares friendship only with the
 * *included* module, so once the launcher KLIB takes that role the friend relation between the per-test
 * `main.klib` and its `lib*.klib` siblings is lost. Virtual dispatch of `internal open` declarations
 * crossing the friend boundary would then resolve to the base implementation instead of the override
 * (e.g. `WasmJsCodegenBoxTestGenerated$Box$Bridges.testInternalMethodOverrideInFriendModule` returns
 * the wrong value with the Path B layout). Routing every isolatedWithBox test through the friend-safe mechanism
 * removes that footgun once and for all, at the price of keeping `WasmJsLauncherAdditionalSourceProvider`
 * structurally required for isolated tests.
 *
 * Concretely:
 *
 *  - The per-test main KLIB is kept as `mainLibraries.first()` (i.e. the `-Xinclude` main module). This
 *    preserves any `-Xfriend-modules` friendship between sibling KLIBs of the same multi-module test.
 *  - The synthetic `ProxyBatchLauncher.kt` is still generated and passed as an additional free-arg source
 *    (`isAdditional = true`). It is silently dropped by the linking pipeline (`WasmCliPipeline` only runs
 *    `WasmConfigurationPhase + WasmBackendPipelinePhase` when `-Xinclude` is set — no
 *    `WebFrontendPipelinePhase` / `Fir2IrPhase` to compile sources), so its `ProxyLauncher_<hash>` class
 *    has no runtime effect here. It is retained as a diagnostic artifact (visible alongside the
 *    `launcher.klib` and per-test outputs in the temp folder) and to keep the sanity check's
 *    expected-suite-name set aligned with groupedBatch.
 *  - The per-test KLIB's own `Launcher_<hash>` class (added by `WasmJsLauncherAdditionalSourceProvider`
 *    during Stage 1) is what `GenerateWasmTests` actually picks up — it lives in the included main module
 *    and gets registered as the `@Test` test suite emitted by the runtime.
 *  - On WASI, the per-test KLIB also carries `wasiBoxTestRun.startTest()` (added by
 *    `WasmWasiBoxTestHelperSourceProvider`) — an `@WasmExport fun startTest()` that calls `box()` and
 *    is exported because the per-test KLIB is the included main module. This is the entry point that
 *    WasmEdge / Wasmtime invoke.
 *  - Therefore `WasmJsLauncherAdditionalSourceProvider` is structurally required for isolatedWithBox to work,
 *    even though groupedBatch does not need it.
 *
 * ### isolatedWithoutBox — Isolated batch, no `box()` anywhere (custom JS-driven tests)
 *
 * Triggered when [isIsolatedBatch] is `true` and `batchLauncherFile == null` (e.g. `Box$Size$Add`,
 * `Box$Size$HelloWorldPromise` — tests driven entirely by a custom `entry.mjs`/`index.mjs`).
 *
 *  - No `ProxyBatchLauncher.kt` is generated.
 *  - One of the per-test KLIBs is included as `-Xinclude` so any incidental lowerings still apply.
 *  - All `.mjs`/`.js` files from the test sources are copied into the executable folder, where they form the
 *    actual entry point that the VM invokes.
 *  - The post-run sanity check in `AbstractWasmFolderBoxRunnerGroupingStage` is skipped for these tests
 *    (`hasBoxMethod(input) == false`) — pass/fail is determined entirely by the VM exit code.
 *
 * ## Failure reporting
 *
 * After the VM run, `AbstractWasmFolderBoxRunnerGroupingStage` parses TeamCity-formatted output to attribute
 * failures to individual tests. A post-run sanity check verifies
 * that every test in the batch produced a matching `##teamcity[testSuiteFinished name='ProxyLauncher_<hash>'`
 * (or `Launcher_<hash>` for isolatedWithBox) line — guarding against silent test skips.
 *
 * @see WasmCoroutineHelpersModuleTransformer for how synthetic `helpers` files are extracted into a shared module.
 * @see BatchingPackageInserter for per-test package patching that enables batching without name collisions.
 * @see org.jetbrains.kotlin.wasm.test.providers.WasmJsLauncherAdditionalSourceProvider for the Stage-1 launcher
 *      that is essential to isolatedWithBox and structurally redundant in groupedBatch.
 * @see org.jetbrains.kotlin.wasm.test.handlers.AbstractWasmBoxRunnerGroupingStage for the run/verify side
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
            val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("combined-sources")
            val filteredOutputs = collectFilteredOutputs(inputArtifact)

            // An isolated batch (`BatchToken.Isolated`) is compiled as a standalone box-export test:
            // `box()` is exported via `@JsExport` (see `WasmJsExportBoxPreprocessor`) and invoked
            // directly by the runner. Everything else — real multi-test batches AND tests that merely
            // carried a unique batch token (e.g. `BatchToken.Custom` from a one-off `// LANGUAGE:`
            // feature) — goes through the grouped path: a `ProxyBatchLauncher` reaches `box()` via its
            // FQN, so no per-test `box` export is needed (which would otherwise clash across the batch).
            return if (isIsolatedBatch(inputArtifact)) {
                doIsolated(inputArtifact, filteredOutputs, tempDir, facade)
            } else {
                groupedBatch(inputArtifact, filteredOutputs, tempDir, facade)
            }
        }

        /**
         * groupedBatch — Non-isolated grouped batch (see the class-level KDoc for the full description).
         *
         * Generates a small `ProxyBatchLauncher.kt` covering all tests in the batch, compiles only
         * that file into a `launcher.klib`, and then links the launcher KLIB (as the `-Xinclude`
         * main module) together with all per-test KLIBs (passed as ordinary `-libraries`).
         */
        private fun groupedBatch(
            inputArtifact: GroupingStageInputArtifact,
            filteredOutputs: MutableList<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>,
            tempDir: File,
            facade: CustomWasmSecondStageFacade,
        ): BinaryArtifacts.Wasm {
            val someModule = inputArtifact.nonGroupingStageOutputs.first().testServices.moduleStructure.modules.last()
            val isWasiTarget = someModule.targetPlatform(testServices).isWasmWasi()

            val batchLauncherFile = generateGroupedBatchLauncherSource(filteredOutputs, someModule, tempDir, isWasiTarget)
            // The launcher KLIB (Step 1) is compiled with the *current* compiler, so it must be linked
            // against the *current* (FIRST-stage) standard libraries. The final executable (Step 2) is
            // linked with the *custom/previously-released* (SECOND-stage) compiler, so it must use the
            // matching SECOND-stage standard libraries — otherwise the per-test box code would be linked
            // against the current (e.g. already-bug-fixed) stdlib and an expected backend/runtime failure
            // of the old compiler (e.g. KT-86478) would not be reproduced.
            val firstStageSettings = aggregateBatchSettings(filteredOutputs, CompilationStage.FIRST)
            val secondStageSettings = aggregateBatchSettings(filteredOutputs, CompilationStage.SECOND)
            val perTestKlibPaths = deduplicateHelperKlibPaths(filteredOutputs)
            val cleanedFirstStageRegularDependencies =
                filterOutDuplicateHelperKlibs(filteredOutputs, firstStageSettings.regularDependencies, perTestKlibPaths)
            val cleanedSecondStageRegularDependencies =
                filterOutDuplicateHelperKlibs(filteredOutputs, secondStageSettings.regularDependencies, perTestKlibPaths)

            // Step 1: Compile ONLY the launcher into a small KLIB (a few lines of source, no test sources merged).
            val launcherKlibFile = tempDir.resolve("launcher.klib")
            val launcherModule = someModule.copy(files = listOf(batchLauncherFile))
            WasmFirstStageFacade(testServices).compileSourcesToKlib(
                launcherModule,
                listOf(batchLauncherFile.originalFile),
                launcherKlibFile,
                languageVersion = firstStageSettings.maxLanguageVersion.versionString,
                customLanguageFeatures = firstStageSettings.allLanguageFeatures,
                customOptIns = firstStageSettings.allOptIns,
                allowKotlinPackage = firstStageSettings.allAllowKotlinPackage,
                cleanedFirstStageRegularDependencies + perTestKlibPaths,
                firstStageSettings.friendDependencies,
            )

            // Step 2: Link the launcher KLIB (as the included "main" module) together with all per-test
            // KLIBs (passed as ordinary -libraries) into a WASM executable, using the SECOND-stage
            // (custom/previously-released) standard libraries.
            val moduleNameHash = someModule.name.hashCode().toHexString()
            val (exitCode, output, executableFolder) = facade.runCli(
                someModule.copy(files = emptyList()),
                moduleNameHash,
                customLanguageFeatures = secondStageSettings.allLanguageFeatures,
                customOptIns = secondStageSettings.allOptIns,
                allowKotlinPackage = secondStageSettings.allAllowKotlinPackage,
                includedLibrary = launcherKlibFile.absolutePath,
                libraries = perTestKlibPaths,
                regularDependencies = cleanedSecondStageRegularDependencies,
                friendDependencies = secondStageSettings.friendDependencies,
            )
            if (exitCode != ExitCode.OK) {
                // Throw an exception to abort further test execution.
                throw CustomKlibCompilerException(exitCode, output.toString(Charsets.UTF_8.name()))
            }
            // Copy additional non-Kotlin files (e.g. *.mjs, *.js) from per-test modules to the executable folder.
            for ([services, module, _] in filteredOutputs) {
                for (file in module.files) {
                    if (file.name.endsWith(".mjs") || file.name.endsWith(".js")) {
                        val content = services.sourceFileProvider.getContentOfSourceFile(file)
                        executableFolder.resolve(file.name).writeText(content)
                    }
                }
            }
            return WasmFolderBinaryArtifact(executableFolder)
        }

        private fun doIsolated(
            inputArtifact: GroupingStageInputArtifact,
            filteredOutputs: MutableList<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>,
            tempDir: File,
            facade: CustomWasmSecondStageFacade,
        ): BinaryArtifacts.Wasm {
            val services = inputArtifact.nonGroupingStageOutputs.first().testServices
            val testModules = filteredOutputs.map { it.second }
            // Pick the last non-helpers module as the main module. The helpers module is
            // synthesized by WasmCoroutineHelpersModuleTransformer and contains only the
            // synthetic `helpers` package files, never `box()`.
            val mainModule = testModules.lastOrNull { it.name != WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME }
                ?: testModules.last()

            val [regularDependencies, friendDependencies] = mainModule.collectDependencies(services, CompilationStage.SECOND)

            // Per-test KLIB paths (the artifacts produced by the NonGroupingStage for this isolated batch).
            val perTestKlibPathsIsolated = filteredOutputs.map { it.third.outputFile.absolutePath }.reversed()

            val fileWithBox = testModules.firstNotNullOfOrNull { module ->
                module.files.firstOrNull {
                    val content = services.sourceFileProvider.getContentOfSourceFile(it)
                    MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(content)
                }
            }

            return if (fileWithBox != null) {
                // For isolated tests, `BatchingPackageInserter.processModule()` skips patching when
                //   - the target is Native, OR
                //   - `WITH_REFLECT` is among the global directives, OR
                //   - any source file contains one of the `GroupingTestIsolator.ISOLATION_SOURCE_REGEXES`
                //     patterns (e.g., `// WASM_FAILS_IN: `, `::class.qualifiedName`, `import kotlin.reflect.`).
                // Mirror exactly that decision here so the FQN of `box()` in the generated launcher matches
                // what `BatchingPackageInserter` actually produced for the per-test KLIB.

                isolatedWithBox(
                    facade,
                    mainModule,
                    perTestKlibPathsIsolated,
                    regularDependencies,
                    friendDependencies,
                    testModules,
                    services
                )
            } else {
                isolatedWithoutBox(facade, mainModule, perTestKlibPathsIsolated, regularDependencies, friendDependencies, testModules, services)
            }
        }

        private fun isolatedWithoutBox(
            facade: CustomWasmSecondStageFacade,
            mainModule: TestModule,
            perTestKlibPathsIsolated: List<String>,
            regularDependencies: Set<String>,
            friendDependencies: Set<String>,
            testModules: List<TestModule>,
            services: TestServices,
        ): WasmFolderBinaryArtifact {
            // isolatedWithoutBox: No box() in any module of the isolated test: keep one of the per-test KLIBs
            // as the main module so that lowerings can still process whatever @Test classes
            // were generated for the per-test sources. These tests are driven by custom JS
            // entry points (e.g. `entry.mjs`), not by the unit-test runner.
            val (exitCode, output, executableFolder) = facade.runCli(
                mainModule.copy(files = emptyList()),
                mainModule.name.hashCode().toHexString(),
                customLanguageFeatures = mainModule.directives[LanguageSettingsDirectives.LANGUAGE],
                customOptIns = mainModule.directives[LanguageSettingsDirectives.OPT_IN],
                allowKotlinPackage = LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in mainModule.directives,
                includedLibrary = perTestKlibPathsIsolated.first(),
                libraries = perTestKlibPathsIsolated.drop(1),
                regularDependencies = regularDependencies,
                friendDependencies = friendDependencies,
            )
            return if (exitCode == ExitCode.OK) {
                // Copy all additional files to the executable folder
                for (testModule in testModules) {
                    for (file in testModule.files) {
                        if (file.name.endsWith(".mjs") || file.name.endsWith(".js")) {
                            val content = services.sourceFileProvider.getContentOfSourceFile(file)
                            executableFolder.resolve(file.name).writeText(content)
                        }
                    }
                }
                WasmFolderBinaryArtifact(executableFolder)
            } else {
                throw CustomKlibCompilerException(exitCode, output.toString(Charsets.UTF_8.name()))
            }
        }

        private fun isolatedWithBox(
            facade: CustomWasmSecondStageFacade,
            mainModule: TestModule,
            perTestKlibPathsIsolated: List<String>,
            regularDependencies: Set<String>,
            friendDependencies: Set<String>,
            testModules: List<TestModule>,
            services: TestServices,
        ): WasmFolderBinaryArtifact {
            // isolatedWithBox (isolated, has `box()`) — unified path that handles both the formerly-separate Path B (no friend dependencies)
            // and Path C (with friend dependencies) using a single mechanism:
            //
            //  - The per-test main KLIB is used as `mainLibraries.first()` (i.e. the
            //    `-Xinclude` main module). This preserves the friend relation declared by
            //    `-Xfriend-modules` between `main.klib` and any `lib*.klib` siblings of a
            //    multi-module test (which would otherwise be lost if the launcher KLIB were
            //    the included module — virtual dispatch of `internal open` declarations
            //    crossing the friend boundary would break).
            //  - The synthetic `ProxyBatchLauncher.kt` is passed as a free-arg source file
            //    (`isAdditional = true`). It is silently dropped by the linking pipeline
            //    (`WasmCliPipeline` only runs `WasmConfigurationPhase + WasmBackendPipelinePhase`
            //    when `-Xinclude` is set — no `WebFrontendPipelinePhase` / `Fir2IrPhase` to
            //    compile sources). That's fine: the per-test KLIB's `Launcher_<hash>` class
            //    (added by `WasmJsLauncherAdditionalSourceProvider` during Stage 1) is what
            //    `GenerateWasmTests` picks up, and on WASI the per-test KLIB's
            //    `wasiBoxTestRun.startTest()` (added by `WasmWasiBoxTestHelperSourceProvider`)
            //    is what becomes the entry point invoked by WasmEdge/Wasmtime.
            //  - Therefore `WasmJsLauncherAdditionalSourceProvider` is structurally required
            //    for this unified path to work — even though groupedBatch (non-isolated grouped
            //    batch) does not need it.
            //  - The `ProxyBatchLauncher.kt` content is still synthesized so it can serve as
            //    diagnostic output, and so the sanity check in
            //    `AbstractWasmFolderBoxRunnerGroupingStage.computeExpectedSuiteNames()`
            //    continues to accept the `Launcher_<hash>` suite name actually emitted at
            //    runtime.
            val (exitCode, output, executableFolder) = facade.runCli(
                mainModule,
                mainModule.name.hashCode().toHexString(),
                customLanguageFeatures = mainModule.directives[LanguageSettingsDirectives.LANGUAGE],
                customOptIns = mainModule.directives[LanguageSettingsDirectives.OPT_IN],
                allowKotlinPackage = LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in mainModule.directives,
                includedLibrary = perTestKlibPathsIsolated.first(),
                libraries = perTestKlibPathsIsolated.drop(1),
                regularDependencies = regularDependencies,
                friendDependencies = friendDependencies,
            )
            return if (exitCode == ExitCode.OK) {
                // Copy all additional files to the executable folder
                for (testModule in testModules) {
                    for (file in testModule.files) {
                        if (file.name.endsWith(".mjs") || file.name.endsWith(".js")) {
                            val content = services.sourceFileProvider.getContentOfSourceFile(file)
                            executableFolder.resolve(file.name).writeText(content)
                        }
                    }
                }
                WasmFolderBinaryArtifact(executableFolder)
            } else {
                throw CustomKlibCompilerException(exitCode, output.toString(Charsets.UTF_8.name()))
            }
        }

        override fun TestModule.collectDependencies(
            testServices: TestServices,
            compilationStage: CompilationStage,
        ): Pair<Set<String>, Set<String>> {
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
                        WasmTarget.WASI -> error("WASI target is not supported yet")
                    }
                }
                transitiveLibraries.mapTo(this) { it.absolutePath }
            }

            val friendDependencies: Set<String> = friendLibraries.mapToSetOrEmpty { it.absolutePath }

            return regularDependencies to friendDependencies
        }
    }

    data class CliRunResult(val exitCode: ExitCode, val output: ByteArrayOutputStream, val executableFolder: File)

    fun runCli(
        module: TestModule,
        dirName: String,
        customLanguageFeatures: List<String>,
        customOptIns: List<String>,
        allowKotlinPackage: Boolean,
        includedLibrary: String, // will be passed as `-Xinclude`, so its tests functions will be processed by GenerateWasmTests.
        libraries: List<String>, // will be passed in a list within `-libraries`
        regularDependencies: Set<String>, // will be passed in a list within `-libraries`
        friendDependencies: Set<String>, // will be passed as `-Xfriend-modules`, indicating a friendship with `includedLibrary`
    ): CliRunResult {
        val wasmArtifactFile = testServices.temporaryDirectoryManager.getOrCreateTempDirectory(dirName).resolve("$WASM_BASE_FILE_NAME.wasm")
        val compilerXmlOutput = ByteArrayOutputStream()
        val isWasmWasi = module.targetPlatform(testServices).isWasmWasi()

        val groupingStageInputs = runCatching { testServices.groupingStageInputs }.getOrNull().orEmpty()
        val allDirectivesOfFirstModule = groupingStageInputs.firstOrNull()?.testServices?.moduleStructure?.allDirectives
            ?: testServices.moduleStructure.allDirectives
        val isNewEH = USE_NEW_EXCEPTION_HANDLING_PROPOSAL in allDirectivesOfFirstModule
        val isOldEH = USE_OLD_EXCEPTION_HANDLING_PROPOSAL in allDirectivesOfFirstModule
        for (input in groupingStageInputs) {
            if (isNewEH) require(USE_NEW_EXCEPTION_HANDLING_PROPOSAL in input.testServices.moduleStructure.allDirectives) {
                "Malformed group: all tests in group must have same USE_NEW_EXCEPTION_HANDLING_PROPOSAL setting"
            }
            if (isOldEH) require(USE_OLD_EXCEPTION_HANDLING_PROPOSAL in input.testServices.moduleStructure.allDirectives) {
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
                    listOf(
                        CommonJsAndWasmCompilerArguments::libraries.cliArgument,
                        regularAndFriendDependencies.joinToString(File.pathSeparator),
                    )
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
                customLanguageFeatures
                    // Second stage CLI does not accept test-only features like `ImplicitSignedToUnsignedIntegerConversion`, so they need to be filtered out.
                    // `ImplicitSignedToUnsignedIntegerConversion` is a first-stage-only feature, so it's safe not to pass it to the second stage.
                    // Should some test-only feature be relevant to the second stage, then a split of the second stage to several CLI phases would help.
                    .filterNot { LanguageFeature.valueOf(it.removePrefix("+").removePrefix("-")).testOnly }
                    .map { CommonCompilerArguments::manuallyConfiguredFeatures.cliArgument + ":$it" },
                customOptIns.map { CommonCompilerArguments::optIn.cliArgument + "=$it" },
            )
        }

        if (exitCode == ExitCode.OK) {
            // Successfully compiled. Return the artifact.
            require(wasmArtifactFile.exists()) {
                "Internal testinfra error: Couldn't find expected generated wasm artifact ${wasmArtifactFile.absolutePath}"
            }

            return CliRunResult(exitCode, compilerXmlOutput, wasmArtifactFile.parentFile)
        } else {
            // Throw an exception to abort further test execution.
            throw CustomKlibCompilerException(exitCode, compilerXmlOutput.toString(Charsets.UTF_8.name()))
        }
    }
}
