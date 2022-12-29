/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.CacheStorage
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.lower.CacheInfoBuilder
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.io.File

internal val BuildAdditionalCacheInfoPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment, CacheAdditionalInfo?>(
        name = "BuildAdditionalCacheInfo",
        description = "Build additional cache info (inline functions bodies and fields of classes)",
        outputIfNotEnabled = { _, _, _, _ -> null }
) { context, module ->
    // TODO: Use explicit parameter
    val parent = context.context
    val moduleDeserializer = parent.irLinker.moduleDeserializers[module.descriptor]
    if (moduleDeserializer == null) {
        require(module.descriptor.isFromInteropLibrary()) { "No module deserializer for ${module.descriptor}" }
        null
    } else {
        CacheInfoBuilder(context, moduleDeserializer, module).build()
    }
}

internal data class SaveAdditionalCacheInfoPhaseInput(
        val cacheAdditionalInfo: CacheAdditionalInfo?,
        val dependenciesTrackingResult: DependenciesTrackingResult,
        val cacheDirectory: File
)

/**
 * It is naturally a part of "produce LLVM module", so using NativeGenerationState context should be OK.
 */
internal val SaveAdditionalCacheInfoPhase = createSimpleNamedCompilerPhase<PhaseContext, SaveAdditionalCacheInfoPhaseInput>(
        name = "SaveAdditionalCacheInfo",
        description = "Save additional cache info (inline functions bodies and fields of classes)"
) { context, input ->
    val cacheOutputs = CacheOutputs(input.cacheDirectory.absolutePath, context.config.target, context.config.produce)
    CacheStorage(cacheOutputs).saveAdditionalCacheInfo(input.cacheAdditionalInfo, input.dependenciesTrackingResult)
}

internal val FinalizeCachePhase = createSimpleNamedCompilerPhase<PhaseContext, CacheOutputs>(
        name = "FinalizeCache",
        description = "Finalize cache (rename temp to the final dist)"
) { _, outputFiles ->
    CacheStorage.renameOutput(outputFiles)
}