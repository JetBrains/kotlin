/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

internal class WasmModuleDescriptor(val associatedObjectGetter: kotlin.wasm.internal.reftypes.funcref)

internal val moduleDescriptors = mutableListOf<WasmModuleDescriptor>()

internal fun registerModuleDescriptor(associatedObjectGetter: kotlin.wasm.internal.reftypes.funcref) {
    moduleDescriptors.add(WasmModuleDescriptor(associatedObjectGetter))
}

@Suppress("UNUSED_PARAMETER")
internal fun callAssociatedObjectGetter(param1: Long, param2: Long, funcRef: kotlin.wasm.internal.reftypes.funcref): Any? =
    null //TODO: Make this intrinsic after bootstrap