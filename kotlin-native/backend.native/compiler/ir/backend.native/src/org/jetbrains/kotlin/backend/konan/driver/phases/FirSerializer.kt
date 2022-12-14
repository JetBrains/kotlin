/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.getSerializedData
import org.jetbrains.kotlin.ir.backend.js.incrementalDataProvider
import org.jetbrains.kotlin.ir.backend.js.serializeModuleIntoKlib
import org.jetbrains.kotlin.ir.backend.js.serializeSingleFirFile
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.library.KotlinAbiVersion
import java.io.File

data class FirSerializerOutput(
        val klibPath: File
)

internal val FirSerializerPhase = createSimpleNamedCompilerPhase<FirFrontendContext, Fir2IrOutput, FirSerializerOutput>(
        "FirSerializer", "Fir serializer",
        outputIfNotEnabled = { _, _, _, _ -> FirSerializerOutput(File("")) }
) { context: FirFrontendContext, input: Fir2IrOutput ->
    // Serialize KLib in the same way as K2/JS does.

    val configuration = context.environment.configuration
    val sourceFiles = input.firFiles.mapNotNull { it.sourceFile }
    val firFilesBySourceFile = input.firFiles.associateBy { it.sourceFile }
    val metadataVersion =
            configuration.get(CommonConfigurationKeys.METADATA_VERSION)
                    ?: GenerationState.LANGUAGE_TO_METADATA_VERSION.getValue(configuration.languageVersionSettings.languageVersion)

    val outputFiles = OutputFiles(context.config.outputPath, context.config.target, context.config.produce)
    val nopack = configuration.getBoolean(KonanConfigKeys.NOPACK)
    val outputKlibPath = outputFiles.klibOutputFileName(!nopack)
    val icData = configuration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList()
    val resolvedLibraries = context.config.resolvedLibraries.getFullResolvedList()

    serializeModuleIntoKlib(
            moduleName = context.config.moduleId,
            configuration = configuration,
            messageLogger = configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None,
            sourceFiles,
            klibPath = outputKlibPath,
            resolvedLibraries.map { it.library },
            input.fir2irResult.irModuleFragment,
            expectDescriptorToSymbol = mutableMapOf(), // TODO: expect -> actual mapping
            cleanFiles = icData,
            nopack = true,
            perFile = false,
            containsErrorCode = false, // at this point, `messageCollector.hasErrors()` and `diagnosticsReporter.hasErrors` are both false
            abiVersion = KotlinAbiVersion.CURRENT, // TODO get from test file data
            jsOutputName = null
    ) { file ->
        val firFile = firFilesBySourceFile[file] ?: error("cannot find FIR file by source file ${file.name} (${file.path})")
        serializeSingleFirFile(firFile, input.session, input.scopeSession, metadataVersion)
    }

    FirSerializerOutput(File(outputKlibPath))
}

internal fun <T : FirFrontendContext> PhaseEngine<T>.runFirSerializer(
        fir2irOutput: Fir2IrOutput
): FirSerializerOutput {
    return this.runPhase(FirSerializerPhase, fir2irOutput)
}