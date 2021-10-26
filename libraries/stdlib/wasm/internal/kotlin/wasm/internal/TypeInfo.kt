/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")  // Used by compiler

package kotlin.wasm.internal

internal const val TYPE_INFO_ELEMENT_SIZE = 4

internal const val TYPE_INFO_TYPE_PACKAGE_NAME_LENGTH_OFFSET = 0
internal const val TYPE_INFO_TYPE_PACKAGE_NAME_PRT_OFFSET = TYPE_INFO_TYPE_PACKAGE_NAME_LENGTH_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_TYPE_SIMPLE_NAME_LENGTH_OFFSET = TYPE_INFO_TYPE_PACKAGE_NAME_PRT_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_TYPE_SIMPLE_NAME_PRT_OFFSET = TYPE_INFO_TYPE_SIMPLE_NAME_LENGTH_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_SUPER_TYPE_OFFSET = TYPE_INFO_TYPE_SIMPLE_NAME_PRT_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_ITABLE_PTR_OFFSET = TYPE_INFO_SUPER_TYPE_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_VTABLE_LENGTH_OFFSET = TYPE_INFO_ITABLE_PTR_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_VTABLE_OFFSET = TYPE_INFO_VTABLE_LENGTH_OFFSET + TYPE_INFO_ELEMENT_SIZE

internal class TypeInfoData(val typeId: Int, val packageName: String, val typeName: String)

internal fun getTypeInfoTypeDataByPtr(typeInfoPtr: Int): TypeInfoData {
    val fqNameLength = wasm_i32_load(typeInfoPtr + TYPE_INFO_TYPE_PACKAGE_NAME_LENGTH_OFFSET)
    val fqNameLengthPtr = wasm_i32_load(typeInfoPtr + TYPE_INFO_TYPE_PACKAGE_NAME_PRT_OFFSET)
    val simpleNameLength = wasm_i32_load(typeInfoPtr + TYPE_INFO_TYPE_SIMPLE_NAME_LENGTH_OFFSET)
    val simpleNamePtr = wasm_i32_load(typeInfoPtr + TYPE_INFO_TYPE_SIMPLE_NAME_PRT_OFFSET)
    val packageName = stringLiteral(fqNameLengthPtr, fqNameLength)
    val simpleName = stringLiteral(simpleNamePtr, simpleNameLength)
    return TypeInfoData(typeInfoPtr, packageName, simpleName)
}

internal fun getSuperTypeId(typeInfoPtr: Int): Int =
    wasm_i32_load(typeInfoPtr + TYPE_INFO_SUPER_TYPE_OFFSET)

internal fun getVtablePtr(obj: Any): Int =
    obj.typeInfo + TYPE_INFO_VTABLE_OFFSET

internal fun getVtableLength(obj: Any): Int =
    wasm_i32_load(obj.typeInfo + TYPE_INFO_VTABLE_LENGTH_OFFSET)

internal fun getItablePtr(obj: Any): Int =
    wasm_i32_load(obj.typeInfo + TYPE_INFO_ITABLE_PTR_OFFSET)

internal fun getInterfaceListLength(itablePtr: Int): Int =
    wasm_i32_load(itablePtr + TYPE_INFO_VTABLE_LENGTH_OFFSET)

internal fun getVirtualMethodId(obj: Any, virtualFunctionSlot: Int): Int {
    val vtablePtr = getVtablePtr(obj)
    val methodIdPtr = vtablePtr + virtualFunctionSlot * TYPE_INFO_ELEMENT_SIZE
    return wasm_i32_load(methodIdPtr)
}

// Returns -1 if obj does not implement interface
internal fun getInterfaceImplId(obj: Any, interfaceId: Int): Int {
    val interfaceListSizePtr = getItablePtr(obj)
    val interfaceListPtr = interfaceListSizePtr + TYPE_INFO_ELEMENT_SIZE
    val interfaceListSize = wasm_i32_load(interfaceListSizePtr)

    var interfaceSlot = 0
    while (interfaceSlot < interfaceListSize) {
        val supportedInterface = wasm_i32_load(interfaceListPtr + interfaceSlot * TYPE_INFO_ELEMENT_SIZE)
        if (supportedInterface == interfaceId) {
            return wasm_i32_load(interfaceListPtr + interfaceListSize * TYPE_INFO_ELEMENT_SIZE + interfaceSlot * TYPE_INFO_ELEMENT_SIZE)
        }
        interfaceSlot++
    }

    return -1
}

internal fun isInterface(obj: Any, interfaceId: Int): Boolean {
    return getInterfaceImplId(obj, interfaceId) != -1
}

@ExcludedFromCodegen
internal fun <T> wasmClassId(): Int =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmInterfaceId(): Int =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmGetTypeInfoData(): TypeInfoData =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmTypeId(): Int =
    implementedAsIntrinsic