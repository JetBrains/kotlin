/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.engine.ExternalTool
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

/**
 * Invokes the TypeScript compiler to compile `.ts` files in the test data, if there are any.
 */
class TypeScriptCompilationHandler(testServices: TestServices) : AbstractJsArtifactsCollector(testServices) {
    companion object {
        private val nodeJs by lazy {
            ExternalTool(System.getProperty("javascript.engine.path.NodeJs"))
        }

        fun compiledTypeScriptOutput(testServices: TestServices, mode: TranslationMode): File {
            val originalTestFile = testServices.moduleStructure.originalTestDataFiles.first()
            val mainModule = JsEnvironmentConfigurator.getMainModule(testServices)
            val moduleKind = JsEnvironmentConfigurator.getModuleKind(testServices, mainModule)
            return JsEnvironmentConfigurator
                .getJsArtifactsOutputDir(testServices, mode)
                .resolve(originalTestFile.nameWithoutExtension + "__main${moduleKind.jsExtension}")
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val moduleStructure = testServices.moduleStructure
        val allDirectives = moduleStructure.allDirectives
        if (JsEnvironmentConfigurationDirectives.GENERATE_DTS !in allDirectives) return

        val dtsFiles = modulesToArtifact.values.mapNotNull { it.dtsFile }

        val originalTestFile = testServices.moduleStructure.originalTestDataFiles.first()
        val mainModule = JsEnvironmentConfigurator.getMainModule(testServices)
        val moduleKind = JsEnvironmentConfigurator.getModuleKind(testServices, mainModule)
        val mainTsFile = originalTestFile
            .parentFile
            .resolve(originalTestFile.nameWithoutExtension + "__main${moduleKind.tsExtension}")
            .takeIf { it.exists() }
            ?: return

        val outputFile = compiledTypeScriptOutput(testServices, TranslationMode.FULL_DEV)

        val customizedTarget = allDirectives[JsEnvironmentConfigurationDirectives.TSC_TARGET].firstOrNull()
        val defaultTarget = if (JsEnvironmentConfigurationDirectives.ES6_MODE in allDirectives) "es6" else "es5"
        val target = customizedTarget
            ?: defaultTarget

        val libs = listOf(
            customizedTarget ?: "es2015", // We generate Promise usages even in ES5 mode, so we should use at least the ES2015 definitions
            "dom",
        )

        val moduleOption = allDirectives[JsEnvironmentConfigurationDirectives.TSC_MODULE].firstOrNull()?.let {
            listOf("--module", it)
        }.orEmpty()

        val rootOutputDir = File(allDirectives[JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR].first())
        val tscPath = rootOutputDir.resolve("node_modules/typescript/bin/tsc").absolutePath

        nodeJs.run(
            tscPath,
            *dtsFiles.map { it.path }.toTypedArray(),
            mainTsFile.path,
            "--target", target,
            "--lib", libs.joinToString(","),
            *moduleOption.toTypedArray(),
            "--strict", "true",
            "--newline", "lf",
            "--outdir", outputFile.parent, // Using --outdir instead of --outfile because the latter is not compatible with ES modules
            workingDirectory = File(allDirectives[JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR].first())
        )

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
}