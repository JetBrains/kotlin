/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.js.test.handlers.JsTypeScriptCompilationHandler.Companion.getMainTsFile
import org.jetbrains.kotlin.js.test.handlers.TypeScriptCompilation
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.config.wasmIncludedModuleOnly
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

        val includedModuleOnly = moduleStructure.modules.any {
            testServices.compilerConfigurationProvider.getCompilerConfiguration(it).wasmIncludedModuleOnly
        }
        val defaultMode = if (!includedModuleOnly) "dev" else ""

        val outputDir = testServices.getWasmTestOutputDirectoryForMode(defaultMode)
        val outputJsFile = compiledTypeScriptOutput(testServices, defaultMode, JavaScript.DOT_EXTENSION)

        TypeScriptCompilation(
            testServices,
            modulesToArtifact,
            { artifact ->
                require(artifact is BinaryArtifacts.Wasm.CompilationSets)
                File(outputDir, artifact.compilation.compilerResult.baseFileName + ".d.mts").also { tsFile ->
                    artifact.compilation.compilerResult.dts?.let {
                        tsFile.parentFile.mkdirs()
                        tsFile.writeText(it)
                    }
                }
            },
            mainTsFile,
            outputJsFile,
            File(allDirectives[WasmEnvironmentConfigurationDirectives.PATH_TO_NODE_DIR].first()),
        ).processAfterAllModules()

        // Copy the compiled TypeScript artifact into directories corresponding to each translation mode
        outputJsFile.copyTo(compiledTypeScriptOutput(testServices, defaultMode), overwrite = true)
        if (!includedModuleOnly) {
            outputJsFile.copyTo(compiledTypeScriptOutput(testServices, "dce"), overwrite = true)
        }
    }

    companion object {
        private fun compiledTypeScriptOutput(testServices: TestServices, mode: String, fileDotExtension: String): File {
            val originalTestFile = testServices.moduleStructure.originalTestDataFiles.first()

            return testServices.getWasmTestOutputDirectoryForMode(mode)
                .resolve(originalTestFile.nameWithoutExtension + "__main$fileDotExtension")
        }

        fun compiledTypeScriptOutput(testServices: TestServices, mode: String): File =
            compiledTypeScriptOutput(testServices, mode, JavaScript.DOT_MODULE_EXTENSION)
    }
}