/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.preprocessors

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.impl.shouldIsolateTestInGroupingConfiguration
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * Marks the `box()` function as `@JsExport` during the (first-stage) CLI compilation, so the
 * second-stage CLI compiler exports it and the box-export test runner can invoke `jsModule.box()`.
 * In the in-process test pipeline the same is achieved by `WasmLoweringFacade.transform()`, which
 * sets `wasmTestBoxFunctionToExport` on the compiler configuration.
 *
 * The annotation is added only for isolated tests.
 */
class WasmJsExportBoxPreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    private val topLevelBoxRegex = Regex("(^|\n|public\\s+)fun box\\(.*\\)")

    override fun process(file: TestFile, content: String): String {
        return topLevelBoxRegex.replace(content) { "\n@JsExport " + it.value }
    }

    @TestInfrastructureInternals
    override fun processModule(module: TestModule, filesContent: MutableMap<TestFile, String>) {
        // Only isolated tests (ones that the grouping engine runs alone via the box-export model) need `box()` exported.
        // We must NOT add `@JsExport` for tests that may share a multi-test batch: each per-test KLIB would then export a symbol named `box`,
        //   and linking several of them produces clashing `box` exports in the generated `index.mjs` (error: "Identifier 'box' has already been declared").
        // For such grouped batches, `box()` is reached internally via its FQN from the synthesized `ProxyBatchLauncher`, not via an export.
        if (!testServices.shouldIsolateTestInGroupingConfiguration(fileGenerationPhase = true)) return

        // Tests that explicitly drive the unit-test runner likewise do not use the box-export model and are skipped here.
        if (RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives) return

        super.processModule(module, filesContent)
    }
}
