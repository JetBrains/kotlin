/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")  // Used by compiler

package kotlin.wasm.internal

internal const val TYPE_INFO_ELEMENT_SIZE = 4

internal const val TYPE_INFO_VTABLE_OFFSET = 2 * TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_VTABLE_LENGTH_OFFSET = TYPE_INFO_ELEMENT_SIZE
internal const val SUPER_CLASS_ID_OFFSET = 0

internal fun getVtablePtr(obj: Any): Int =
    obj.typeInfo + TYPE_INFO_VTABLE_OFFSET

internal fun getVtableLength(obj: Any): Int =
    wasm_i32_load(obj.typeInfo + TYPE_INFO_VTABLE_LENGTH_OFFSET)

internal fun getInterfaceListLength(obj: Any): Int =
    wasm_i32_load(obj.typeInfo + TYPE_INFO_VTABLE_LENGTH_OFFSET)

internal fun getSuperClassId(obj: Any): Int =
    wasm_i32_load(obj.typeInfo + SUPER_CLASS_ID_OFFSET)

internal fun getVirtualMethodId(obj: Any, virtualFunctionSlot: Int): Int {
    val vtablePtr = getVtablePtr(obj)
    val methodIdPtr = vtablePtr + virtualFunctionSlot * TYPE_INFO_ELEMENT_SIZE
    return wasm_i32_load(methodIdPtr)
}

internal fun getInterfaceMethodId(obj: Any, methodSignatureId: Int): Int {
    val vtableLength = getVtableLength(obj)
    val vtableSignatures = getVtablePtr(obj) + vtableLength * TYPE_INFO_ELEMENT_SIZE
    var virtualFunctionSlot = 0
    while (virtualFunctionSlot < vtableLength) {
        if (wasm_i32_load(vtableSignatures + virtualFunctionSlot * TYPE_INFO_ELEMENT_SIZE) == methodSignatureId) {
            return getVirtualMethodId(obj, virtualFunctionSlot)
        }
        virtualFunctionSlot++
    }
    wasm_unreachable()
}


internal fun isSubClassOfImpl(currentClassId: Int, otherClassId: Int): Boolean {
    if (currentClassId == otherClassId) return true
    val anyClassId = wasmClassId<Any>()
    if (currentClassId == anyClassId && otherClassId != anyClassId) return false
    return isSubClassOfImpl(wasm_i32_load(currentClassId + SUPER_CLASS_ID_OFFSET), otherClassId)
}

internal fun isSubClass(obj: Any, classId: Int): Boolean {
    return isSubClassOfImpl(obj.typeInfo, classId)
}

internal fun isInterface(obj: Any, interfaceId: Int): Boolean {
    val vtableLength = getVtableLength(obj)
    val interfaceListSizePtr = getVtablePtr(obj) + 2 * vtableLength * TYPE_INFO_ELEMENT_SIZE
    val interfaceListPtr = interfaceListSizePtr + TYPE_INFO_ELEMENT_SIZE
    val interfaceListSize = wasm_i32_load(interfaceListSizePtr)

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

@ExcludedFromCodegen
internal fun <T> wasmClassId(): Int =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmInterfaceId(): Int =
    implementedAsIntrinsic