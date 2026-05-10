/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser
import org.jetbrains.kotlin.js.test.converters.augmentWithModuleName
import org.jetbrains.kotlin.js.test.utils.getModeOutputFilePath
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.JsIrArtifact
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

class JsSourceMapValidator(testServices: TestServices) : AbstractJsArtifactsCollector(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            validateSourceMaps()
        }
    }

    private fun validateSourceMaps() {
        val globalDirectives = testServices.moduleStructure.allDirectives
        if (JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE in globalDirectives) return

        val allMapFilesPerTranslationMode = collectAllTheGeneratedMapFiles()

        for ((mode, mapFiles) in allMapFilesPerTranslationMode) {
            for (mapFilePath in mapFiles) {
                val mapFile = File(mapFilePath)
                // TODO: Discuss if we accept empty source map file, since technically it's invalid .map file
                val sourceMapFileContent = mapFile.readText().ifEmpty { continue }
                val result = ECMA426BasedSourceMapParser.parseSourceMap(sourceMapFileContent, baseUrl = mapFile.parent)
                if (result is ECMA426BasedSourceMapParser.ParsingResult.Failure) {
                    throw AssertionError(
                        """
                        Failed to parse source map for file: ${mapFile.absolutePath}, in translation mode: $mode
                        The failure is: $result
                    """.trimIndent()
                    )
                }
            }
        }
    }

    private fun collectAllTheGeneratedMapFiles(): Map<TranslationMode, List<String>> {
        val result = mutableMapOf<TranslationMode, List<String>>()
        val (module, compilerResult) = modulesToArtifact.entries
            .mapNotNull { (m, c) -> (c as? JsIrArtifact)?.let { m to c.compilerResult } }
            .single()

        compilerResult.entries.forEach { (mode, outputs) ->
            val outputFile = getModeOutputFilePath(testServices, module, mode)

            result[mode] = outputs.dependencies.mapTo(mutableListOf("${outputFile}.map")) {
                "${outputFile.augmentWithModuleName(it.artifactConfiguration.moduleName)}.map"
            }
        }

        return result
    }
}
