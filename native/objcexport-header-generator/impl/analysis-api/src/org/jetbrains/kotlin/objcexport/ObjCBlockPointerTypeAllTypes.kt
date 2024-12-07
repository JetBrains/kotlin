package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCBlockPointerType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCType
import org.jetbrains.kotlin.tooling.core.withClosure

/**
 * [ObjCBlockPointerType] used to represent functional types
 * ```kotlin
 * val foo: (String) -> Int
 * ```
 * So to fetch all [ObjCType]`s we need to traverse over all function arguments and return type
 * Plus traverse over all nested functions:
 * ```kotlin
 * val foo: (String) -> (Int) -> () -> Unit
 * ```
 */
internal fun ObjCBlockPointerType.allTypes(): List<ObjCType> {
    return this.withClosure<ObjCType> { type ->
        if (type is ObjCBlockPointerType) listOf(type.returnType) + type.parameterTypes
        else emptyList()
    }.toList()
}