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

internal class TypeInfoData(val typeId: Int, val isInterface: Boolean, val packageName: String, val typeName: String)

internal fun getTypeInfoTypeDataByPtr(typeInfoPtr: Int): TypeInfoData {
    val fqNameLength = wasm_i32_load(typeInfoPtr + TYPE_INFO_TYPE_PACKAGE_NAME_LENGTH_OFFSET)
    val fqNameId = wasm_i32_load(typeInfoPtr + TYPE_INFO_TYPE_PACKAGE_NAME_ID_OFFSET)
    val fqNamePtr = wasm_i32_load(typeInfoPtr + TYPE_INFO_TYPE_PACKAGE_NAME_PRT_OFFSET)
    val simpleNameLength = wasm_i32_load(typeInfoPtr + TYPE_INFO_TYPE_SIMPLE_NAME_LENGTH_OFFSET)
    val simpleNameId = wasm_i32_load(typeInfoPtr + TYPE_INFO_TYPE_SIMPLE_NAME_ID_OFFSET)
    val simpleNamePtr = wasm_i32_load(typeInfoPtr + TYPE_INFO_TYPE_SIMPLE_NAME_PRT_OFFSET)
    val packageName = stringLiteral(fqNameId, fqNamePtr, fqNameLength)
    val simpleName = stringLiteral(simpleNameId, simpleNamePtr, simpleNameLength)
    return TypeInfoData(typeInfoPtr, isInterface = false, packageName, simpleName)
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
internal fun <T> wasmClassId(): Int =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmInterfaceId(): Int =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmGetTypeInfoData(): TypeInfoData =
    implementedAsIntrinsic
