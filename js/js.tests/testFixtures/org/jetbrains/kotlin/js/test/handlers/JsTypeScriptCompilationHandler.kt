/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.config.TsCompilationStrategy
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.TS_COMPILATION_STRATEGY
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

/**
 * Invokes the TypeScript compiler to compile `.ts` files in the test data, if there are any.
 */
class JsTypeScriptCompilationHandler(testServices: TestServices) : AbstractJsArtifactsCollector(testServices) {

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val allDirectives = testServices.moduleStructure.allDirectives
        val tsCompilationStrategy = allDirectives[TS_COMPILATION_STRATEGY].lastOrNull() ?: return
        if (tsCompilationStrategy == TsCompilationStrategy.NONE) return

        val outputFile = compiledTypeScriptOutput(testServices, TranslationMode.FULL_DEV)

        val mainModule = JsEnvironmentConfigurator.getMainModule(testServices)
        val moduleKind = JsEnvironmentConfigurator.getModuleKind(testServices, mainModule)
        val mainTsFile = getMainTsFile(testServices, moduleKind.tsExtension) ?: return

        val dtsFiles = JsEnvironmentConfigurator
            .getJsArtifactsOutputDir(testServices, TranslationMode.FULL_DEV)
            .listFiles { it.name.endsWith(".d.ts") || it.name.endsWith(".d.mts") }!!
            .toList()

        TypeScriptCompilation(
            testServices,
            dtsFiles,
            mainTsFile,
            outputFile,
            File(allDirectives[JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR].first()),
        ).processAfterAllModules()

        // Copy the compiled TypeScript artifact into directories corresponding to each translation mode,
        // so that tests that only support e.g. the per-module mode don't fail because of missing artifact.
        for (mode in JsEnvironmentConfigurator.getTranslationModesForTest(testServices, mainModule)) {
            if (mode == TranslationMode.FULL_DEV) {
                // The actual compiled artifact is in the directory for the FULL_DEV mode.
                continue
            }
            outputFile.copyTo(compiledTypeScriptOutput(testServices, mode), overwrite = true)
        }
    }

    companion object {
        fun compiledTypeScriptOutput(testServices: TestServices, mode: TranslationMode): File {
            val originalTestFile = testServices.moduleStructure.originalTestDataFiles.first()
            val mainModule = JsEnvironmentConfigurator.getMainModule(testServices)
            val moduleKind = JsEnvironmentConfigurator.getModuleKind(testServices, mainModule)
            return JsEnvironmentConfigurator
                .getJsArtifactsOutputDir(testServices, mode)
                .resolve(originalTestFile.nameWithoutExtension + "__main${moduleKind.jsExtension}")
        }

        fun getMainTsFile(testServices: TestServices, extension: String): File? {
            val originalTestFile = testServices.moduleStructure.originalTestDataFiles.first()
            return originalTestFile
                .parentFile
                .resolve(originalTestFile.nameWithoutExtension + "__main$extension")
                .takeIf { it.exists() }
        }
    }
}
