/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.blackbox

import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.jetbrains.kotlin.test.impl.shouldIsolateTestInGroupingConfiguration
import org.jetbrains.kotlin.test.model.AbstractGroupingStageTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider.Companion.detectPackage
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider.Companion.findFileWithBoxMethod
import org.jetbrains.kotlin.test.services.sourceProviders.SourceContentView
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.test.testInfraError
import java.io.File

/**
 * The base of the second stage of the two-stage K/JS test pipeline, which links the KLIBs produced by the non-grouping
 * stage into an executable. It decides how a batch is linked and prepares what a grouped batch is linked from, while
 * an implementation chooses the compiler that links: the current one in-process, or a released one via CLI.
 *
 * An isolated test is linked exactly as in the one-stage pipeline: its main KLIB is the included one.
 *
 * A grouped batch is linked from a synthesized launcher: one `ProxyLauncher_<encoded-package>` class per test, calling
 * its `box()` by the fully qualified name, plus the driver that runs them all and prints a structured result line per
 * test (see [GroupedTestsResultProtocol]). Only the launcher is compiled at this stage, into a small KLIB that becomes
 * the included one, while the KLIBs of the tests are passed as ordinary libraries.
 *
 * @see org.jetbrains.kotlin.js.test.handlers.JsGroupingStageBoxRunner for the side that runs the executable.
 */
abstract class AbstractJsSecondStageGroupingFacade(
    val testServices: TestServices,
) : AbstractGroupingStageTestFacade<GroupingStageInputArtifact, BinaryArtifacts.Js>() {
    final override val inputKind: TestArtifactKind<GroupingStageInputArtifact>
        get() = GroupingStageInputArtifact.Kind
    final override val outputKind: TestArtifactKind<BinaryArtifacts.Js>
        get() = ArtifactKinds.Js

    /**
     * What a grouped batch is linked from.
     *
     * @property launcherModule The synthetic module of the launcher. It has the name and the settings of the main
     *   module of the first test, so they share the compiler configuration.
     * @property launcherSource The source of the launcher, which is yet to be compiled into the included KLIB.
     * @property perTestKlibs The KLIBs of all the tests of the batch.
     * @property workingDir Where the launcher can be compiled to.
     */
    protected class GroupedBatch(
        val outputs: List<NonGroupingStageOutput>,
        val launcherModule: TestModule,
        val launcherSource: File,
        val perTestKlibs: List<String>,
        val workingDir: File,
    ) {
        /** The services of the test whose settings the whole batch is linked with. */
        val services: TestServices
            get() = outputs.first().testServices
    }

    protected abstract fun linkIsolated(output: NonGroupingStageOutput): BinaryArtifacts.Js?

    protected abstract fun linkGroupedBatch(batch: GroupedBatch): BinaryArtifacts.Js?

    final override fun transform(inputArtifact: GroupingStageInputArtifact): BinaryArtifacts.Js? {
        val outputs = inputArtifact.nonGroupingStageOutputs
        // The decision must be the one `BatchingPackageInserter` took on the first stage: a test that is not
        // isolated got its packages renamed, even if it ended up alone in its batch.
        val isIsolated = outputs.first().testServices.shouldIsolateTestInGroupingConfiguration(fileGenerationPhase = true)
        return if (isIsolated) {
            checkTestInfrastructure(outputs.size == 1) { "An isolated test is batched with ${outputs.size - 1} other test(s)" }
            linkIsolated(outputs.single())
        } else {
            linkGroupedBatch(prepareGroupedBatch(outputs))
        }
    }

    private fun prepareGroupedBatch(outputs: List<NonGroupingStageOutput>): GroupedBatch {
        val someModule = JsEnvironmentConfigurator.getMainModule(outputs.first().testServices)
        val workingDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("grouped-batch-launcher")
        val launcherFile = generateLauncherSource(outputs, someModule, workingDir)
        return GroupedBatch(
            outputs = outputs,
            launcherModule = someModule.copy(files = listOf(launcherFile)),
            launcherSource = launcherFile.originalFile,
            perTestKlibs = collectPerTestKlibs(outputs),
            workingDir = workingDir,
        )
    }

    /**
     * All the tests of a batch carry the same helpers module (see [JsTestHelpersModuleTransformer] and
     * [JsGroupingTestIsolator]), so only one of its KLIBs is linked: the others would declare the very same symbols.
     */
    private fun collectPerTestKlibs(outputs: List<NonGroupingStageOutput>): List<String> {
        var hasHelpersKlib = false
        return buildList {
            for (output in outputs) {
                val services = output.testServices
                for (module in services.moduleStructure.modules) {
                    val klib = services.artifactsProvider.getArtifactSafe(module, ArtifactKinds.KLib) ?: continue
                    if (module.name == JsTestHelpersModuleTransformer.HELPERS_MODULE_NAME) {
                        if (hasHelpersKlib) continue
                        hasHelpersKlib = true
                    }
                    add(klib.outputFile.canonicalPath)
                }
            }
        }
    }

    private fun generateLauncherSource(outputs: List<NonGroupingStageOutput>, someModule: TestModule, workingDir: File): TestFile {
        val proxyClassNames = mutableListOf<String>()
        val content = buildString {
            // The launcher merely forwards to `box()` of every test, whatever opt-in the test has required for it.
            appendLine("@file:Suppress(\"OPT_IN_USAGE\", \"OPT_IN_USAGE_ERROR\")")
            appendLine()
            for (output in outputs) {
                val services = output.testServices
                val sourceFileProvider = services.sourceFileProvider
                // The launcher calls the `box()` compiled into the KLIB of the test, so both the discovery and the package
                // must come from the transformed sources, which already have the package `BatchingPackageInserter` set.
                val fileWithBox = findFileWithBoxMethod(
                    services.moduleStructure.modules.asReversed(),
                    SourceContentView.TRANSFORMED,
                    sourceFileProvider,
                ) ?: testInfraError("No file with box() function found in any module of the test ${services.testInfo}")
                val boxFqName = detectPackage(fileWithBox, SourceContentView.TRANSFORMED, sourceFileProvider)
                    ?.let { "$it.box" }
                    ?: "box"

                val className = computeJsProxyLauncherClassName(services.testInfo)
                proxyClassNames += className
                appendLine("class $className {")
                appendLine("    fun runTest() {")
                appendLine("        val result = $boxFqName()")
                appendLine("        if (result != \"OK\") throw AssertionError(\"Test failed with: \$result. Expected <OK>, actual <\$result>.\")")
                appendLine("    }")
                appendLine("}")
                appendLine()
            }
            append(
                GroupedTestsResultProtocol.generateResultCollectingRunnerSource(
                    proxyClassNames,
                    JsGroupedTestsExportedEntryPointGenerator,
                )
            )
        }
        val launcherFile = workingDir.resolve("ProxyBatchLauncher.kt")
        launcherFile.writeText(content)
        return TestFile(
            relativePath = launcherFile.name,
            originalContent = content,
            originalFile = launcherFile,
            startLineNumberInOriginalFile = 0,
            isAdditional = true,
            directives = someModule.files.first().directives,
        )
    }
}
