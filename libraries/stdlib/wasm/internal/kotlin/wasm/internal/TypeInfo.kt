/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")  // Used by compiler

package kotlin.wasm.internal

internal class TypeInfoData(val typeId: Long, val packageName: String, val typeName: String)

@Suppress("UNUSED_PARAMETER")
@WasmArrayOf(Long::class, isNullable = false, isMutable = false)
internal class WasmLongImmutableArray(size: Int) {
    @WasmOp(WasmOp.ARRAY_GET)
    fun get(index: Int): Long =
        implementedAsIntrinsic

    @WasmOp(WasmOp.ARRAY_LEN)
    fun len(): Int =
        implementedAsIntrinsic
}

internal fun getInterfaceSlot(obj: Any, interfaceId: Long): Int {
    val interfaceArray = wasmGetRttiSupportedInterfaces(obj) ?: return -1
    val interfaceArraySize = interfaceArray.len()

    var interfaceSlot = 0
    while (interfaceSlot < interfaceArraySize) {
        val supportedInterface = interfaceArray.get(interfaceSlot)
        if (supportedInterface == interfaceId) {
            return interfaceSlot
        }
        interfaceSlot++
    }
    return -1
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <T> wasmIsInterface(obj: Any): Boolean =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmTypeId(): Long =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmGetTypeRtti(): kotlin.wasm.internal.reftypes.structref =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun wasmGetObjectRtti(obj: Any): kotlin.wasm.internal.reftypes.structref =
    implementedAsIntrinsic

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun wasmGetRttiSupportedInterfaces(obj: Any): WasmLongImmutableArray? =
    implementedAsIntrinsic

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun wasmGetRttiIntField(intFieldIndex: Int, obj: kotlin.wasm.internal.reftypes.structref): Int =
    implementedAsIntrinsic

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun wasmGetRttiLongField(intFieldIndex: Int, obj: kotlin.wasm.internal.reftypes.structref): Long =
    implementedAsIntrinsic

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun wasmGetRttiSuperClass(rtti: kotlin.wasm.internal.reftypes.structref): kotlin.wasm.internal.reftypes.structref? =
    implementedAsIntrinsic

