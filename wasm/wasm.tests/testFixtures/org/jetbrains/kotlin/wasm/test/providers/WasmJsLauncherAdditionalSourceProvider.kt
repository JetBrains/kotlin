/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.providers

import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.impl.shouldIsolateTestInGroupingConfiguration
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.sourceProviders.AbstractLauncherAdditionalSourceProvider

/**
 * Provides per-test "launcher" sources that are added to every test module containing a `box()`
 * function during the NonGroupingStage (Stage 1) compilation:
 *
 *   ```kotlin
 *   class Launcher_<hash> {
 *       @kotlin.test.Test
 *       fun runTest() {
 *           val result = <boxFqName>()
 *           kotlin.test.assertEquals("OK", result, "Test failed with: $result")
 *       }
 *   }
 *   ```
 *
 * These are essential for the isolatedWithBox path of `WasmCompilerSecondStageFacade.Grouping.transform()`
 * (isolated batch with `box()`, possibly with friend module dependencies). In isolatedWithBox the per-test main KLIB
 * is used as the `-Xinclude` main module (so that `-Xfriend-modules` correctly declares friendship
 * with sibling KLIBs of the same multi-module test), which means the pipeline runs only
 * `WasmConfigurationPhase + WasmBackendPipelinePhase` — no frontend/Fir2Ir — and free-arg source
 * files (like the synthetic `ProxyBatchLauncher.kt`) are silently ignored. The only way to get a
 * `@Test`-annotated entry point into the lowered IR of the included main module is to bake it
 * into the per-test KLIB at Stage 1, which is what this provider does.
 *
 * For the other Stage 2 paths (groupedBatch — non-isolated grouped batch), a fresh `ProxyBatchLauncher.kt` is compiled into a small
 * `launcher.klib` that becomes the `-Xinclude` main module, and the per-test `Launcher_<hash>`
 * classes in the per-test KLIBs are simply ignored (their KLIBs are passed as ordinary
 * `-libraries`, and non-included modules only get `DeserializationStrategy.EXPLICITLY_EXPORTED`).
 * In isolatedWithoutBox (isolated batch without `box()`) there is no `ProxyBatchLauncher` at all and the
 * test is driven by a custom JS entry point.
 */
class WasmJsLauncherAdditionalSourceProvider(testServices: TestServices) : AbstractLauncherAdditionalSourceProvider(testServices) {
    companion object {
        /**
         * Computes the synthetic per-test `Launcher` class name used by the WASM (non-grouped)
         * test infrastructure for tests that are executed in isolation.
         *
         * The same name is referenced in two places that must stay in sync:
         *   - [generateLauncherContent] generates the launcher class declaration
         *     `class Launcher_<hash> { @Test fun runTest() = <fqn>.box() }` for the test file;
         *   - `AbstractWasmFolderBoxRunnerGroupingStage.computeExpectedSuiteNames()` consumes it
         *     as one of the expected `##teamcity[testSuiteFinished name='Launcher_<hash>'`
         *     markers when verifying that an isolated test from a grouped batch actually ran.
         */
        fun computeLauncherClassName(file: TestFile): String =
            "Launcher_${file.relativePath.hashCode().toUInt().toString(36)}"
    }

    override fun generateLauncherContent(boxFqName: String, expectedResult: String): String =
        error("Use overload with testFile")

    override fun generateLauncherContent(boxFqName: String, testFile: TestFile, expectedResult: String): String {
        val launcherClassName = computeLauncherClassName(testFile)
        return """
            class $launcherClassName {
                @kotlin.test.Test
                fun runTest() {
                    val result = $boxFqName()
                    kotlin.test.assertEquals("$expectedResult", result, "Test failed with: ${'$'}result")
                }
            }
        """.trimIndent()
    }

    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        // Skip groupedBatch (non-isolated grouped batch). In that path the Stage 2 facade
        // (`WasmCompilerSecondStageFacade.Grouping.transform()`) synthesizes a fresh
        // `ProxyBatchLauncher.kt` and compiles it into a small `launcher.klib` that is used as the
        // `-Xinclude` main module, while the per-test KLIBs are passed as ordinary `-libraries`.
        // The per-test `Launcher_<hash>` class baked into the per-test KLIB by this provider is
        // therefore dead — it is never visited by `GenerateWasmTests` because that lowering only
        // runs over the main module's IR.
        //
        // The provider is still required for the other Stage 2 paths:
        //   * isolatedWithBox — isolated batches (with or without friend deps): the per-test KLIB ends
        //     up as the included main module;
        //   * isolatedWithoutBox — non-grouped (legacy) execution: no `ProxyBatchLauncher.kt` exists at all.
        if (testServices.isGroupedNonIsolatedBatch(globalDirectives, testModuleStructure)) return emptyList()

        // An isolated single-test batch is executed via the standalone box-export
        // model (like `FirWasmJsCodegenBoxTestGenerated` / `WasmBoxRunner`): the runner calls
        // `jsModule.box()` and checks `"OK"` instead of driving the unit-test runner. Such a test must
        // therefore NOT link the `@Test` launcher / `kotlin-test` framework, otherwise `GenerateWasmTests`
        // would set `testFunctionDeclarator`, keep `runRootSuites`, and inflate the DCE/optimized output
        // size far beyond the standalone `WASM_*_EXPECTED_OUTPUT_SIZE` expectations. Instead, `box()` is
        // exported (`@JsExport`) so the runner can invoke it directly (see `WasmJsExportBoxPreprocessor`
        // for the CLI path and `WasmLoweringFacade` for the in-process path).
        // Tests that explicitly exercise the unit-test runner (`// RUN_UNIT_TESTS`) still need the launcher.
        if (RUN_UNIT_TESTS !in testModuleStructure.allDirectives &&
            testServices.shouldIsolateTestInGroupingConfiguration(testModuleStructure, fileGenerationPhase = true)
        ) {
            return emptyList()
        }

        return super.produceAdditionalFiles(globalDirectives, module, testModuleStructure)
    }
}
