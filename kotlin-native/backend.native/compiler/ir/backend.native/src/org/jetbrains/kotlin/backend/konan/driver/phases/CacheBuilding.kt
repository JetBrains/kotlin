/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.CacheStorage
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.lower.CacheInfoBuilder
import org.jetbrains.kotlin.backend.konan.serialization.isFromCInteropLibrary
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * Builds additional cache info (inline functions bodies and fields of classes).
 */
internal val BuildAdditionalCacheInfoPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "BuildAdditionalCacheInfo",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
) { context, module ->
    // TODO: Use explicit parameter
    val parent = context.context
    val moduleDeserializer = parent.irLinker.moduleDeserializers[module.descriptor]
    if (moduleDeserializer == null) {
        require(module.descriptor.isFromCInteropLibrary()) { "No module deserializer for ${module.descriptor}" }
    } else {
        CacheInfoBuilder(context, moduleDeserializer, module).build()
    }
}

/**
 * Saves additional cache info (inline functions bodies and fields of classes).
 * It is naturally a part of "produce LLVM module", so using NativeGenerationState context should be OK.
 */
internal val SaveAdditionalCacheInfoPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "SaveAdditionalCacheInfo",
) { context, _ ->
    // TODO: Extract necessary parts of context into explicit input.
    CacheStorage(context).saveAdditionalCacheInfo()
}

/**
 * Finalizes cache (renames temp to the final dist).
 */
internal val FinalizeCachePhase = createSimpleNamedCompilerPhase<PhaseContext, OutputFiles>(
        name = "FinalizeCache",
) { context, outputFiles ->
    //  TODO: Explicit parameter
    CacheStorage.renameOutput(outputFiles, overwrite = context.config.producePerFileCache)
}
