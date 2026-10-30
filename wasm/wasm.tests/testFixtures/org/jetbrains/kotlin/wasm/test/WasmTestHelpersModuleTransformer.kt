/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.test.klib.HelperModuleExtractingTransformer
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider.Companion.detectPackage
import org.jetbrains.kotlin.test.services.sourceProviders.SourceContentView

/**
 * The counterpart of [WasmCoroutineHelpersModuleTransformer] for the grouped suites, which moves into the `helpers` module
 * all the additional sources that `BatchingPackageInserter` does not rename:
 * - the coroutine helpers of `CoroutineHelpersSourceFilesProvider` (package `helpers`),
 * - the common files of [WasmAdditionalSourceProvider] (package `kotlin`).
 *
 * Every test gets the same copy of these sources. Left inside the test modules, they would be declared once per test in a
 * grouped batch, and the linker binds each of their signatures to the declaration of one test only. In the closed-world
 * multimodule mode, where every test module becomes a Wasm module of its own, the other tests then call a function of a
 * module they don't import. Extracted, a batch links a single helpers KLIB, see `deduplicateHelperKlibPaths`.
 */
object WasmTestHelpersModuleTransformer : HelperModuleExtractingTransformer() {
    // Shared with the other transformer, so that `isHelpersModule` tells the helpers module made by either of them.
    const val HELPERS_MODULE_NAME: String = WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME

    private val HELPER_PACKAGES = setOf("helpers", "kotlin")

    override val helpersModuleName: String
        get() = HELPERS_MODULE_NAME

    /** Every module of a test may get its own copy of the same helper file: the helpers module needs a single one. */
    override fun selectHelpersModuleFiles(helperFiles: List<TestFile>): List<TestFile> = helperFiles.distinctBy { it.relativePath }

    override fun isHelperFile(file: TestFile): Boolean =
        file.isAdditional && file.name.endsWith(".kt") && detectPackage(file, SourceContentView.ORIGINAL) in HELPER_PACKAGES
}

/**
 * Whether this is the helpers module of a test: the one of [WasmTestHelpersModuleTransformer], or the one of
 * [WasmCoroutineHelpersModuleTransformer] in a grouped suite which doesn't extract the common files, as both share its name.
 */
internal val TestModule.isHelpersModule: Boolean
    get() = name == WasmTestHelpersModuleTransformer.HELPERS_MODULE_NAME
