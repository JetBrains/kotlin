/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.impl.shouldIsolateTestInGroupingConfiguration
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.isSingleTestBatch
import org.jetbrains.kotlin.test.model.AbstractGroupingStageTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.BatchingPackageInserter.Companion.computePackage
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.wasm.test.WasmCoroutineHelpersModuleTransformer
import java.io.File

data class PerTestOutput(val testServices: TestServices, val testModule: TestModule, val klib: BinaryArtifacts.KLib)
data class DependencyPaths(val regular: Set<String>, val friend: Set<String>)

/**
 * Base class for the two Stage-2 grouping facades for K/Wasm:
 *
 *  - [CustomWasmSecondStageFacade.Grouping] — the CLI-based implementation for KLib forward compatibility tests.
 *  - [org.jetbrains.kotlin.wasm.test.converters.WasmInProcessSecondStageFacade.Grouping] — the in-process counterpart.
 *
 * It hosts the shared, target-agnostic batch-analysis helpers (collecting the per-test KLIB outputs,
 * generating the groupedBatch proxy launcher source, aggregating batch-wide language settings and dependencies,
 * and deduplicating the synthetic `helpers.klib` artifacts) so both implementations can reuse the exact
 * same logic instead of one delegating to a throwaway instance of the other.
 */
abstract class AbstractWasmSecondStageGroupingFacade(
    val testServices: TestServices,
) : AbstractGroupingStageTestFacade<GroupingStageInputArtifact, BinaryArtifacts.Wasm>() {

    /**
     * Returns `true` when the batch consists of a test that the grouping engine deliberately isolates
     * (`BatchToken.Isolated` — e.g. friend-module, DCE-size, custom-JS-entry, `IGNORE_BACKEND`, or
     * `IGNORE_KLIB_*_WITH_CUSTOM_SECOND_STAGE` tests).
     *
     * Unlike [isSingleTestBatch], this is `false` for a test that ended up alone merely because it
     * carried a unique batch token (e.g. `BatchToken.Custom` from a one-off `// LANGUAGE:` feature):
     * such a test is still driven through the grouped (`ProxyBatchLauncher`) path, since its `box()`
     * is reached internally via its FQN rather than via a `@JsExport` export.
     */
    protected fun isIsolatedBatch(inputArtifact: GroupingStageInputArtifact): Boolean =
        inputArtifact.nonGroupingStageOutputs.first().testServices
            .shouldIsolateTestInGroupingConfiguration(fileGenerationPhase = true)

    /**
     * Collects the list of (testServices, testModule, KLib artifact) triples produced by the
     * NonGroupingStage that should participate in this Stage-2 batch.
     *
     * Modules whose failures are ignored (e.g. via `IGNORE_BACKEND`) or that do not have a
     * KLib artifact (e.g. because their Stage-1 compilation failed in an expected way) are
     * silently skipped.
     */
    fun collectFilteredOutputs(
        inputArtifact: GroupingStageInputArtifact,
    ): List<PerTestOutput> {
        val filteredOutputs = mutableListOf<PerTestOutput>()
        for (output in inputArtifact.nonGroupingStageOutputs) {
            val services = output.testServices
            for (module in services.moduleStructure.modules) {
                if (!services.codegenSuppressionChecker.failuresInModuleAreIgnored(module)) {
                    val artifact = try {
                        services.artifactsProvider.getArtifact(module, ArtifactKinds.KLib)
                    } catch (_: Exception) {
                        continue
                    }
                    filteredOutputs.add(PerTestOutput(services, module, artifact))
                }
            }
        }
        return filteredOutputs
    }

    /**
     * Generates the `ProxyBatchLauncher.kt` source for the groupedBatch path — one `@Test`-annotated
     * `ProxyLauncher_<hash>` class per test in the batch, plus (on WASI) a
     * `@WasmExport fun startTest()` entry point.
     *
     * Writes the result to `tempDir/ProxyBatchLauncher.kt` and returns the corresponding
     * [TestFile] marked as an additional source so it can be passed to the compiler.
     */
    fun generateGroupedBatchLauncherSource(
        filteredOutputs: List<PerTestOutput>,
        someModule: TestModule,
        tempDir: File,
        isWasiTarget: Boolean,
    ): TestFile {
        val proxyClassNames = mutableListOf<String>()
        val proxyLauncherContent = buildString {
            appendLine(
                """
                import kotlin.test.Test
                import kotlin.test.assertEquals

                """.trimIndent()
            )
            for ([services, _] in filteredOutputs.groupBy { it.testServices }) {
                val additionalPackage = BatchingPackageInserter.computePackage(services.testInfo)
                val fileWithBox = services.moduleStructure.modules.asReversed().firstNotNullOfOrNull { module ->
                    module.files.firstOrNull {
                        val content = services.sourceFileProvider.getContentOfSourceFile(it)
                        MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(content)
                    }
                }
                if (fileWithBox == null) testInfraError("No file with box() function found in any module of the test ${services.testInfo}")

                val originalPackage = fileWithBox.let { MainFunctionForBlackBoxTestsSourceProvider.detectPackage(it) }

                val boxFqName = if (originalPackage != null) "$additionalPackage.$originalPackage.box" else "$additionalPackage.box"

                val uniqueClassName = computeProxyLauncherClassName(services.testInfo)
                proxyClassNames += uniqueClassName
                append(
                    $$"""
                    class $$uniqueClassName {
                        @Test
                        fun runTest() {
                            val result = $$boxFqName()
                            assertEquals("OK", result, "Test failed with: $result")
                        }
                    }
                    """.trimIndent()
                )
            }

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
     * Aggregated dependencies and optins of every test in a groupedBatch, applied
     * uniformly to both the launcher KLIB compilation and the final link.
     *
     * Aggregation rules:
     *  - `regularDependencies` and `friendDependencies` — union across all tests;
     *  - `maxLanguageVersion` — maximum across all tests (so the batch is compiled using the maximum language version);
     *  - `allOptIns` — union of `OPT_IN` directives;
     *  - `allAllowKotlinPackage` — `true` if any test in the batch requested it.
     */
    class BatchSettings(
        val regularDependencies: Set<String>,
        val friendDependencies: Set<String>,
        val maxLanguageVersion: org.jetbrains.kotlin.config.LanguageVersion,
        val allOptIns: List<String>,
        val allAllowKotlinPackage: Boolean,
    )

    /**
     * Shared precomputed stage-2 batch data used by grouped/isolated orchestration paths.
     *
     * Invariants:
     * - [filteredOutputs] preserves the original batching order.
     * - [settings] are aggregated from exactly [filteredOutputs] with unchanged [aggregateBatchSettings] rules.
     * - [perTestKlibPaths] is produced by [deduplicateHelperKlibPaths], so at most one `helpers.klib` path is present.
     * - [cleanedRegularDependencies] is [settings.regularDependencies] minus duplicate helper KLIBs,
     *   keeping helper-KLIB filtering behavior identical to [filterOutDuplicateHelperKlibs].
     */
    class BatchExecutionContext(
        val filteredOutputs: List<PerTestOutput>,
        val settings: BatchSettings,
        val perTestKlibPaths: List<String>,
        val cleanedRegularDependencies: Set<String>,
    )

    protected fun buildBatchExecutionContext(
        inputArtifact: GroupingStageInputArtifact,
        compilationStage: CompilationStage,
    ): BatchExecutionContext {
        val filteredOutputs = collectFilteredOutputs(inputArtifact)
        return buildBatchExecutionContext(filteredOutputs, compilationStage)
    }

    protected fun buildBatchExecutionContext(
        filteredOutputs: List<PerTestOutput>,
        compilationStage: CompilationStage,
    ): BatchExecutionContext {
        val settings = aggregateBatchSettings(filteredOutputs, compilationStage)
        val perTestKlibPaths = deduplicateHelperKlibPaths(filteredOutputs)
        val cleanedRegularDependencies = filterOutDuplicateHelperKlibs(filteredOutputs, settings.regularDependencies, perTestKlibPaths)
        return BatchExecutionContext(
            filteredOutputs = filteredOutputs,
            settings = settings,
            perTestKlibPaths = perTestKlibPaths,
            cleanedRegularDependencies = cleanedRegularDependencies,
        )
    }

    fun aggregateBatchSettings(
        filteredOutputs: List<PerTestOutput>,
        compilationStage: CompilationStage,
    ): BatchSettings {
        val regularDependencies = mutableSetOf<String>()
        val friendDependencies = mutableSetOf<String>()
        for ([services, module, _] in filteredOutputs) {
            module.collectDependencies(services, compilationStage).let { [regular, friend] ->
                regularDependencies += regular
                friendDependencies += friend
            }
        }

        val maxLanguageVersion = filteredOutputs.maxOf { [_, module, _] ->
            module.languageVersionSettings.languageVersion
        }

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
    fun deduplicateHelperKlibPaths(
        filteredOutputs: List<PerTestOutput>,
    ): List<String> =
        filteredOutputs
            .distinctBy { [_, module, artifact] ->
                if (module.name == WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME) {
                    WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME
                } else {
                    artifact.outputFile.absolutePath
                }
            }
            .map { it.klib.outputFile.absolutePath }

    /**
     * Removes from `regularDependencies` any `helpers.klib` path that comes from a per-test
     * output other than the one already kept by [deduplicateHelperKlibPaths].
     *
     * After deduplication the final list of libraries passed to the compiler must have at
     * most one helpers.klib path; the remaining helpers.klib that comes from `someModule`'s
     * `collectDependencies` is also a per-test artifact, but it is the only one we keep.
     */
    fun filterOutDuplicateHelperKlibs(
        filteredOutputs: List<PerTestOutput>,
        regularDependencies: Set<String>,
        perTestKlibPaths: List<String>,
    ): MutableSet<String> {
        val helperKlibsInPerTest = filteredOutputs
            .filter { it.testModule.name == WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME }
            .map { it.klib.outputFile.absolutePath }
            .toSet()
        val keptHelperKlib = perTestKlibPaths.firstOrNull { it in helperKlibsInPerTest }
        return regularDependencies.filterNotTo(mutableSetOf()) { dep ->
            dep in helperKlibsInPerTest && dep != keptHelperKlib
        }
    }

    /**
     * WasmEdge/Wasmtime invoke the `startTest` export as the entry point.
     * Since this is a grouped batch with many tests, `startTest` here drives all ProxyLauncher_*.runTest() methods sequentially.
     * We deliberately
     * - do NOT rely on `box()` (there are many of them in different per-test KLIBs) and
     * - do NOT rely on the synthetic `startUnitTests` symbol: it is generated by the compiler backend and not callable from Kotlin source.
     * Inspired by `wasm/wasm.tests/_additionalFilesForTests/wasiAdditionalFiles/wasiBoxTestRun.kt`
     */
    private fun generateWasiStartTest(proxyClassNames: List<String>): String = buildString {
        appendLine(
            """
            @kotlin.wasm.WasmImport("wasi_snapshot_preview1", "proc_exit")
            private external fun wasiProcExit(code: Int)

            @kotlin.wasm.WasmExport
            fun startTest() {
                try {
            """.trimIndent()
        )
        for (className in proxyClassNames) {
            appendLine("        $className().runTest()")
        }
        appendLine(
            """
                } catch (e: Throwable) {
                    println("Failed with exception!")
                    println(e.message)
                    e.printStackTrace()
                    wasiProcExit(1)
                }
            }
            """.trimIndent()
        )
    }

    /**
     * Copies all `.mjs` and `.js` files from the given modules into [outputDir].
     *
     * Each module's files are read via the corresponding [TestServices.sourceFileProvider] and
     * written verbatim into [outputDir], preserving the original file name.
     */
    protected fun copyJsFilesToOutputDir(
        modules: List<Pair<TestServices, TestModule>>,
        outputDir: File,
    ) {
        val copiedJsByFileName = mutableMapOf<String, Pair<String, String>>()
        for ([services, module] in modules) {
            for (file in module.files) {
                if (file.name.endsWith(".mjs") || file.name.endsWith(".js")) {
                    val content = services.sourceFileProvider.getContentOfSourceFile(file)

                    val existingEntry = copiedJsByFileName[file.name]
                    if (existingEntry != null) {
                        if (existingEntry.first != content) {
                            testInfraError(
                                "Conflicting JS companion file '${file.name}' in grouped batch: " +
                                        "module '${existingEntry.second}' and module '${module.name}' provide different content"
                            )
                        }
                    } else {
                        copiedJsByFileName[file.name] = content to module.name
                    }

                    outputDir.resolve(file.name).writeText(content)
                }
            }
        }
    }

    /*
     * For the module, returns pair: regularDependencies(including stdlib and kotlin-test) and friendDependencies
     * `compilationStage` matters for Klib compatibility tests, where different versions of stdlib and kotlin-test are used at each stage.
     */
    abstract fun TestModule.collectDependencies(
        testServices: TestServices,
        compilationStage: CompilationStage,
    ): DependencyPaths

    override val inputKind: TestArtifactKind<GroupingStageInputArtifact>
        get() = GroupingStageInputArtifact.Kind
    override val outputKind: TestArtifactKind<BinaryArtifacts.Wasm>
        get() = ArtifactKinds.Wasm
}

/**
 * Computes the synthetic per-test `ProxyLauncher` class name used by the WASM grouped test infrastructure.
 * The test infrastructure tracks this name to persistently identify the test from the testInfo
 *
 * The hash is derived from the per-test additional package (see [computePackage]) so that
 * the result is short enough for filesystem paths yet uniquely identifies the test.
 */
internal fun computeProxyLauncherClassName(testInfo: KotlinTestInfo): String =
    "ProxyLauncher_${computePackage(testInfo).hashCode().toUInt().toString(36)}"
