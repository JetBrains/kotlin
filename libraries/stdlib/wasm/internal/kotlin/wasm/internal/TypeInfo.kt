/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")  // Used by compiler

package kotlin.wasm.internal

internal const val TYPE_INFO_ELEMENT_SIZE = 4

internal const val TYPE_INFO_TYPE_PACKAGE_NAME_LENGTH_OFFSET = 0
internal const val TYPE_INFO_TYPE_PACKAGE_NAME_ID_OFFSET = TYPE_INFO_TYPE_PACKAGE_NAME_LENGTH_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_TYPE_PACKAGE_NAME_PRT_OFFSET = TYPE_INFO_TYPE_PACKAGE_NAME_ID_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_TYPE_SIMPLE_NAME_LENGTH_OFFSET = TYPE_INFO_TYPE_PACKAGE_NAME_PRT_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_TYPE_SIMPLE_NAME_ID_OFFSET = TYPE_INFO_TYPE_SIMPLE_NAME_LENGTH_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_TYPE_SIMPLE_NAME_PRT_OFFSET = TYPE_INFO_TYPE_SIMPLE_NAME_ID_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_SUPER_TYPE_OFFSET = TYPE_INFO_TYPE_SIMPLE_NAME_PRT_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_ITABLE_SIZE_OFFSET = TYPE_INFO_SUPER_TYPE_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_ITABLE_OFFSET = TYPE_INFO_ITABLE_SIZE_OFFSET + TYPE_INFO_ELEMENT_SIZE

internal class TypeInfoData(val typeId: Int, val packageName: String, val typeName: String)

internal val TypeInfoData.isInterfaceType
    get() = typeId < 0

internal fun getTypeInfoTypeDataByPtr(typeInfoPtr: Int): TypeInfoData {
    val packageName = getPackageName(typeInfoPtr)
    val simpleName = getSimpleName(typeInfoPtr)
    return TypeInfoData(typeInfoPtr, packageName, simpleName)
}

internal fun getSimpleName(typeInfoPtr: Int) = getString(
    typeInfoPtr,
    TYPE_INFO_TYPE_SIMPLE_NAME_LENGTH_OFFSET,
    TYPE_INFO_TYPE_SIMPLE_NAME_ID_OFFSET,
    TYPE_INFO_TYPE_SIMPLE_NAME_PRT_OFFSET
)

internal fun getPackageName(typeInfoPtr: Int) = getString(
    typeInfoPtr,
    TYPE_INFO_TYPE_PACKAGE_NAME_LENGTH_OFFSET,
    TYPE_INFO_TYPE_PACKAGE_NAME_ID_OFFSET,
    TYPE_INFO_TYPE_PACKAGE_NAME_PRT_OFFSET
)

private fun getString(typeInfoPtr: Int, lengthOffset: Int, idOffset: Int, ptrOffset: Int): String {
    val length = wasm_i32_load(typeInfoPtr + lengthOffset)
    val id = wasm_i32_load(typeInfoPtr + idOffset)
    val ptr = wasm_i32_load(typeInfoPtr + ptrOffset)
    return stringLiteral(id, ptr, length)
}

internal fun getSuperTypeId(typeInfoPtr: Int): Int =
    wasm_i32_load(typeInfoPtr + TYPE_INFO_SUPER_TYPE_OFFSET)

internal fun isInterfaceById(obj: Any, interfaceId: Int): Boolean {
    val interfaceListSize = wasm_i32_load(obj.typeInfo + TYPE_INFO_ITABLE_SIZE_OFFSET)
    val interfaceListPtr = obj.typeInfo + TYPE_INFO_ITABLE_OFFSET

    var interfaceSlot = 0
    while (interfaceSlot < interfaceListSize) {
        val supportedInterface = wasm_i32_load(interfaceListPtr + interfaceSlot * TYPE_INFO_ELEMENT_SIZE)
        if (supportedInterface == interfaceId) {
            return true
        }
        interfaceSlot++
    }
    return false
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <T> wasmIsInterface(obj: Any): Boolean =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmTypeId(): Int =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmGetTypeInfoData(): TypeInfoData =
    implementedAsIntrinsic
