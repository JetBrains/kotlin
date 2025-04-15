/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.cli.common.collectSources
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.perfManager
import org.jetbrains.kotlin.cli.pipeline.web.WebFir2IrPipelinePhase.transformFirToIr
import org.jetbrains.kotlin.cli.pipeline.web.WebFrontendPipelinePhase.compileModulesToAnalyzedFirWithLightTree
import org.jetbrains.kotlin.cli.pipeline.web.WebKlibSerializationPipelinePhase.serializeFirKlib
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.loadWebKlibsInTestPipeline
import org.jetbrains.kotlin.js.config.friendLibraries
import org.jetbrains.kotlin.js.config.libraries
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.wasm.config.wasmTarget
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset

abstract class AbstractFirWasmInvalidationTest :
    FirWasmAbstractInvalidationTest(TargetBackend.WASM, "incrementalOut/invalidationFir")

abstract class AbstractFirWasmInvalidationWithPLTest :
    FirWasmAbstractInvalidationWithPLTest("incrementalOut/invalidationFirWithPL")

abstract class FirWasmAbstractInvalidationWithPLTest(workingDirPath: String) :
    FirWasmAbstractInvalidationTest(
        TargetBackend.WASM,
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

abstract class FirWasmAbstractInvalidationTest(
    targetBackend: TargetBackend,
    workingDirPath: String
) : WasmAbstractInvalidationTest(targetBackend, workingDirPath) {
    private fun getFirInfoFile(defaultInfoFile: File): File {
        val firInfoFileName = "${defaultInfoFile.nameWithoutExtension}.fir.${defaultInfoFile.extension}"
        val firInfoFile = defaultInfoFile.parentFile.resolve(firInfoFileName)
        return firInfoFile.takeIf { it.exists() } ?: defaultInfoFile
    }

    override fun getModuleInfoFile(directory: File): File {
        return getFirInfoFile(super.getModuleInfoFile(directory))
    }

    override fun getProjectInfoFile(directory: File): File {
        return getFirInfoFile(super.getProjectInfoFile(directory))
    }

    override fun buildKlib(
        configuration: CompilerConfiguration,
        moduleName: String,
        sourceDir: File,
        outputKlibFile: File
    ) {
        val outputStream = ByteArrayOutputStream()
        val messageCollector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, true)
        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)

        val libraries = configuration.libraries
        val friendLibraries = configuration.friendLibraries
        val sourceFiles = configuration.addSourcesFromDir(sourceDir)

        val klibs = loadWebKlibsInTestPipeline(
            configuration,
            libraryPaths = libraries,
            friendPaths = friendLibraries,
            platformChecker = KlibPlatformChecker.Wasm(configuration.wasmTarget.alias)
        )

        val moduleStructure = ModulesStructure(
            project = environment.project,
            mainModule = MainModule.SourceFiles(sourceFiles),
            compilerConfiguration = configuration,
            klibs = klibs,
        )

        val groupedSources = collectSources(configuration, environment.project, messageCollector)
        val analyzedOutput = compileModulesToAnalyzedFirWithLightTree(
            moduleStructure = moduleStructure,
            groupedSources = groupedSources,
            ktSourceFiles = groupedSources.commonSources + groupedSources.platformSources,
            libraries = libraries,
            friendLibraries = friendLibraries,
            diagnosticsReporter = diagnosticsReporter,
            performanceManager = configuration.perfManager,
            incrementalDataProvider = null,
            lookupTracker = null,
            useWasmPlatform = true,
        )

        val fir2IrActualizedResult = transformFirToIr(moduleStructure, analyzedOutput.output, diagnosticsReporter)

        if (analyzedOutput.reportCompilationErrors(moduleStructure, diagnosticsReporter, messageCollector)) {
            val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
            throw AssertionError("The following errors occurred compiling test:\n$messages")
        }

        serializeFirKlib(
            moduleStructure = moduleStructure,
            firOutputs = analyzedOutput.output,
            fir2IrActualizedResult = fir2IrActualizedResult,
            outputKlibPath = outputKlibFile.absolutePath,
            nopack = false,
            messageCollector = messageCollector,
            diagnosticsReporter = diagnosticsReporter,
            jsOutputName = moduleName,
            useWasmPlatform = true,
            wasmTarget = WasmTarget.JS,
        )

        if (messageCollector.hasErrors()) {
            val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
            throw AssertionError("The following errors occurred serializing test klib:\n$messages")
        }
    }
}
