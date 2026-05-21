/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.providers

import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
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
 *
 *   @kotlin.wasm.WasmExport
 *   fun hasTestFailures(): Boolean = kotlin.test.hasTestFailures()
 *   ```
 *
 * These are essential for the Stage 2 friend-dependency isolated path in
 * `WasmJsCompilerSecondStageFacade.Grouping.transform()`. In that path the per-test main KLIB is
 * used as the `-Xinclude` main module (so that `-Xfriend-modules` correctly declares friendship
 * with sibling KLIBs of the same multi-module test), which means the pipeline runs only
 * `WasmConfigurationPhase + WasmBackendPipelinePhase` — no frontend/Fir2Ir — and free-arg source
 * files (like the synthetic `ProxyBatchLauncher.kt`) are silently ignored. The only way to get a
 * `@Test`-annotated entry point into the lowered IR of the included main module is to bake it
 * into the per-test KLIB at Stage 1, which is what this provider does.
 *
 * For the other Stage 2 paths (non-isolated grouped batch, and isolated batches without friend
 * dependencies), Option B applies: a fresh `ProxyBatchLauncher.kt` is compiled into a small
 * `launcher.klib` that becomes the `-Xinclude` main module, and the per-test `Launcher_<hash>`
 * classes in the per-test KLIBs are simply ignored (their KLIBs are passed as ordinary
 * `-libraries`, and non-included modules only get `DeserializationStrategy.EXPLICITLY_EXPORTED`).
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

    override fun generateLauncherContent(boxFqName: String, file: TestFile, expectedResult: String): String {
        val launcherClassName = computeLauncherClassName(file)
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
        val launcherFiles = super.produceAdditionalFiles(globalDirectives, module, testModuleStructure)
        if (launcherFiles.isEmpty()) return emptyList()

        val launcherFile = launcherFiles.single()
        val file = launcherFile.originalFile
        file.appendText(
            """

                @kotlin.wasm.WasmExport
                fun hasTestFailures(): Boolean {
                    return kotlin.test.hasTestFailures()
                }
            """.trimIndent()
        )
        return launcherFiles
    }
}
