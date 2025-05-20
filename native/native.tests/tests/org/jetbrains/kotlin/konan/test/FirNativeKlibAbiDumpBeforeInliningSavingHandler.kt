/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.test.backend.handlers.AbstractKlibAbiDumpBeforeInliningSavingHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class FirNativeKlibAbiDumpBeforeInliningSavingHandler(
    testServices: TestServices,
) : AbstractKlibAbiDumpBeforeInliningSavingHandler(testServices) {
    override fun serializeModule(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticReporterFactory.createReporter(configuration.messageCollector)
        val outputFile = getAbiCheckKlibArtifactFile(module.name)

        FirNativeKlibSerializerFacade(testServices).serializeBare(module, inputArtifact, outputFile, configuration, diagnosticReporter)

        return BinaryArtifacts.KLib(outputFile, diagnosticReporter)
    }
}
