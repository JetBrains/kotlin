/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")  // Used by compiler

package kotlin.wasm.internal

internal const val TYPE_INFO_ELEMENT_SIZE = 4


internal const val SUPER_CLASS_ID_OFFSET = 0
internal const val TYPE_INFO_ITABLE_PTR_OFFSET = SUPER_CLASS_ID_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_VTABLE_LENGTH_OFFSET = TYPE_INFO_ITABLE_PTR_OFFSET + TYPE_INFO_ELEMENT_SIZE
internal const val TYPE_INFO_VTABLE_OFFSET = TYPE_INFO_VTABLE_LENGTH_OFFSET + TYPE_INFO_ELEMENT_SIZE

internal fun getVtablePtr(obj: Any): Int =
    obj.typeInfo + TYPE_INFO_VTABLE_OFFSET

internal fun getVtableLength(obj: Any): Int =
    wasm_i32_load(obj.typeInfo + TYPE_INFO_VTABLE_LENGTH_OFFSET)

internal fun getItablePtr(obj: Any): Int =
    wasm_i32_load(obj.typeInfo + TYPE_INFO_ITABLE_PTR_OFFSET)

internal fun getInterfaceListLength(itablePtr: Int): Int =
    wasm_i32_load(itablePtr + TYPE_INFO_VTABLE_LENGTH_OFFSET)

internal fun getSuperClassId(obj: Any): Int =
    wasm_i32_load(obj.typeInfo + SUPER_CLASS_ID_OFFSET)

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
    return getInterfaceImplId(obj, interfaceId) != -1
}

@ExcludedFromCodegen
internal fun <T> wasmClassId(): Int =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmInterfaceId(): Int =
    implementedAsIntrinsic