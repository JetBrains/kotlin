/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.backend.handlers.WasmBinaryArtifactHandler
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull

class WasmDtsHandler(testServices: TestServices) : WasmBinaryArtifactHandler(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        val globalDirectives = testServices.moduleStructure.allDirectives
        if (WasmEnvironmentConfigurationDirectives.CHECK_TYPESCRIPT_DECLARATIONS !in globalDirectives) return

        val referenceDtsFile = module.files.first().originalFile.withReplacedExtensionOrNull(".kt", ".d.ts")
            ?: error("Can't find reference .d.ts file")

        val generatedDts = info.compilerResult.dts
            ?: error("Can't find generated .d.ts file")

        KotlinTestUtils.assertEqualsToFile(referenceDtsFile, generatedDts)
    }
}
