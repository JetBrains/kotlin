/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.impl.shouldIsolateTestInGroupingConfiguration
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.model.AbstractGroupingStageTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.wasm.test.WasmCoroutineHelpersModuleTransformer
import java.io.File

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
     * Global invariant: a batch that contains exactly one test must always be compiled andvexecuted as a standalone box-export test
     * (like `FirWasmJsCodegenBoxTestGenerated/WasmBoxRunner`), *regardless of why* it ended up alone in the batch
     * — whether because it was isolated (`BatchToken.Isolated`) or
     * — merely because it carried a unique batch token (e.g. `BatchToken.Custom` derived from a one-off `// LANGUAGE:` feature).
     * Such a batch is therefore routed to the "isolated" (box-export) compilation path instead of the groupedBatch path:
     * the synthesized `@Test`/`ProxyLauncher` machinery and the corresponding TeamCity-marker-based sanity check
     * make sense only for a *real* multi-test batch driven by the JUnit/unit-test runner.
     * A single-test batch run via the unit-test runner would otherwise produce no TeamCity `testSuiteFinished` markers on the WASI VMs
     * (WasmEdge/Wasmtime invoke the `startTest` export directly), failing the sanity check on empty output.
     */
    protected fun isSingleTestBatch(inputArtifact: GroupingStageInputArtifact): Boolean =
        inputArtifact.nonGroupingStageOutputs.map { it.testServices.testInfo }.distinct().size == 1

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
     * Generates the `ProxyBatchLauncher.kt` source for the groupedBatch path — one `@Test`-annotated
     * `ProxyLauncher_<hash>` class per test in the batch, plus (on WASI) a
     * `@WasmExport fun startTest()` entry point.
     *
     * Writes the result to `tempDir/ProxyBatchLauncher.kt` and returns the corresponding
     * [TestFile] marked as an additional source so it can be passed to the compiler.
     */
    fun generateGroupedBatchLauncherSource(
        filteredOutputs: List<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>,
        someModule: TestModule,
        tempDir: File,
        isWasiTarget: Boolean,
    ): TestFile {
        val proxyClassNames = mutableListOf<String>()
        val proxyLauncherContent = buildString {
            append("import kotlin.test.Test\n")
            append("import kotlin.test.assertEquals\n\n")
            for ([services, _] in filteredOutputs.groupBy { it.first }) {
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
     * Aggregated dependencies and language settings of every test in a groupedBatch, applied
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
    class BatchSettings(
        val regularDependencies: MutableSet<String>,
        val friendDependencies: MutableSet<String>,
        val maxLanguageVersion: org.jetbrains.kotlin.config.LanguageVersion,
        val allLanguageFeatures: List<String>,
        val allOptIns: List<String>,
        val allAllowKotlinPackage: Boolean,
    )

    fun aggregateBatchSettings(
        filteredOutputs: List<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>,
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
    fun deduplicateHelperKlibPaths(
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
    fun filterOutDuplicateHelperKlibs(
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
        append("    } catch (e: Throwable) {\n")
        append("        println(\"Failed with exception!\")\n")
        append("        println(e.message)\n")
        append("        println(e.printStackTrace())\n")
        append("        wasiProcExit(1)\n")
        append("    }\n")
        append("}\n")
    }

    /*
     * For the module, returns pair: regularDependencies(including stdlib and kotlin-test) and friendDependencies
     * `compilationStage` matters for Klib compatibility tests, where different versions of stdlib and kotlin-test are used at each stage.
     */
    abstract fun TestModule.collectDependencies(
        testServices: TestServices,
        compilationStage: CompilationStage,
    ): Pair<Set<String>, Set<String>>

    override val inputKind: TestArtifactKind<GroupingStageInputArtifact>
        get() = GroupingStageInputArtifact.Kind
    override val outputKind: TestArtifactKind<BinaryArtifacts.Wasm>
        get() = ArtifactKinds.Wasm
}
