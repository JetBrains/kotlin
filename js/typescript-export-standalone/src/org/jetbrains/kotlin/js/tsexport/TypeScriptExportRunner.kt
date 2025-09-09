/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.klib.reader.KaModules
import org.jetbrains.kotlin.analysis.api.klib.reader.createKaModulesForStandaloneAnalysis
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedDeclaration
import org.jetbrains.kotlin.ir.backend.js.tsexport.toTypeScript
import org.jetbrains.kotlin.ir.backend.js.tsexport.toTypeScriptFragment
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.config.TsCompilationStrategy
import org.jetbrains.kotlin.js.config.WebArtifactConfiguration
import org.jetbrains.kotlin.library.metadata.KlibInputModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import java.io.File

public data class TypeScriptExportConfig(
    public val targetPlatform: TargetPlatform,
    public val artifactConfiguration: WebArtifactConfiguration
)

public typealias InputModule = KlibInputModule<TypeScriptModuleConfig>

/**
 * @property outputName The equivalent of passing the `-Xir-per-module-output-name` compiler flag or setting
 *   the `outputModuleName` Gradle option.
 */
public data class TypeScriptModuleConfig(
    public val outputName: String?,
)

public fun runTypeScriptExport(klibs: List<KlibInputModule<TypeScriptModuleConfig>>, config: TypeScriptExportConfig): List<File> {
    if (config.artifactConfiguration.tsCompilationStrategy == TsCompilationStrategy.NONE) {
        return emptyList()
    }

    val kaModules = createKaModulesForStandaloneAnalysis(klibs, config.targetPlatform)
    val exportModel = klibs.map {
        generateExportModelForModule(it, kaModules, config)
    }
    val artifacts = TsArtifactProducer.generateArtifacts(exportModel, config.artifactConfiguration.granularity)
    config.artifactConfiguration.outputDirectory.normalizedAbsoluteFile.mkdirs()
    return when (config.artifactConfiguration.tsCompilationStrategy) {
        TsCompilationStrategy.NONE -> emptyList()
        TsCompilationStrategy.MERGED -> {
            writeMergedTsFile(artifacts, config.artifactConfiguration, config.artifactConfiguration.moduleName)
        }
        TsCompilationStrategy.EACH_FILE -> {
            when (config.artifactConfiguration.granularity) {
                JsGenerationGranularity.WHOLE_PROGRAM -> {
                    val jsFileName = config.artifactConfiguration.outputJsFile().name
                    writeMergedTsFile(artifacts, config.artifactConfiguration, jsFileName)
                }
                JsGenerationGranularity.PER_FILE, JsGenerationGranularity.PER_MODULE -> artifacts.map { artifact ->
                    val jsFileName = config.artifactConfiguration.outputJsFile(artifact.externalModuleName).name
                    val dtsFile = config.artifactConfiguration.outputDtsFile(artifact.externalModuleName).normalizedAbsoluteFile
                    val tsDefinitions = listOf(artifact.exportModel.toTypeScriptFragment(config.artifactConfiguration.moduleKind))
                        .toTypeScript(jsFileName, config.artifactConfiguration.moduleKind)
                    dtsFile.writeText(tsDefinitions)
                    dtsFile
                }
            }
        }
    }
}

private fun writeMergedTsFile(
    artifacts: List<TypeScriptModuleArtifact>,
    config: WebArtifactConfiguration,
    moduleName: String,
): List<File> {
    val dtsFile = config.outputDtsFile().normalizedAbsoluteFile
    val mergedTsDefinitions = artifacts
        .map { it.exportModel.toTypeScriptFragment(config.moduleKind) }
        .toTypeScript(moduleName, config.moduleKind)
    dtsFile.writeText(mergedTsDefinitions)

    return listOf(dtsFile)
}

private val File.normalizedAbsoluteFile
    get() = absoluteFile.normalize()

internal data class ProcessedModule(
    val library: KaLibraryModule,
    val declarationsGroupedByFile: Map<FileArtifactKey, List<ExportedDeclaration>>,
    val jsOutputName: String,
)

/**
 * @property fileName If the source file was annotated with `@JsFileName`, the argument of the annotation, otherwise the name
 *   of the source file without extension.
 */
internal data class FileArtifactKey(
    val packageFqName: FqName,
    val fileName: String,
)

private fun generateExportModelForModule(
    klib: KlibInputModule<TypeScriptModuleConfig>,
    kaModules: KaModules<TypeScriptModuleConfig>,
    config: TypeScriptExportConfig,
): ProcessedModule = analyze(kaModules.useSiteModule) {
    val library = kaModules.mainModules.single { it.libraryName == klib.name }
    ExportModelGenerator(config.artifactConfiguration.moduleKind).generateExport(library, klib.config)
}
