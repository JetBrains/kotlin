/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.isSingleTestBatch
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.GroupingStageHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.WasmCompilationSetsBinaryArtifact
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.isKtFile
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.wasm.test.blackbox.WasmGroupingTestIsolator
import org.jetbrains.kotlin.wasm.test.utils.DirectiveTestUtils

/**
 * Checks the in-text Wasm IR directives of a test (see [DirectiveTestUtils]) in the grouping stage, as [WasmIrHandler] does
 * in the single-stage suites. The IR of a batch doesn't tell its tests apart, and their names are escaped there, so
 * [WasmGroupingTestIsolator] isolates the tests having such directives, and only a single-test batch is checked, against
 * the IR of its main compilation.
 */
class WasmGroupingStageIrHandler(testServices: TestServices) : GroupingStageHandler<BinaryArtifacts.Wasm>(
    testServices,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false,
) {
    override val artifactKind: TestArtifactKind<BinaryArtifacts.Wasm>
        get() = ArtifactKinds.Wasm

    override fun processArtifact(artifact: BinaryArtifacts.Wasm) {
        if (!testServices.isSingleTestBatch()) return
        val input = testServices.groupingStageInputs.first()
        val services = input.testServices
        val sourcesWithDirectives = services.moduleStructure.modules
            .flatMap { it.files }
            .filter { it.isKtFile && !it.isAdditional }
            .map { it.originalContent }
            .filter { DirectiveTestUtils.hasDirectives(it) }
        if (sourcesWithDirectives.isEmpty()) return

        input.catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this) }) {
            check(artifact is WasmCompilationSetsBinaryArtifact) { "Unexpected artifact type: ${artifact::class}" }
            val linkedModule = artifact.compilation.compilerResult.linkedModule
                ?: testInfraError("The Wasm IR of test ${services.testInfo} was not kept")
            sourcesWithDirectives.forEach {
                DirectiveTestUtils.processDirectives(linkedModule, it, services.defaultsProvider.targetBackend!!)
            }
        }
    }
}
