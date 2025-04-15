/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.backend.js.JsGenerationGranularity
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.js.config.friendLibraries
import org.jetbrains.kotlin.js.config.incrementalDataProvider
import org.jetbrains.kotlin.js.config.libraries
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
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
    override fun createConfiguration(
        moduleName: String,
        moduleKind: ModuleKind,
        languageFeatures: List<String>,
        allLibraries: List<String>,
        friendLibraries: List<String>,
        includedLibrary: String?,
    ): CompilerConfiguration {
        val config = super.createConfiguration(
            moduleName = moduleName,
            moduleKind = moduleKind,
            languageFeatures = languageFeatures,
            allLibraries = allLibraries,
            friendLibraries = friendLibraries,
            includedLibrary = includedLibrary,
        )
        config.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.WARNING))
        return config
    }
}

abstract class IrAbstractInvalidationTest(
    targetBackend: TargetBackend,
    granularity: JsGenerationGranularity,
    workingDirPath: String
) : JsAbstractInvalidationTest(targetBackend, granularity, workingDirPath) {
    override fun buildKlib(
        configuration: CompilerConfiguration,
        moduleName: String,
        sourceDir: File,
        outputKlibFile: File
    ) {
        val projectJs = environment.project

        val sourceFiles = configuration.addSourcesFromDir(sourceDir)

        val klibs = loadWebKlibsInTestPipeline(
            configuration,
            libraryPaths = configuration.libraries,
            friendPaths = configuration.friendLibraries,
            platformChecker = KlibPlatformChecker.JS
        )

        val sourceModule = prepareAnalyzedSourceModule(
            project = projectJs,
            files = sourceFiles,
            configuration = configuration,
            klibs = klibs,
            analyzer = AnalyzerWithCompilerReport(configuration)
        )

        val moduleSourceFiles = (sourceModule.mainModule as MainModule.SourceFiles).files
        val icData = sourceModule.compilerConfiguration.incrementalDataProvider?.getSerializedData(moduleSourceFiles) ?: emptyList()
        val (moduleFragment, irPluginContext) = generateIrForKlibSerialization(
            project = environment.project,
            files = moduleSourceFiles,
            configuration = configuration,
            analysisResult = sourceModule.jsFrontEndResult.jsAnalysisResult,
            klibs = sourceModule.klibs,
            icData = icData,
            irFactory = IrFactoryImpl,
        ) {
            sourceModule.getModuleDescriptor(it)
        }
        generateKLib(
            sourceModule,
            outputKlibFile.canonicalPath,
            nopack = false,
            jsOutputName = moduleName,
            icData = icData,
            moduleFragment = moduleFragment,
            irBuiltIns = irPluginContext.irBuiltIns,
            diagnosticReporter = DiagnosticReporterFactory.createPendingReporter(configuration.messageCollector),
        )
    }
}
