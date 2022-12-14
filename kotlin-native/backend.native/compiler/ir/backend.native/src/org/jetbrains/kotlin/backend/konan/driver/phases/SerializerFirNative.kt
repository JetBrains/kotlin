/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.util.IrMessageLogger

internal val SerializerFirNativePhase = createSimpleNamedCompilerPhase<K2FrontendContext, K2FrontendPhaseOutput.IR, SerializerOutput>(
        "SerializerFirNative", "FIR serializer in Native style",
        outputIfNotEnabled = { _, _, _, _ -> SerializerOutput(null, null, null, emptyList()) }
) { context: K2FrontendContext, input: K2FrontendPhaseOutput.IR ->
    val environment: KotlinCoreEnvironment = context.environment
    val sourceFiles = input.firFiles.mapNotNull { it.sourceFile }
    assert(sourceFiles.size == input.fir2irResult.irModuleFragment.files.size)
    val configuration = environment.configuration
    val messageLogger: IrMessageLogger = configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None
    val expectActualLinker = configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER) ?: false
    val sourceBaseDirs = configuration[CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES] ?: emptyList()
    val absolutePathNormalization = configuration[CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH] ?: false
    val serializedIr =
            KonanIrModuleSerializer(
                    messageLogger,
                    input.fir2irResult.irModuleFragment.irBuiltins,
                    mutableMapOf(), // TODO: expect -> actual mapping
                    skipExpects = !expectActualLinker,
                    compatibilityMode = CompatibilityMode.CURRENT, // TODO get from test file data
                    normalizeAbsolutePaths = absolutePathNormalization,
                    sourceBaseDirs = sourceBaseDirs
            ).serializedIrModule(input.fir2irResult.irModuleFragment)
    val serializer = KlibMetadataMonolithicSerializer(
            configuration.languageVersionSettings,
            configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!,
            context.config.project,
            exportKDoc = context.shouldExportKDoc(),
            !expectActualLinker, includeOnlyModuleContent = true)
    val serializedMetadata = serializer.serializeModule(input.fir2irResult.irModuleFragment.descriptor)
    val neededLibraries = context.config.librariesWithDependencies(input.fir2irResult.irModuleFragment.descriptor)

    SerializerOutput(serializedMetadata, serializedIr, null, neededLibraries)
}

internal fun <T : K2FrontendContext> PhaseEngine<T>.runSerializerFirNative(
        input: K2FrontendPhaseOutput.IR
): SerializerOutput {
    return this.runPhase(SerializerFirNativePhase, input)
}

