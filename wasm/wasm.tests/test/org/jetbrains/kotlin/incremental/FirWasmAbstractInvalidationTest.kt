/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.collectSources
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.js.klib.compileModulesToAnalyzedFirWithLightTree
import org.jetbrains.kotlin.cli.js.klib.serializeFirKlib
import org.jetbrains.kotlin.cli.js.klib.transformFirToIr
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsGenerationGranularity
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TargetBackend
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset

abstract class AbstractFirWasmInvalidationPerFileTest :
    FirWasmAbstractInvalidationTest(TargetBackend.WASM, JsGenerationGranularity.PER_FILE, "incrementalOut/invalidationFir/perFile")

abstract class FirWasmAbstractInvalidationTest(
    targetBackend: TargetBackend,
    granularity: JsGenerationGranularity,
    workingDirPath: String
) : WasmAbstractInvalidationTest(targetBackend, granularity, workingDirPath) {
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
        dependencies: Collection<File>,
        friends: Collection<File>,
        outputKlibFile: File
    ) {
        val outputStream = ByteArrayOutputStream()
        val messageCollector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, true)
        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

        val libraries = dependencies.map { it.absolutePath }
        val friendLibraries = friends.map { it.absolutePath }
        val sourceFiles = configuration.addSourcesFromDir(sourceDir)
        val moduleStructure = ModulesStructure(
            project = environment.project,
            mainModule = MainModule.SourceFiles(sourceFiles),
            compilerConfiguration = configuration,
            dependencies = libraries,
            friendDependenciesPaths = friendLibraries
        )

        val groupedSources = collectSources(configuration, environment.project, messageCollector)
        val analyzedOutput = compileModulesToAnalyzedFirWithLightTree(
            moduleStructure = moduleStructure,
            groupedSources = groupedSources,
            ktSourceFiles = groupedSources.commonSources + groupedSources.platformSources,
            libraries = libraries,
            friendLibraries = friendLibraries,
            diagnosticsReporter = diagnosticsReporter,
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
            useWasmPlatform = false,
            wasmTarget = WasmTarget.JS,
        )

        if (messageCollector.hasErrors()) {
            val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
            throw AssertionError("The following errors occurred serializing test klib:\n$messages")
        }
    }
}
