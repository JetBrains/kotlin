/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfigurationService
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.backend.common.phaser.SimpleNamedCompilerPhase
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
        val psiToIrResult: PsiToIrResult?,
)

data class SerializerResult(
        val serializedMetadata: SerializedMetadata?,
        val serializedIr: SerializedIrModule?,
        val dataFlowGraph: ByteArray?,
        val neededLibraries: List<KonanLibrary>
)

internal val SerializerPhase = object : SimpleNamedCompilerPhase<PhaseContext, SerializerInput, SerializerResult>(
        "Serializer", "IR serialzer",
) {
    override fun phaseBody(context: PhaseContext, input: SerializerInput): SerializerResult {
        val config = context.config
        val expectActualLinker = config.configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER) ?: false
        val messageLogger = config.configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None
        val relativePathBase = config.configuration.get(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES) ?: emptyList()
        val normalizeAbsolutePaths = config.configuration.get(CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH) ?: false

        val serializedIr = if (input.psiToIrResult != null) {
            val ir = input.psiToIrResult.irModule
            KonanIrModuleSerializer(
                    messageLogger, ir.irBuiltins, input.psiToIrResult.expectDescriptorToSymbol,
                    skipExpects = !expectActualLinker,
                    compatibilityMode = CompatibilityMode.CURRENT,
                    normalizeAbsolutePaths = normalizeAbsolutePaths,
                    sourceBaseDirs = relativePathBase,
            ).serializedIrModule(ir)
        } else null

        val serializer = KlibMetadataMonolithicSerializer(
                config.configuration.languageVersionSettings,
                config.configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!,
                config.project,
                exportKDoc = context.shouldExportKDoc(),
                !expectActualLinker, includeOnlyModuleContent = true)
        val serializedMetadata = serializer.serializeModule(input.moduleDescriptor)
        val neededLibraries = config.librariesWithDependencies(input.moduleDescriptor)
        return SerializerResult(serializedMetadata, serializedIr, null, neededLibraries)
    }

    override fun outputIfNotEnabled(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<SerializerInput>, context: PhaseContext, input: SerializerInput): SerializerResult =
            SerializerResult(null, null, null, emptyList())
}

internal fun <T: PhaseContext> PhaseEngine<T>.runSerializer(
        moduleDescriptor: ModuleDescriptor,
        psiToIrResult: PsiToIrResult?,
): SerializerResult {
    val input = SerializerInput(moduleDescriptor, psiToIrResult)
    return this.runPhase(context, SerializerPhase, input)
}