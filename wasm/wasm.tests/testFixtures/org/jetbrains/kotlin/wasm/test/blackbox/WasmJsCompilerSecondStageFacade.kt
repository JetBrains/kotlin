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
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.js.test.klib.CustomWebCompilerSettings
import org.jetbrains.kotlin.js.test.klib.collectDependencies
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.RETURN_VALUE_CHECKER_MODE
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_OLD_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.frontend.fir.getTransitivesAndFriends
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageFacade
import org.jetbrains.kotlin.test.model.AbstractGroupingStageTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.model.WasmFolderBinaryArtifact
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.test.impl.shouldIsolateTestInGroupingConfiguration
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator.Companion.WASM_BASE_FILE_NAME
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.targetPlatform
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlin.wasm.test.WasmCoroutineHelpersModuleTransformer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerSecondStageFacade] for WasmJs (and WasmWasi).
 *
 * Suits any backend version — either the current one or an older released one. It's specified via the
 * `webCompilerSettings` parameter.
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
 *    entry point(s) and a queryable `hasTestFailures()` export used by the JS test runner to detect failures.
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
 * ### Path A — Non-isolated grouped batch (the common case)
 *
 * Triggered when [isIsolatedBatch] is `false`. This handles the bulk of WASM codegen-box tests, where many
 * independent tests are linked together into a single WASM executable for throughput. It is the path that
 * makes batching pay off: the structurally cheapest possible Stage 2 — only the synthetic launcher source
 * is compiled fresh; everything else (per-test sources) is reused as-is from the Stage 1 KLIB outputs.
 *
 *  - A single `ProxyBatchLauncher.kt` is synthesized containing one `ProxyLauncher_<hash>` `@Test` class per
 *    test in the batch (each calling its `box()` via the per-test FQN computed from
 *    [BatchingPackageInserter.computePackage] + [MainFunctionForBlackBoxTestsSourceProvider.detectPackage]),
 *    plus a single `@WasmExport fun hasTestFailures()` and (on WASI) `@WasmExport fun startTest()` that
 *    drives every `ProxyLauncher_*.runTest()` sequentially.
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
 * ### Path BC — Isolated batch, has `box()`
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
 * the wrong value with the Path B layout). Routing every Path-BC test through the friend-safe mechanism
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
 *    expected-suite-name set aligned with Path A.
 *  - The per-test KLIB's own `Launcher_<hash>` class (added by `WasmJsLauncherAdditionalSourceProvider`
 *    during Stage 1) is what `GenerateWasmTests` actually picks up — it lives in the included main module
 *    and gets registered as the `@Test` test suite emitted by the runtime.
 *  - On WASI, the per-test KLIB also carries `wasiBoxTestRun.startTest()` (added by
 *    `WasmWasiBoxTestHelperSourceProvider`) — an `@WasmExport fun startTest()` that calls `box()` and
 *    is exported because the per-test KLIB is the included main module. This is the entry point that
 *    WasmEdge / Wasmtime invoke.
 *  - Therefore `WasmJsLauncherAdditionalSourceProvider` is structurally required for Path BC to work,
 *    even though Path A does not need it.
 *
 * ### Path D — Isolated batch, no `box()` anywhere (custom JS-driven tests)
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
 * Regardless of path, the synthesized `ProxyBatchLauncher` always exports `hasTestFailures()`. After the
 * VM run, `AbstractWasmFolderBoxRunnerGroupingStage` parses TeamCity-formatted output to attribute failures
 * to individual tests and uses `hasTestFailures()` as a robust fallback. A post-run sanity check verifies
 * that every test in the batch produced a matching `##teamcity[testSuiteFinished name='ProxyLauncher_<hash>'`
 * (or `Launcher_<hash>` for Path BC) line — guarding against silent test skips.
 *
 * @see WasmCoroutineHelpersModuleTransformer for how synthetic `helpers` files are extracted into a shared module.
 * @see BatchingPackageInserter for per-test package patching that enables batching without name collisions.
 * @see org.jetbrains.kotlin.wasm.test.providers.WasmJsLauncherAdditionalSourceProvider for the Stage-1 launcher
 *      that is essential to Path BC and structurally redundant in Path A.
 * @see org.jetbrains.kotlin.wasm.test.handlers.AbstractWasmFolderBoxRunnerGroupingStage for the run/verify side
 *      of the pipeline that consumes the [BinaryArtifacts.Wasm] produced here.
 */
class WasmJsCompilerSecondStageFacade private constructor(
    val testServices: TestServices,
    private val customWebCompilerSettings: CustomWebCompilerSettings
) {
    companion object {
        init {
            try {
                System.setProperty("kotlinc.test.allow.testonly.language.features", "true")
            } catch (_: Exception) {
                // In some environments setting properties might be prohibited
            }
        }
    }

    class Grouping(
        val testServices: TestServices,
        private val customWebCompilerSettings: CustomWebCompilerSettings
    ) : AbstractGroupingStageTestFacade<GroupingStageInputArtifact, BinaryArtifacts.Wasm>() {
        override fun transform(inputArtifact: GroupingStageInputArtifact): BinaryArtifacts.Wasm {
            val facade = WasmJsCompilerSecondStageFacade(testServices, customWebCompilerSettings)
            val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("combined-sources")
            val filteredOutputs = collectFilteredOutputs(inputArtifact)

            // Check if this is a single isolated test (which might be multi-module)
            val isIsolatedBatch = inputArtifact.nonGroupingStageOutputs.map { it.testServices.testInfo }.distinct().size == 1 &&
                    inputArtifact.nonGroupingStageOutputs.first().testServices.shouldIsolateTestInGroupingConfiguration(fileGenerationPhase = true)

            return if (isIsolatedBatch) {
                doIsolated(inputArtifact, filteredOutputs, tempDir, facade)
            } else {
                doNonIsolatedGroupedBatch(inputArtifact, filteredOutputs, tempDir, facade)
            }
        }

        /**
         * Collects the list of (testServices, testModule, KLib artifact) triples produced by the
         * NonGroupingStage that should participate in this Stage-2 batch.
         *
         * Modules whose failures are ignored (e.g. via `IGNORE_BACKEND`) or that do not have a
         * KLib artifact (e.g. because their Stage-1 compilation failed in an expected way) are
         * silently skipped.
         */
        private fun collectFilteredOutputs(
            inputArtifact: GroupingStageInputArtifact,
        ): MutableList<Triple<TestServices, TestModule, BinaryArtifacts.KLib>> {
            val filteredOutputs = mutableListOf<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>()
            for (output in inputArtifact.nonGroupingStageOutputs) {
                val services = output.testServices
                for (module in services.moduleStructure.modules) {
                    if (!services.codegenSuppressionChecker.failuresInModuleAreIgnored(module)) {
                        val artifact = try {
                            services.artifactsProvider.getArtifact(module, ArtifactKinds.KLib)
                        } catch (_: Exception) {
                            continue
                        }
                        filteredOutputs.add(Triple(services, module, artifact))
                    }
                }
            }
            return filteredOutputs
        }

        /**
         * Path A — Non-isolated grouped batch (see the class-level KDoc for the full description).
         *
         * Generates a small `ProxyBatchLauncher.kt` covering all tests in the batch, compiles only
         * that file into a `launcher.klib`, and then links the launcher KLIB (as the `-Xinclude`
         * main module) together with all per-test KLIBs (passed as ordinary `-libraries`).
         */
        private fun doNonIsolatedGroupedBatch(
            inputArtifact: GroupingStageInputArtifact,
            filteredOutputs: MutableList<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>,
            tempDir: File,
            facade: WasmJsCompilerSecondStageFacade,
        ): BinaryArtifacts.Wasm {
            val someModule = inputArtifact.nonGroupingStageOutputs.first().testServices.moduleStructure.modules.last()
            val isWasiTarget = someModule.targetPlatform(testServices).isWasmWasi()

            val batchLauncherFile = generatePathALauncherSource(filteredOutputs, someModule, tempDir, isWasiTarget)
            val settings = aggregateBatchSettings(filteredOutputs)
            val perTestKlibPaths = deduplicateHelperKlibPaths(filteredOutputs)
            val cleanedRegularDependencies = filterOutDuplicateHelperKlibs(filteredOutputs, settings.regularDependencies, perTestKlibPaths)

            // Step 1: Compile ONLY the launcher into a small KLIB (a few lines of source, no test sources merged).
            val launcherKlibFile = tempDir.resolve("launcher.klib")
            val launcherModule = someModule.copy(files = listOf(batchLauncherFile))
            facade.compileSourcesToKlib(
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

            // Step 2: Link the launcher KLIB (as the included "main" module) together with all per-test
            // KLIBs (passed as ordinary -libraries via mainLibraries.drop(1)) into a WASM executable.
            val moduleNameHash = someModule.name.hashCode().toHexString()
            val (exitCode, output, executableFolder) = facade.runCli(
                someModule.copy(files = emptyList()),
                moduleNameHash,
                customLanguageFeatures = settings.allLanguageFeatures,
                customOptIns = settings.allOptIns,
                allowKotlinPackage = settings.allAllowKotlinPackage,
                mainLibraries = listOf(launcherKlibFile.absolutePath) + perTestKlibPaths,
                regularDependencies = cleanedRegularDependencies,
                friendDependencies = settings.friendDependencies,
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

        /**
         * Generates the `ProxyBatchLauncher.kt` source for Path A — one `@Test`-annotated
         * `ProxyLauncher_<hash>` class per test in the batch, plus the shared
         * `@WasmExport fun hasTestFailures()` and (on WASI) `@WasmExport fun startTest()` entry
         * points.
         *
         * Writes the result to `tempDir/ProxyBatchLauncher.kt` and returns the corresponding
         * [TestFile] marked as an additional source so it can be passed to the compiler.
         */
        private fun generatePathALauncherSource(
            filteredOutputs: List<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>,
            someModule: TestModule,
            tempDir: File,
            isWasiTarget: Boolean,
        ): TestFile {
            val proxyClassNames = mutableListOf<String>()
            val proxyLauncherContent = buildString {
                append("import kotlin.test.Test\n")
                append("import kotlin.test.assertEquals\n\n")
                for ((services, _) in filteredOutputs.groupBy { it.first }) {
                    val mainModule = services.moduleStructure.modules.last()
                    val additionalPackage = BatchingPackageInserter.computePackage(services.testInfo)
                    val fileWithBox = mainModule.files.firstOrNull {
                        val content = services.sourceFileProvider.getContentOfSourceFile(it)
                        MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(content)
                    }
                    if (fileWithBox == null) continue

                    val originalPackage = fileWithBox.let { MainFunctionForBlackBoxTestsSourceProvider.detectPackage(it) }

                    val boxFqName = if (originalPackage != null) "$additionalPackage.$originalPackage.box" else "$additionalPackage.box"

                    val uniqueClassName = BatchingPackageInserter.computeProxyLauncherClassName(services.testInfo)
                    proxyClassNames += uniqueClassName
                    append("class $uniqueClassName {\n")
                    append("    @Test\n")
                    append("    fun runTest() {\n")
                    append("        val result = $boxFqName()\n")
                    append($$"        assertEquals(\"OK\", result, \"Test failed with: $result\")\n")
                    append("    }\n")
                    append("}\n\n")
                }

                append("\n@kotlin.wasm.WasmExport\n")
                append("fun hasTestFailures(): Boolean {\n")
                append("    return kotlin.test.hasTestFailures()\n")
                append("}\n")

                if (isWasiTarget) {
                    append(generateWasiStartTest(proxyClassNames))
                }
            }
            val tempFile = tempDir.resolve("ProxyBatchLauncher.kt")
            tempFile.writeText(proxyLauncherContent)
            return TestFile(
                "ProxyBatchLauncher.kt",
                proxyLauncherContent,
                tempFile,
                0,
                true,
                someModule.files.first().directives,
            )
        }

        /**
         * Aggregated dependencies and language settings of every test in a Path-A batch, applied
         * uniformly to both the launcher KLIB compilation and the final link.
         *
         * Aggregation rules:
         *  - `regularDependencies` and `friendDependencies` — union across all tests;
         *  - `maxLanguageVersion` — maximum across all tests (so e.g. a Kotlin 2.5-using test
         *    inside the batch raises the version for the whole batch);
         *  - `allLanguageFeatures` — union of `LANGUAGE` directives;
         *  - `allOptIns` — union of `OPT_IN` directives;
         *  - `allAllowKotlinPackage` — `true` if any test in the batch requested it.
         */
        private class BatchSettings(
            val regularDependencies: MutableSet<String>,
            val friendDependencies: MutableSet<String>,
            val maxLanguageVersion: org.jetbrains.kotlin.config.LanguageVersion,
            val allLanguageFeatures: List<String>,
            val allOptIns: List<String>,
            val allAllowKotlinPackage: Boolean,
        )

        private fun aggregateBatchSettings(
            filteredOutputs: List<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>,
        ): BatchSettings {
            val regularDependencies = mutableSetOf<String>()
            val friendDependencies = mutableSetOf<String>()
            for ([services, module, _] in filteredOutputs) {
                module.collectDependencies(services, customWebCompilerSettings).let { [regular, friend] ->
                    regularDependencies += regular
                    friendDependencies += friend
                }
            }

            val maxLanguageVersion = filteredOutputs.maxOf { [_, module, _] ->
                module.languageVersionSettings.languageVersion
            }

            val allLanguageFeatures = filteredOutputs.flatMap { [_, module, _] ->
                module.directives[LanguageSettingsDirectives.LANGUAGE]
            }.distinct()

            val allOptIns = filteredOutputs.flatMap { [_, module, _] ->
                module.directives[LanguageSettingsDirectives.OPT_IN]
            }.distinct()

            val allAllowKotlinPackage = filteredOutputs.any { [_, module, _] ->
                LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in module.directives
            }

            return BatchSettings(
                regularDependencies,
                friendDependencies,
                maxLanguageVersion,
                allLanguageFeatures,
                allOptIns,
                allAllowKotlinPackage,
            )
        }

        /**
         * Deduplicates the per-test KLIB paths so that at most one `helpers.klib` survives in the
         * resulting library list.
         *
         * When `WITH_COROUTINES` is used, each test in the batch contributes a separate
         * `helpers.klib` produced by `WasmCoroutineHelpersModuleTransformer`. All such helpers
         * KLIBs are byte-equivalent in a single batch (built from the same synthetic `helpers`
         * package files) and all carry the same KLIB `unique_name = "helpers"`. We keep only the
         * first one so the linker doesn't fail with `The same 'unique_name=helpers' found in more
         * than one library`.
         *
         * Note: per-test KLIBs may have OS-specific filenames (e.g. `kt19475-helpers.klib`), but
         * inside they all declare `unique_name = "helpers"`. We identify them by their `TestModule`
         * name alone, which is the constant `helpers` across the batch.
         */
        private fun deduplicateHelperKlibPaths(
            filteredOutputs: List<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>,
        ): List<String> =
            filteredOutputs
                .distinctBy { [_, module, artifact] ->
                    if (module.name == WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME) {
                        WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME
                    } else {
                        artifact.outputFile.absolutePath
                    }
                }
                .map { it.third.outputFile.absolutePath }

        /**
         * Removes from `regularDependencies` any `helpers.klib` path that comes from a per-test
         * output other than the one already kept by [deduplicateHelperKlibPaths].
         *
         * After deduplication the final list of libraries passed to the compiler must have at
         * most one helpers.klib path; the remaining helpers.klib that comes from `someModule`'s
         * `collectDependencies` is also a per-test artifact, but it is the only one we keep.
         */
        private fun filterOutDuplicateHelperKlibs(
            filteredOutputs: List<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>,
            regularDependencies: Set<String>,
            perTestKlibPaths: List<String>,
        ): MutableSet<String> {
            val helperKlibsInPerTest = filteredOutputs
                .filter { it.second.name == WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME }
                .map { it.third.outputFile.absolutePath }
                .toSet()
            val keptHelperKlib = perTestKlibPaths.firstOrNull { it in helperKlibsInPerTest }
            return regularDependencies.filterNotTo(mutableSetOf()) { dep ->
                dep in helperKlibsInPerTest && dep != keptHelperKlib
            }
        }

        /**
         * WasmEdge/Wasmtime invoke the `startTest` export as the entry point.
         * Since this is a grouped batch with many tests, `startTest` here drives all
         * ProxyLauncher_*.runTest() methods sequentially. We deliberately do NOT
         * rely on `box()` (there are many of them in different per-test KLIBs) and
         * do NOT rely on the synthetic `startUnitTests` symbol (it is generated by
         * the compiler backend and not callable from Kotlin source).
         */
        private fun generateWasiStartTest(proxyClassNames: List<String>): String = buildString {
            append("\n")
            append("@kotlin.wasm.WasmImport(\"wasi_snapshot_preview1\", \"proc_exit\")\n")
            append("private external fun wasiProcExit(code: Int)\n")
            append("\n")
            append("@kotlin.wasm.WasmExport\n")
            append("fun startTest() {\n")
            append("    try {\n")
            for (className in proxyClassNames) {
                append("        $className().runTest()\n")
            }
            append("        if (kotlin.test.hasTestFailures()) wasiProcExit(1)\n")
            append("    } catch (e: Throwable) {\n")
            append("        println(\"Failed with exception!\")\n")
            append("        println(e.message)\n")
            append("        println(e.printStackTrace())\n")
            append("        wasiProcExit(1)\n")
            append("    }\n")
            append("}\n")
        }

        private fun doIsolated(
            inputArtifact: GroupingStageInputArtifact,
            filteredOutputs: MutableList<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>,
            tempDir: File,
            facade: WasmJsCompilerSecondStageFacade,
        ): BinaryArtifacts.Wasm {
            val services = inputArtifact.nonGroupingStageOutputs.first().testServices
            val testModules = filteredOutputs.map { it.second }
            // Pick the last non-helpers module as the main module. The helpers module is
            // synthesized by WasmCoroutineHelpersModuleTransformer and contains only the
            // synthetic `helpers` package files, never `box()`.
            val mainModule = testModules.lastOrNull { it.name != WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME }
                ?: testModules.last()

            val [regularDependencies, friendDependencies] = mainModule.collectDependencies(services, customWebCompilerSettings)

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
                val needsSkipPatchPackages = BatchingPackageInserter.needsSkipPatchPackages(services.moduleStructure)
                val additionalPackage = BatchingPackageInserter.computePackage(services.testInfo)

                val batchLauncherFile =
                    generateBatchLauncherFile(fileWithBox, !needsSkipPatchPackages, additionalPackage, services, mainModule, tempDir)
                pathBC(
                    facade,
                    mainModule,
                    batchLauncherFile,
                    perTestKlibPathsIsolated,
                    regularDependencies,
                    friendDependencies,
                    testModules,
                    services
                )
            } else {
                pathD(facade, mainModule, perTestKlibPathsIsolated, regularDependencies, friendDependencies, testModules, services)
            }
        }

        private fun generateBatchLauncherFile(
            fileWithBox: TestFile,
            isPatched: Boolean,
            additionalPackage: String,
            services: TestServices,
            mainModule: TestModule,
            tempDir: File,
        ): TestFile {
            val originalPackage = MainFunctionForBlackBoxTestsSourceProvider.detectPackage(fileWithBox)
            val boxFqName = if (isPatched) {
                if (originalPackage != null) "$additionalPackage.$originalPackage.box" else "$additionalPackage.box"
            } else {
                if (originalPackage != null) "$originalPackage.box" else "box"
            }
            val uniqueClassName = BatchingPackageInserter.computeProxyLauncherClassName(services.testInfo)

            val proxyLauncherContent = buildString {
                append("import kotlin.test.Test\n")
                append("import kotlin.test.assertEquals\n\n")
                append("class $uniqueClassName {\n")
                append("    @Test\n")
                append("    fun runTest() {\n")
                append("        val result = $boxFqName()\n")
                append("        assertEquals(\"OK\", result, \"Test failed with: \$result\")\n")
                append("    }\n")
                append("}\n\n")

                append("@kotlin.wasm.WasmExport\n")
                append("fun hasTestFailures(): Boolean {\n")
                append("    return kotlin.test.hasTestFailures()\n")
                append("}\n")

                if (mainModule.targetPlatform(testServices).isWasmWasi()) {
                    // WasmEdge/Wasmtime invoke the `startTest` export as the entry point.
                    // For isolated tests we drive the single ProxyLauncher's runTest() and
                    // signal failure via wasiProcExit(1) — same mechanism as the grouped path.
                    append("\n")
                    append("@kotlin.wasm.WasmImport(\"wasi_snapshot_preview1\", \"proc_exit\")\n")
                    append("private external fun wasiProcExit(code: Int)\n")
                    append("\n")
                    append("@kotlin.wasm.WasmExport\n")
                    append("fun startTest() {\n")
                    append("    try {\n")
                    append("        $uniqueClassName().runTest()\n")
                    append("        if (kotlin.test.hasTestFailures()) wasiProcExit(1)\n")
                    append("    } catch (e: Throwable) {\n")
                    append("        println(\"Failed with exception!\")\n")
                    append("        println(e.message)\n")
                    append("        println(e.printStackTrace())\n")
                    append("        wasiProcExit(1)\n")
                    append("    }\n")
                    append("}\n")
                }
            }

            val tempFile = tempDir.resolve("ProxyBatchLauncher.kt")
            tempFile.writeText(proxyLauncherContent)
            return TestFile("ProxyBatchLauncher.kt", proxyLauncherContent, tempFile, 0, true, mainModule.files.first().directives)
        }

        private fun pathD(
            facade: WasmJsCompilerSecondStageFacade,
            mainModule: TestModule,
            perTestKlibPathsIsolated: List<String>,
            regularDependencies: Set<String>,
            friendDependencies: Set<String>,
            testModules: List<TestModule>,
            services: TestServices,
        ): WasmFolderBinaryArtifact {
            // Path D: No box() in any module of the isolated test: keep one of the per-test KLIBs
            // as the main module so that lowerings can still process whatever @Test classes
            // were generated for the per-test sources. These tests are driven by custom JS
            // entry points (e.g. `entry.mjs`), not by the unit-test runner.
            val (exitCode, output, executableFolder) = facade.runCli(
                mainModule.copy(files = emptyList()),
                mainModule.name.hashCode().toHexString(),
                customLanguageFeatures = mainModule.directives[LanguageSettingsDirectives.LANGUAGE],
                customOptIns = mainModule.directives[LanguageSettingsDirectives.OPT_IN],
                allowKotlinPackage = LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in mainModule.directives,
                mainLibraries = perTestKlibPathsIsolated,
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

        private fun pathBC(
            facade: WasmJsCompilerSecondStageFacade,
            mainModule: TestModule,
            batchLauncherFile: TestFile,
            perTestKlibPathsIsolated: List<String>,
            regularDependencies: Set<String>,
            friendDependencies: Set<String>,
            testModules: List<TestModule>,
            services: TestServices,
        ): WasmFolderBinaryArtifact {
            // Path BC (isolated, has `box()`) — unified path that handles both the
            // formerly-separate Path B (no friend dependencies) and Path C (with friend
            // dependencies) using a single mechanism:
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
            //    for this unified path to work — even though Path A (non-isolated grouped
            //    batch) does not need it.
            //  - The `ProxyBatchLauncher.kt` content is still synthesized so it can serve as
            //    diagnostic output, and so the sanity check in
            //    `AbstractWasmFolderBoxRunnerGroupingStage.computeExpectedSuiteNames()`
            //    continues to accept the `Launcher_<hash>` suite name actually emitted at
            //    runtime.
            val (exitCode, output, executableFolder) = facade.runCli(
                mainModule.copy(files = listOfNotNull(batchLauncherFile)),
                mainModule.name.hashCode().toHexString(),
                customLanguageFeatures = mainModule.directives[LanguageSettingsDirectives.LANGUAGE],
                customOptIns = mainModule.directives[LanguageSettingsDirectives.OPT_IN],
                allowKotlinPackage = LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in mainModule.directives,
                mainLibraries = perTestKlibPathsIsolated,
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

        override val inputKind: TestArtifactKind<GroupingStageInputArtifact>
            get() = GroupingStageInputArtifact.Kind
        override val outputKind: TestArtifactKind<BinaryArtifacts.Wasm>
            get() = ArtifactKinds.Wasm
    }

    class NonGrouping(
        testServices: TestServices,
        private val customWebCompilerSettings: CustomWebCompilerSettings,
    ) : CustomKlibCompilerSecondStageFacade<BinaryArtifacts.Wasm>(testServices) {
        override val outputKind get() = ArtifactKinds.Wasm

        override fun isMainModule(module: TestModule) = module == WasmEnvironmentConfigurator.getMainModule(testServices)
        override fun collectDependencies(module: TestModule) = module.collectDependencies(testServices, CompilationStage.SECOND)

        override fun compileBinary(
            module: TestModule,
            customArgs: List<String>,
            mainLibrary: String,
            regularDependencies: Set<String>,
            friendDependencies: Set<String>,
        ): BinaryArtifacts.Wasm {
            val facade = WasmJsCompilerSecondStageFacade(testServices, customWebCompilerSettings)
            val (exitCode, output, executableFolder) = facade.runCli(
                module,
                module.name,
                customLanguageFeatures = module.directives[LanguageSettingsDirectives.LANGUAGE],
                customOptIns = module.directives[LanguageSettingsDirectives.OPT_IN],
                allowKotlinPackage = LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in module.directives,
                mainLibraries = listOf(mainLibrary),
                regularDependencies = regularDependencies,
                friendDependencies = friendDependencies,
            )

            if (exitCode == ExitCode.OK) {
                // Successfully compiled. Return the artifact.
                return WasmFolderBinaryArtifact(executableFolder)
            } else {
                // Throw an exception to abort further test execution.
                throw CustomKlibCompilerException(exitCode, output.toString(Charsets.UTF_8.name()))
            }
        }
    }

    data class CliRunResult(val exitCode: ExitCode, val output: ByteArrayOutputStream, val executableFolder: File)

    private fun applyK2MPPArgs(
        module: TestModule,
        customLanguageFeatures: List<String>
    ): List<String> {
        val isMPP = customLanguageFeatures.any { it == "+MultiPlatformProjects" } ||
                module.directives[LanguageSettingsDirectives.LANGUAGE].contains("+MultiPlatformProjects")

        if (!isMPP) return emptyList()

        val allModules = testServices.moduleStructure.modules
        return buildList {
            allModules.forEach { add("-Xfragments=${it.name}") }

            allModules.forEach { m ->
                m.dependsOnDependencies.forEach { dependency ->
                    add("-Xfragment-refines=${m.name}:${dependency.dependencyModule.name}")
                }
            }

            allModules.forEach { m ->
                m.files.forEach { file ->
                    add("-Xfragment-sources=${m.name}:${file.originalFile.absolutePath}")
                }
            }
        }
    }

    private fun compileSourcesToKlib(
        module: TestModule,
        sources: List<File>,
        klibOutputFile: File,
        languageVersion: String,
        customLanguageFeatures: List<String>,
        customOptIns: List<String>,
        allowKotlinPackage: Boolean,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ): ExitCode {
        val returnValueCheckerModes: List<ReturnValueCheckerMode> = module.directives[RETURN_VALUE_CHECKER_MODE]
        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val isWasmWasi = module.targetPlatform(testServices).isWasmWasi()
            val regularAndFriendDependencies = regularDependencies + friendDependencies
            customWebCompilerSettings.customKlibCompiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    runIf(isWasmWasi) {
                        KotlinWasmCompilerArguments::wasmTarget.cliArgument(WasmTarget.WASI.alias)
                    },
                    CommonCompilerArguments::languageVersion.cliArgument(languageVersion),
                    CommonJsAndWasmCompilerArguments::outputDir.cliArgument, klibOutputFile.parentFile.path,
                    CommonJsAndWasmCompilerArguments::moduleName.cliArgument, klibOutputFile.nameWithoutExtension,
                    KotlinWasmCompilerArguments::wasmEnableArrayRangeChecks.cliArgument,
                    CommonCompilerArguments::disableDefaultScriptingPlugin.cliArgument,
                    runIf(allowKotlinPackage) {
                        CommonCompilerArguments::allowKotlinPackage.cliArgument
                    }
                ),
                sources.filter { it.name.endsWith(".kt") }.map { it.absolutePath },
                runIf(regularAndFriendDependencies.isNotEmpty()) {
                    listOf(
                        CommonJsAndWasmCompilerArguments::libraries.cliArgument,
                        regularAndFriendDependencies.joinToString(File.pathSeparator),
                    )
                },
                runIf(friendDependencies.isNotEmpty()) {
                    listOf(CommonJsAndWasmCompilerArguments::friendModules.cliArgument(friendDependencies.joinToString(File.pathSeparator)))
                },
                returnValueCheckerModes.map {
                    CommonCompilerArguments::returnValueChecker.cliArgument(it.state)
                },
                customLanguageFeatures
                    .map { CommonCompilerArguments::manuallyConfiguredFeatures.cliArgument + ":$it" },
                customOptIns.map { CommonCompilerArguments::optIn.cliArgument + "=$it" },
                applyK2MPPArgs(module, customLanguageFeatures),
            )
        }
        if (exitCode != ExitCode.OK) {
            val outputStr = compilerXmlOutput.toString(Charsets.UTF_8.name())
            throw CustomKlibCompilerException(exitCode, outputStr)
        }
        return exitCode
    }

    fun runCli(
        module: TestModule,
        dirName: String,
        customLanguageFeatures: List<String>,
        customOptIns: List<String>,
        allowKotlinPackage: Boolean,
        mainLibraries: List<String>,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ): CliRunResult {
        val wasmArtifactFile = testServices.temporaryDirectoryManager.getOrCreateTempDirectory(dirName).resolve("$WASM_BASE_FILE_NAME.wasm")
        val compilerXmlOutput = ByteArrayOutputStream()
        val isWasmWasi = module.targetPlatform(testServices).isWasmWasi()

        val groupingStageInputs = testServices.groupingStageInputs
        val allDirectivesOfFirstModule = groupingStageInputs.first().testServices.moduleStructure.allDirectives
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
            val regularAndFriendDependencies = regularDependencies + friendDependencies + mainLibraries.drop(1)
            customWebCompilerSettings.customKlibCompiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    runIf(isWasmWasi) {
                        KotlinWasmCompilerArguments::wasmTarget.cliArgument(WasmTarget.WASI.alias)
                    },
                    CommonJsAndWasmCompilerArguments::irProduceJs.cliArgument,
                    runIf(mainLibraries.isNotEmpty()) {
                        KotlinWasmCompilerArguments::includes.cliArgument(mainLibraries.first())
                    },
                    CommonJsAndWasmCompilerArguments::outputDir.cliArgument, wasmArtifactFile.parentFile.path,
                    CommonJsAndWasmCompilerArguments::moduleName.cliArgument, WASM_BASE_FILE_NAME,
                    KotlinWasmCompilerArguments::wasmEnableArrayRangeChecks.cliArgument,
                    CommonCompilerArguments::disableDefaultScriptingPlugin.cliArgument,
                    runIf(allowKotlinPackage) {
                        CommonCompilerArguments::allowKotlinPackage.cliArgument
                    }
                ),
                module.files.filter { it.name.endsWith(".kt") && (it.isAdditional || mainLibraries.isEmpty()) }.map { it.originalFile.absolutePath },
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
                    .map { CommonCompilerArguments::manuallyConfiguredFeatures.cliArgument + ":$it" },
                customOptIns.map { CommonCompilerArguments::optIn.cliArgument + "=$it" },
                applyK2MPPArgs(module, customLanguageFeatures),
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

fun TestModule.collectDependencies(
    testServices: TestServices,
    customWebCompilerSettings: CustomWebCompilerSettings,
): Pair<Set<String>, Set<String>> {
    val [transitiveLibraries: List<File>, friendLibraries: List<File>] = getTransitivesAndFriends(module = this, testServices)

    val regularDependencies: Set<String> = buildSet {
        add(customWebCompilerSettings.stdlib.absolutePath)
        add(customWebCompilerSettings.kotlinTest.absolutePath)
        transitiveLibraries.mapTo(this) { it.absolutePath }
    }

    val friendDependencies: Set<String> = friendLibraries.mapToSetOrEmpty { it.absolutePath }

    return regularDependencies to friendDependencies
}
