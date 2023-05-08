/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.ir.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

abstract class AbstractJsIrInvalidationTest : IrAbstractInvalidationTest(
    targetBackend = TargetBackend.JS_IR,
    workingDirPath = "incrementalOut/invalidation"
)

abstract class AbstractJsIrES6InvalidationTest : IrAbstractInvalidationTest(
    targetBackend = TargetBackend.JS_IR_ES6,
    workingDirPath = "incrementalOut/invalidationES6"
)

abstract class AbstractJsIrInvalidationWithPLTest : IrAbstractInvalidationTest(
    targetBackend = TargetBackend.JS_IR,
    workingDirPath = "incrementalOut/invalidationWithPL"
) {
    override fun createConfiguration(moduleName: String, language: List<String>, moduleKind: ModuleKind): CompilerConfiguration {
        val config = super.createConfiguration(moduleName, language, moduleKind)
        config.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.WARNING))
        return config
    }
}

abstract class IrAbstractInvalidationTest(
    targetBackend: TargetBackend,
    workingDirPath: String
) : AbstractInvalidationTest(targetBackend, workingDirPath) {
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
        val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()
        val (moduleFragment, _) = generateIrForKlibSerialization(
            environment.project,
            moduleSourceFiles,
            configuration,
            sourceModule.jsFrontEndResult.jsAnalysisResult,
            sortDependencies(sourceModule.moduleDependencies),
            icData,
            expectDescriptorToSymbol,
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
            expectDescriptorToSymbol = expectDescriptorToSymbol,
            moduleFragment = moduleFragment
        ) { file ->
            metadataSerializer.serializeScope(file, sourceModule.jsFrontEndResult.bindingContext, moduleFragment.descriptor)
        }
    }
}
