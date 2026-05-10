/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.config

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.moduleName
import java.io.File

/**
 * @property moduleName The name of the compilation module.
 * @property outputDirectory The destination for the generated files
 * @property outputName The base name for generated files.
 * @property granularity The granularity of JS files generation.
 * @property production Whether to optimize the generated code.
 * @property minimizedMemberNames Whether to generate minimized names for non-exported class and interface members.
 */
data class WebArtifactConfiguration(
    val moduleKind: ModuleKind,
    val moduleName: String,
    val outputDirectory: File,
    val outputName: String,
    val granularity: JsGenerationGranularity,
    val tsCompilationStrategy: TsCompilationStrategy,
    val production: Boolean,
    val minimizedMemberNames: Boolean,
) {
    /**
     * Returns the location of the generated JS file according to this configuration.
     *
     * @param outputName The name of the generated JS file without extension.
     */
    fun outputJsFile(outputName: String = this.outputName): File =
        outputDirectory.resolve(outputName + moduleKind.jsExtension)

    /**
     * Returns the location of the generated source map file according to this configuration.
     *
     * @param outputName The name of the generated file without extension.
     */
    fun outputSourceMapFile(outputName: String = this.outputName): File =
        outputDirectory.resolve(outputName + moduleKind.jsExtension + ".map")

    /**
     * Returns the location of the generated TypeScript definition file according to this configuration.
     *
     * @param outputName The name of the generated file without extension.
     */
    fun outputDtsFile(outputName: String = this.outputName): File =
        outputDirectory.resolve(outputName + moduleKind.dtsExtension)

    companion object {
        fun fromFlags(
            configuration: CompilerConfiguration,
            isPerFile: Boolean,
            isPerModule: Boolean,
            generateDts: Boolean,
        ): WebArtifactConfiguration? {
            return WebArtifactConfiguration(
                moduleKind = configuration.moduleKind ?: return null,
                moduleName = configuration.moduleName ?: return null,
                outputDirectory = configuration.outputDir ?: return null,
                outputName = configuration.outputName ?: return null,
                granularity = when {
                    isPerFile -> JsGenerationGranularity.PER_FILE
                    isPerModule -> JsGenerationGranularity.PER_MODULE
                    else -> JsGenerationGranularity.WHOLE_PROGRAM
                },
                tsCompilationStrategy = when {
                    !generateDts -> TsCompilationStrategy.NONE
                    isPerFile -> TsCompilationStrategy.EACH_FILE
                    else -> TsCompilationStrategy.MERGED
                },
                production = configuration.dce,
                minimizedMemberNames = configuration.minimizedMemberNames,
            )
        }
    }
}

