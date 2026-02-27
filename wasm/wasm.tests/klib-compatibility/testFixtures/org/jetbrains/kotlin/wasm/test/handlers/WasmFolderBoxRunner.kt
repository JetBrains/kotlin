/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.services.TestServices

class WasmFolderBoxRunner(
    testServices: TestServices
) : WasmBoxRunnerBase(testServices, executeWithV8Only = true) {

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmFolder()
        }
    }

    private fun runWasmFolder() {
        val artifacts = modulesToArtifact.values.single() as BinaryArtifacts.Wasm.Folder
        val throwables = saveAdditionalFilesAndRun(artifacts.folder, "dev", emptyList(), mutableSetOf())
        if (throwables.isNotEmpty())
            throw throwables.first()
    }
}
