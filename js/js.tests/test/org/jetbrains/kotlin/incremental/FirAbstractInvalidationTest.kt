/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.js.klib.compileModuleToAnalyzedFir
import org.jetbrains.kotlin.cli.js.klib.serializeFirKlib
import org.jetbrains.kotlin.cli.js.klib.transformFirToIr
import org.jetbrains.kotlin.codegen.ProjectInfo
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.test.TargetBackend
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset

abstract class AbstractJsFirInvalidationTest : FirAbstractInvalidationTest(TargetBackend.JS_IR, "incrementalOut/invalidationFir")

abstract class FirAbstractInvalidationTest(
    targetBackend: TargetBackend,
    workingDirPath: String
) : AbstractInvalidationTest(targetBackend, workingDirPath) {
    private val mutedTests = setOf("constVals", "enum", "jsCodeWithConstString")

    override fun isIgnoredTest(projectInfo: ProjectInfo): Boolean {
        return super.isIgnoredTest(projectInfo) || projectInfo.name in mutedTests
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
        val sourceFiles = sourceDir.filteredKtFiles().map { environment.createPsiFile(it) }
        val moduleStructure = ModulesStructure(
            project = environment.project,
            mainModule = MainModule.SourceFiles(sourceFiles),
            compilerConfiguration = configuration,
            dependencies = libraries,
            friendDependenciesPaths = friendLibraries
        )

        val outputs = compileModuleToAnalyzedFir(
            moduleStructure = moduleStructure,
            ktFiles = sourceFiles,
            libraries = libraries,
            friendLibraries = friendLibraries,
            messageCollector = messageCollector,
            diagnosticsReporter = diagnosticsReporter
        )

        if (outputs != null) {
            val fir2IrActualizedResult = transformFirToIr(moduleStructure, outputs, diagnosticsReporter)

            serializeFirKlib(
                moduleStructure = moduleStructure,
                firOutputs = outputs,
                fir2IrActualizedResult = fir2IrActualizedResult,
                outputKlibPath = outputKlibFile.absolutePath,
                messageCollector = messageCollector,
                diagnosticsReporter = diagnosticsReporter,
                jsOutputName = moduleName
            )
        }

        if (messageCollector.hasErrors()) {
            val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
            throw AssertionError("The following errors occurred compiling test:\n$messages")
        }
    }
}
