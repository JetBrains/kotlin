/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.test.klib.HelperModuleExtractingTransformer
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider.Companion.detectPackage
import org.jetbrains.kotlin.test.services.sourceProviders.SourceContentView

/**
 * Moves the additional sources that `BatchingPackageInserter` does not rename into a dedicated module named
 * [HELPERS_MODULE_NAME], which every other module of the test then depends on:
 * - the coroutine helpers of `CoroutineHelpersSourceFilesProvider` (package `helpers`),
 * - the common files of `JsAdditionalSourceProvider` (package `kotlin`).
 *
 * Every test gets the same copy of these sources. Left inside the test modules, they would be declared once per test
 * in a grouped batch and fail the link with `IrSymbol is already bound`. Extracted, a batch links a single helpers KLIB.
 */
object JsTestHelpersModuleTransformer : HelperModuleExtractingTransformer() {
    const val HELPERS_MODULE_NAME: String = "jsTestHelpers"

    private val HELPER_PACKAGES = setOf("helpers", "kotlin")

    override val helpersModuleName: String
        get() = HELPERS_MODULE_NAME

    /** Every module of a test may get its own copy of the same helper file: the helpers module needs a single one. */
    override fun selectHelpersModuleFiles(helperFiles: List<TestFile>): List<TestFile> = helperFiles.distinctBy { it.relativePath }

    override fun isHelperFile(file: TestFile): Boolean =
        file.isAdditional && file.name.endsWith(".kt") && detectPackage(file, SourceContentView.ORIGINAL) in HELPER_PACKAGES
}
