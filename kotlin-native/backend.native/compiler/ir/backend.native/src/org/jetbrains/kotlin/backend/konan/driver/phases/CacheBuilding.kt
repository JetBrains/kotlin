/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.serialization.SerializedKlibFingerprint
import org.jetbrains.kotlin.backend.common.serialization.kotlinLibrary
import org.jetbrains.kotlin.backend.konan.CacheStorage
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.lower.PostLoweringsCacheInfoBuilder
import org.jetbrains.kotlin.backend.konan.lower.PreLoweringsCacheInfoBuilder
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary

/**
 * Builds additional cache info on a pre-lowered state (e.g. inline functions bodies).
 */
internal val BuildAdditionalPreLoweringsCacheInfoPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "BuildAdditionalPreLoweringsCacheInfo",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
) { context, module ->
    // TODO: Use explicit parameter
    val parent = context.context

    val klib = module.kotlinLibrary ?: error("No Kotlin library for module ${module.name}")
    val moduleDeserializer = parent.moduleDeserializerProvider.getDeserializerOrNull(klib)
    if (moduleDeserializer == null) {
        require(klib.isCInteropLibrary()) { "No module deserializer for module ${module.name} (${klib.path})" }
        // C-interop libraries have no deserialized IR, so PreLoweringsCacheInfoBuilder is not run for them. Still record
        // the library fingerprint, so that the cache staleness check can detect a changed library (KT-87273).
        context.klibHash = SerializedKlibFingerprint(klib.path.toFile()).klibFingerprint
    } else {
        PreLoweringsCacheInfoBuilder(context, moduleDeserializer, module).build()
    }
}

/**
 * Builds additional cache info on a post-lowered state (e.g. class layout).
 */
internal val BuildAdditionalPostLoweringsCacheInfoPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "BuildAdditionalPostLoweringsCacheInfo",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
) { context, module ->
    // TODO: Use explicit parameter
    val parent = context.context

    val klib = module.kotlinLibrary ?: error("No Kotlin library for module ${module.name}")
    val moduleDeserializer = parent.moduleDeserializerProvider.getDeserializerOrNull(klib)
    if (moduleDeserializer == null) {
        require(klib.isCInteropLibrary()) { "No module deserializer for module ${module.name} (${klib.path})" }
        return@createSimpleNamedCompilerPhase
    }

    PostLoweringsCacheInfoBuilder(context, moduleDeserializer, module).build()
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
internal val FinalizeCachePhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, OutputFiles>(
        name = "FinalizeCache",
) { context, outputFiles ->
    //  TODO: Explicit parameter
    CacheStorage.renameOutput(outputFiles, overwrite = context.config.producePerFileCache)
}
