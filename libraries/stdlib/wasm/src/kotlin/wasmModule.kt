/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

/**
 * Contains wasm module-dependent services, like associated object getter.
 */
internal class WasmModuleDescriptor(val associatedObjectGetter: kotlin.wasm.internal.reftypes.anyref)

/**
 * List of wasm modules-dependent services.
 */
internal val moduleDescriptors = mutableListOf<WasmModuleDescriptor>()

/**
 * Register new wasm module-dependent descriptor [kotlin.wasm.internal.WasmModuleDescriptor].
 * TODO: Should be removed after bootstrap
 */
internal fun registerModuleDescriptor(associatedObjectGetter: kotlin.wasm.internal.reftypes.funcref) {}

/**
 * Register new wasm module-dependent descriptor [kotlin.wasm.internal.WasmModuleDescriptor].
 * TODO: Should be renamed after bootstrap
 */
internal fun registerModuleDescriptorWithWrapper(associatedObjectGetter: kotlin.wasm.internal.reftypes.anyref) {
    moduleDescriptors.add(WasmModuleDescriptor(associatedObjectGetter))
}

/**
 * Internal intrinsic to get associated object's for provided associated object getter.
 */
@ExcludedFromCodegen
@Suppress("UNUSED_PARAMETER")
internal fun callAssociatedObjectGetter(param1: Long, param2: Long, funcRefWrapper: kotlin.wasm.internal.reftypes.anyref): Any? =
    implementedAsIntrinsic