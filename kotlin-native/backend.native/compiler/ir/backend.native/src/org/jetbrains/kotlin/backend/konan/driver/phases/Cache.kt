/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.CacheStorage
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.konanUnitPhase
import org.jetbrains.kotlin.backend.konan.lower.CacheInfoBuilder
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal val BuildAdditionalCacheInfoPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
    "BuildAdditionalCacheInfo",
    "Build additional cache info (inline functions bodies and fields of classes)"
) { context, module ->
    val irLinker = context.context.irLinker
    val moduleDeserializer = irLinker.moduleDeserializers[module.descriptor]
    if (moduleDeserializer == null) {
        require(module.descriptor.isFromInteropLibrary()) { "No module deserializer for ${module.descriptor}" }
    } else {
        CacheInfoBuilder(context, moduleDeserializer, module).build()
    }
}

internal val SaveAdditionalCacheInfoPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
    "SaveAdditionalCacheInfo",
    "Save additional cache info (inline functions bodies and fields of classes)"
) { context, _ ->
    CacheStorage(context).saveAdditionalCacheInfo()
}

internal val FinalizeCachePhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
    name = "FinalizeCache",
    description = "Finalize cache (rename temp to the final dist)"
) { context, _ ->
    CacheStorage(context).renameOutput()
}