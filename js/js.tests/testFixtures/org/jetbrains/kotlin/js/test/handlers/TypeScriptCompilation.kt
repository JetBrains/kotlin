/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.engine.ExternalTool
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

/**
 * Invokes the TypeScript compiler to compile `.ts` files in the test data, if there are any.
 */
class TypeScriptCompilation<A : ResultingArtifact.Binary<A>>(
    private val testServices: TestServices,
    private val modulesToArtifact: Map<TestModule, A>,
    private val dtsFileByArtifact: (A) -> File?,
    private val mainTsFile: File,
    private val outputFile: File,
    private val rootOutputDir: File,
) {
    companion object {
        private val nodeJs by lazy {
            ExternalTool(System.getProperty("javascript.engine.path.NodeJs"))
        }
    }

    fun processAfterAllModules() {
        val moduleStructure = testServices.moduleStructure
        val allDirectives = moduleStructure.allDirectives

        val dtsFiles = modulesToArtifact.values.mapNotNull(dtsFileByArtifact)

        val customizedTarget = allDirectives[JsEnvironmentConfigurationDirectives.TSC_TARGET].firstOrNull()
        val defaultTarget = if (JsEnvironmentConfigurationDirectives.ES6_MODE in allDirectives) "es6" else "es5"
        val target = customizedTarget
            ?: defaultTarget

        val customizedLibs = allDirectives[JsEnvironmentConfigurationDirectives.TSC_LIB]
            // We generate Promise usages even in ES5 mode, so we should use at least the ES2015 definitions
            .ifEmpty { listOf(customizedTarget ?: "es2015") }

        val libs = customizedLibs + "dom"

        val moduleOption = allDirectives[JsEnvironmentConfigurationDirectives.TSC_MODULE].firstOrNull()?.let {
            listOf("--module", it)
        }.orEmpty()

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
            workingDirectory = rootOutputDir
        )
    }
}