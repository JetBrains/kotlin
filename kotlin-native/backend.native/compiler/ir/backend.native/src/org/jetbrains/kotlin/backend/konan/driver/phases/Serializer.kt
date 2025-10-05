/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.konan.PsiToIrOutput
import org.jetbrains.kotlin.backend.konan.driver.LightPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault

internal data class SerializerInput(
        val moduleDescriptor: ModuleDescriptor,
        val psiToIrOutput: PsiToIrOutput?,
        val produceHeaderKlib: Boolean,
)

typealias SerializerOutput = org.jetbrains.kotlin.backend.common.serialization.SerializerOutput<KonanLibrary>

internal val SerializerPhase = createSimpleNamedCompilerPhase(
        "Serializer",
        outputIfNotEnabled = { _, _, _, _ -> SerializerOutput(null, null, emptyList()) }
) { context: LightPhaseContext, input: SerializerInput ->
    val config = context.config
    val messageCollector = config.configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    val serializedIr = input.psiToIrOutput?.let {
        val ir = it.irModule
        KonanIrModuleSerializer(
            settings = IrSerializationSettings(
                configuration = config.configuration,
                publicAbiOnly = input.produceHeaderKlib,
            ),
            KtDiagnosticReporterWithImplicitIrBasedContext(
                DiagnosticReporterFactory.createPendingReporter(messageCollector),
                config.languageVersionSettings
            ),
            input.psiToIrOutput.irBuiltIns,
        ).serializedIrModule(ir)
    }

    val serializer = KlibMetadataMonolithicSerializer(
            languageVersionSettings = config.configuration.languageVersionSettings,
            metadataVersion = config.configuration.klibMetadataVersionOrDefault(),
            project = config.project,
            exportKDoc = context.shouldExportKDoc(),
            skipExpects = !config.metadataKlib,
            includeOnlyModuleContent = true,
            produceHeaderKlib = input.produceHeaderKlib
    )
    val serializedMetadata = serializer.serializeModule(input.moduleDescriptor)
    val neededLibraries = config.librariesWithDependencies()
    SerializerOutput(serializedMetadata, serializedIr, neededLibraries)
}

internal fun <T : LightPhaseContext> PhaseEngine<T>.runSerializer(
    moduleDescriptor: ModuleDescriptor,
    psiToIrResult: PsiToIrOutput?,
    produceHeaderKlib: Boolean = false,
): SerializerOutput {
    val input = SerializerInput(moduleDescriptor, psiToIrResult, produceHeaderKlib)
    return this.runPhase(SerializerPhase, input)
}