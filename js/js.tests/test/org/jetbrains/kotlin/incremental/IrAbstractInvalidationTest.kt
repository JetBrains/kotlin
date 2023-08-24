/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsGenerationGranularity
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.ir.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

abstract class AbstractJsIrInvalidationPerFileTest : IrAbstractInvalidationTest(
    targetBackend = TargetBackend.JS_IR,
    granularity = JsGenerationGranularity.PER_FILE,
    workingDirPath = "incrementalOut/invalidation/perFile"
)

abstract class AbstractJsIrInvalidationPerModuleTest : IrAbstractInvalidationTest(
    targetBackend = TargetBackend.JS_IR,
    granularity = JsGenerationGranularity.PER_MODULE,
    workingDirPath = "incrementalOut/invalidation/perModule"
)

abstract class AbstractJsIrES6InvalidationPerFileTest : IrAbstractInvalidationTest(
    targetBackend = TargetBackend.JS_IR_ES6,
    granularity = JsGenerationGranularity.PER_FILE,
    workingDirPath = "incrementalOut/invalidationES6/perFile"
)

abstract class AbstractJsIrES6InvalidationPerModuleTest : IrAbstractInvalidationTest(
    targetBackend = TargetBackend.JS_IR_ES6,
    granularity = JsGenerationGranularity.PER_MODULE,
    workingDirPath = "incrementalOut/invalidationES6/perModule"
)

abstract class AbstractJsIrInvalidationPerFileWithPLTest : AbstractJsIrInvalidationWithPLTest(
    granularity = JsGenerationGranularity.PER_FILE,
    workingDirPath = "incrementalOut/invalidationWithPL/perFile"
)

abstract class AbstractJsIrInvalidationPerModuleWithPLTest : AbstractJsIrInvalidationWithPLTest(
    granularity = JsGenerationGranularity.PER_MODULE,
    workingDirPath = "incrementalOut/invalidationWithPL/perModule"
)

abstract class AbstractJsIrInvalidationWithPLTest(granularity: JsGenerationGranularity, workingDirPath: String) : IrAbstractInvalidationTest(
        TargetBackend.JS_IR,
        granularity,
        workingDirPath
    ) {
    override fun createConfiguration(moduleName: String, language: List<String>, moduleKind: ModuleKind): CompilerConfiguration {
        val config = super.createConfiguration(moduleName, language, moduleKind)
        config.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.WARNING))
        return config
    }
}

abstract class IrAbstractInvalidationTest(
    targetBackend: TargetBackend,
    granularity: JsGenerationGranularity,
    workingDirPath: String
) : AbstractInvalidationTest(targetBackend, granularity, workingDirPath) {
    override fun buildKlib(
        configuration: CompilerConfiguration,
        moduleName: String,
        sourceDir: File,
        dependencies: Collection<File>,
        friends: Collection<File>,
        outputKlibFile: File
    ) {
        val projectJs = environment.project

        val sourceFiles = sourceDir.filteredKtFiles().map { environment.createPsiFile(it) }

        val sourceModule = prepareAnalyzedSourceModule(
            project = projectJs,
            files = sourceFiles,
            configuration = configuration,
            dependencies = dependencies.map { it.canonicalPath },
            friendDependencies = friends.map { it.canonicalPath },
            analyzer = AnalyzerWithCompilerReport(configuration)
        )

        val moduleSourceFiles = (sourceModule.mainModule as MainModule.SourceFiles).files
        val icData = sourceModule.compilerConfiguration.incrementalDataProvider?.getSerializedData(moduleSourceFiles) ?: emptyList()
        val (moduleFragment, _) = generateIrForKlibSerialization(
            environment.project,
            moduleSourceFiles,
            configuration,
            sourceModule.jsFrontEndResult.jsAnalysisResult,
            sortDependencies(sourceModule.moduleDependencies),
            icData,
            IrFactoryImpl,
            verifySignatures = true
        ) {
            sourceModule.getModuleDescriptor(it)
        }
        val metadataSerializer =
            KlibMetadataIncrementalSerializer(configuration, sourceModule.project, sourceModule.jsFrontEndResult.hasErrors)

        generateKLib(
            sourceModule,
            outputKlibFile.canonicalPath,
            nopack = false,
            jsOutputName = moduleName,
            icData = icData,
            moduleFragment = moduleFragment
        ) { file ->
            metadataSerializer.serializeScope(file, sourceModule.jsFrontEndResult.bindingContext, moduleFragment.descriptor)
        }
    }
}
