/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.SerializedMetadata

internal data class SerializerInput(
        val moduleDescriptor: ModuleDescriptor,
        val psiToIrOutput: PsiToIrOutput.ForKlib?,
        val produceHeaderKlib: Boolean,
)

data class SerializerOutput(
        val serializedMetadata: SerializedMetadata?,
        val serializedIr: SerializedIrModule?,
        val dataFlowGraph: ByteArray?,
        val neededLibraries: List<KonanLibrary>
)

internal val SerializerPhase = createSimpleNamedCompilerPhase<PhaseContext, SerializerInput, SerializerOutput>(
        "Serializer", "IR serializer",
        outputIfNotEnabled = { _, _, _, _ -> SerializerOutput(null, null, null, emptyList()) }
) { context: PhaseContext, input: SerializerInput ->
    val config = context.config
    val messageLogger = config.configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None
    val relativePathBase = config.configuration.get(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES) ?: emptyList()
    val normalizeAbsolutePaths = config.configuration.get(CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH) ?: false

    val serializedIr = input.psiToIrOutput?.let {
        val ir = it.irModule
        KonanIrModuleSerializer(
                messageLogger, ir.irBuiltins,
                compatibilityMode = CompatibilityMode.CURRENT,
                normalizeAbsolutePaths = normalizeAbsolutePaths,
                sourceBaseDirs = relativePathBase,
                languageVersionSettings = config.languageVersionSettings,
                bodiesOnlyForInlines = input.produceHeaderKlib,
                skipPrivateApi = input.produceHeaderKlib,
        ).serializedIrModule(ir)
    }

    val serializer = KlibMetadataMonolithicSerializer(
            languageVersionSettings = config.configuration.languageVersionSettings,
            metadataVersion = config.configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!,
            project = config.project,
            exportKDoc = context.shouldExportKDoc(),
            skipExpects = !config.metadataKlib,
            includeOnlyModuleContent = true,
            produceHeaderKlib = input.produceHeaderKlib
    )
    val serializedMetadata = serializer.serializeModule(input.moduleDescriptor)
    val neededLibraries = config.librariesWithDependencies()
    SerializerOutput(serializedMetadata, serializedIr, null, neededLibraries)
}

internal fun <T : PhaseContext> PhaseEngine<T>.runSerializer(
    moduleDescriptor: ModuleDescriptor,
    psiToIrResult: PsiToIrOutput.ForKlib?,
    produceHeaderKlib: Boolean = false,
): SerializerOutput {
    val input = SerializerInput(moduleDescriptor, psiToIrResult, produceHeaderKlib)
    return this.runPhase(SerializerPhase, input)
}