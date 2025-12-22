/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.js.test.handlers.JsTypeScriptCompilationHandler.Companion.getMainTsFile
import org.jetbrains.kotlin.js.test.handlers.TypeScriptCompilation
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

/**
 * Invokes the TypeScript compiler to compile `.ts` files in the test data, if there are any.
 */
class WasmTypeScriptCompilationHandler(testServices: TestServices) : AbstractWasmArtifactsCollector(testServices) {

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val moduleStructure = testServices.moduleStructure
        val allDirectives = moduleStructure.allDirectives
        if (WasmEnvironmentConfigurationDirectives.CHECK_TYPESCRIPT_DECLARATIONS !in allDirectives) return

        val mainTsFile = getMainTsFile(testServices, ".ts") ?: return

        val outputDir = testServices.getWasmTestOutputDirectoryForMode("dev")

        val outputFile = compiledTypeScriptOutput(testServices, "dev")

        TypeScriptCompilation(
            testServices,
            modulesToArtifact,
            { artifact ->
                File(outputDir, artifact.compilerResult.baseFileName + ".d.mts").also { tsFile ->
                    artifact.compilerResult.dts?.let {
                        tsFile.parentFile.mkdirs()
                        tsFile.writeText(it)
                    }
                }
            },
            mainTsFile,
            outputFile,
            File(allDirectives[WasmEnvironmentConfigurationDirectives.PATH_TO_NODE_DIR].first()),
        ).processAfterAllModules()

        // Copy the compiled TypeScript artifact into directories corresponding to each translation mode
        for (mode in listOf("dce", "dev")) {
            if (mode == "dev") {
                // The actual compiled artifact is in the directory for the FULL_DEV mode.
                continue
            }
            outputFile.copyTo(compiledTypeScriptOutput(testServices, mode), overwrite = true)
        }
    }

    companion object {
        fun compiledTypeScriptOutput(testServices: TestServices, mode: String): File {
            val originalTestFile = testServices.moduleStructure.originalTestDataFiles.first()

            return testServices.getWasmTestOutputDirectoryForMode(mode)
                .resolve(originalTestFile.nameWithoutExtension + "__main${JavaScript.DOT_EXTENSION}")
        }
    }
}