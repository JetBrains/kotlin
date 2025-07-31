/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")  // Used by compiler

package kotlin.wasm.internal

internal class TypeInfoData(val typeId: Long, val packageName: String, val typeName: String)

private const val TYPE_INFO_FLAG_ANONYMOUS_CLASS = 1
private const val TYPE_INFO_FLAG_LOCAL_CLASS = 2
private const val TYPE_INFO_FLAG_FITS_LATIN1_QUALIFIER = 4
private const val TYPE_INFO_FLAG_FITS_LATIN1_SIMPLE_NAME = 8

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

internal fun wasmArrayAnyIndexOfValue(array: WasmLongImmutableArray, value: Long): Int {
    val arraySize = array.len()
    var index = 0
    while (index < arraySize) {
        val supportedInterface = array.get(index)
        if (supportedInterface == value) {
            return index
        }
        index++
    }
    return -1
}

internal fun isSupportedInterface(obj: Any, interfaceId: Long): Boolean {
    val interfaceArray = wasmGetRttiSupportedInterfaces(obj) ?: return false
    return wasmArrayAnyIndexOfValue(interfaceArray, interfaceId) != -1
}

internal fun getInterfaceVTable(obj: Any, interfaceId: Long): kotlin.wasm.internal.reftypes.anyref =
    wasmGetInterfaceVTableBodyImpl()

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun wasmGetInterfaceVTableBodyImpl(): kotlin.wasm.internal.reftypes.anyref =
    implementedAsIntrinsic

internal fun getQualifiedName(rtti: kotlin.wasm.internal.reftypes.structref): String {
    val typeName = getSimpleName(rtti)
    val packageName = getPackageName(rtti)
    return if (packageName.isEmpty()) typeName else "$packageName.$typeName"
}

internal fun getPackageName(rtti: kotlin.wasm.internal.reftypes.structref): String {
    if (getWasmAbiVersion() < 1) return getPackageName0(rtti)

    val flagFitsOneBitQualifier = wasmGetRttiIntField(9, rtti) and TYPE_INFO_FLAG_FITS_LATIN1_QUALIFIER
    val poolId = wasmGetRttiIntField(2, rtti)
    return if (flagFitsOneBitQualifier != 0)
        stringLiteralLatin1(poolId)
    else
        stringLiteralUtf16(poolId)
}

internal fun getSimpleName(rtti: kotlin.wasm.internal.reftypes.structref): String {
    if (getWasmAbiVersion() < 1) return getSimpleName0(rtti)

    val flagFitsOneBitSimpleName = wasmGetRttiIntField(9, rtti) and TYPE_INFO_FLAG_FITS_LATIN1_SIMPLE_NAME
    val poolId = wasmGetRttiIntField(3, rtti)
    return if (flagFitsOneBitSimpleName != 0)
        stringLiteralLatin1(poolId)
    else
        stringLiteralUtf16(poolId)
}

internal fun getTypeId(rtti: kotlin.wasm.internal.reftypes.structref): Long =
    wasmGetRttiLongField(8, rtti)

internal fun isAnonymousClass(rtti: kotlin.wasm.internal.reftypes.structref): Boolean =
    (wasmGetRttiIntField(9, rtti) and TYPE_INFO_FLAG_ANONYMOUS_CLASS) != 0

internal fun isLocalClass(rtti: kotlin.wasm.internal.reftypes.structref): Boolean =
    (wasmGetRttiIntField(9, rtti) and TYPE_INFO_FLAG_LOCAL_CLASS) != 0

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

// TODO remove following *0 declarations after bootstrap

internal fun getPackageName0(rtti: kotlin.wasm.internal.reftypes.structref): String = stringLiteral(
    start = wasmGetRttiIntField(2, rtti),
    length = wasmGetRttiIntField(3, rtti),
    poolId = wasmGetRttiIntField(4, rtti),
)

internal fun getSimpleName0(rtti: kotlin.wasm.internal.reftypes.structref): String = stringLiteral(
    start = wasmGetRttiIntField(5, rtti),
    length = wasmGetRttiIntField(6, rtti),
    poolId = wasmGetRttiIntField(7, rtti),
)
