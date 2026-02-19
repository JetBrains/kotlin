/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.backend.handlers.WasmBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.isKtFile
import org.jetbrains.kotlin.wasm.test.utils.DirectiveTestUtils

class WasmIrHandler(testServices: TestServices) : WasmBinaryArtifactHandler(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        require(info is BinaryArtifacts.Wasm.CompilationSets)
        val ktFiles = module.files.filter { it.isKtFile }.associate { it.originalFile to it.originalContent }

        ktFiles.forEach {
            DirectiveTestUtils.processDirectives(
                info.compilation.compiledModule,
                it.value,
                testServices.defaultsProvider.targetBackend!!,
            )
        }
    }
}
