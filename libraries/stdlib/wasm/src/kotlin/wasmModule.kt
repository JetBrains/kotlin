/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

/**
 * Contains wasm module-dependent services, like associated object getter.
 */
internal class WasmModuleDescriptor(val associatedObjectGetter: kotlin.wasm.internal.reftypes.funcref)

/**
 * List of wasm modules-dependent services.
 */
internal val moduleDescriptors = mutableListOf<WasmModuleDescriptor>()

/**
 * Register new wasm module-dependent descriptor [kotlin.wasm.internal.WasmModuleDescriptor].
 */
internal fun registerModuleDescriptor(associatedObjectGetter: kotlin.wasm.internal.reftypes.funcref) {
    moduleDescriptors.add(WasmModuleDescriptor(associatedObjectGetter))
}

/**
 * Internal intrinsic to get associated object's for provided associated object getter.
 */
@Suppress("UNUSED_PARAMETER")
internal fun callAssociatedObjectGetter(param1: Long, param2: Long, funcRef: kotlin.wasm.internal.reftypes.funcref): Any? =
    null //TODO: Make this intrinsic after bootstrap