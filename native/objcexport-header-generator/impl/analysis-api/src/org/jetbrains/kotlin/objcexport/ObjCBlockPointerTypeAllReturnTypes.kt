package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCBlockPointerType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCType
import org.jetbrains.kotlin.tooling.core.withLinearClosure

internal fun ObjCBlockPointerType.allReturnTypes(): List<ObjCType> {
    return this.withLinearClosure<ObjCType> { type ->
        if (type is ObjCBlockPointerType) type.returnType
        else null
    }.toList()
}