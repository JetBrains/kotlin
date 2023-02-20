/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.CacheStorage
import org.jetbrains.kotlin.backend.konan.DependenciesTracker
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.lower.CacheInfoBuilder
import org.jetbrains.kotlin.backend.konan.serialization.SerializedClassFields
import org.jetbrains.kotlin.backend.konan.serialization.SerializedEagerInitializedFile
import org.jetbrains.kotlin.backend.konan.serialization.SerializedInlineFunctionReference
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.io.File

internal val BuildAdditionalCacheInfoPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "BuildAdditionalCacheInfo",
        description = "Build additional cache info (inline functions bodies and fields of classes)",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
) { context, module ->
    // TODO: Use explicit parameter
    val parent = context.context
    val moduleDeserializer = parent.irLinker.moduleDeserializers[module.descriptor]
    if (moduleDeserializer == null) {
        require(module.descriptor.isFromInteropLibrary()) { "No module deserializer for ${module.descriptor}" }
    } else {
        CacheInfoBuilder(context, moduleDeserializer, module).build()
    }
}

internal data class SaveAdditionalCacheInfoInput(
        val cacheRootDirectory: File,
        val immediateBitcodeDependencies: List<DependenciesTracker.ResolvedDependency>,
        val inlineFunctionBodies: List<SerializedInlineFunctionReference>,
        val classFields: List<SerializedClassFields>,
        val eagerInitializedFiles: List<SerializedEagerInitializedFile>,
)

internal val SaveAdditionalCacheInfoPhase = createSimpleNamedCompilerPhase<PhaseContext, SaveAdditionalCacheInfoInput>(
        name = "SaveAdditionalCacheInfo",
        description = "Save additional cache info (inline functions bodies and fields of classes)"
) { _, input ->
    CacheStorage(input.cacheRootDirectory).saveAdditionalCacheInfo(
            input.immediateBitcodeDependencies,
            input.inlineFunctionBodies,
            input.classFields,
            input.eagerInitializedFiles
    )
}

internal data class FinalizeCacheInput(
        val tempDir: File,
        val finalDir: File,
)

internal val FinalizeCachePhase = createSimpleNamedCompilerPhase<PhaseContext, FinalizeCacheInput>(
        name = "FinalizeCache",
        description = "Finalize cache (rename temp to the final dist)"
) { _, input ->
    CacheStorage.renameOutput(input.tempDir, input.finalDir)
}