/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.unwrapToPrimitiveOrReference
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.isSuspend

/**
 * LLVM function's parameter type with its attributes.
 */
class LlvmParamType(val llvmType: LLVMTypeRef, val attributes: List<LlvmParameterAttribute> = emptyList())

/**
 * A bit better readability for cases when [LlvmParamType] represents return type.
 */
typealias LlvmRetType = LlvmParamType

internal fun RuntimeAware.getLlvmFunctionParameterTypes(function: IrFunction): List<LlvmParamType> {
    val returnType = getLlvmFunctionReturnType(function).llvmType
    val paramTypes = ArrayList(function.allParameters.map { LlvmParamType(getLLVMType(it.type), defaultParameterAttributesForIrType(it.type)) })
    if (function.isSuspend)
        paramTypes.add(LlvmParamType(kObjHeaderPtr))                       // Suspend functions have implicit parameter of type Continuation<>.
    if (isObjectType(returnType))
        paramTypes.add(LlvmParamType(kObjHeaderPtrPtr))

    return paramTypes
}

internal fun RuntimeAware.getLlvmFunctionReturnType(function: IrFunction): LlvmRetType {
    val returnType = when {
        function is IrConstructor -> LlvmParamType(voidType)
        function.isSuspend -> LlvmParamType(kObjHeaderPtr)                // Suspend functions return Any?.
        else -> LlvmParamType(getLLVMReturnType(function.returnType), defaultParameterAttributesForIrType(function.returnType))
    }
    return returnType
}

// Note: Probably, this function should become target-dependent in the future.
private fun defaultParameterAttributesForIrType(irType: IrType): List<LlvmParameterAttribute> {
    // TODO: We perform type unwrapping twice: one to get the underlying type, and then this one.
    //  Unwrapping is not cheap, so it might affect compilation time.
    return irType.unwrapToPrimitiveOrReference(
            eachInlinedClass = { inlinedClass, _ ->
                when (inlinedClass.classId) {
                    UnsignedType.UBYTE.classId -> return listOf(LlvmParameterAttribute.ZeroExt)
                    UnsignedType.USHORT.classId -> return listOf(LlvmParameterAttribute.ZeroExt)
                }
            },
            ifPrimitive = { primitiveType, _ ->
                when (primitiveType) {
                    KonanPrimitiveType.BOOLEAN -> listOf(LlvmParameterAttribute.ZeroExt)
                    KonanPrimitiveType.CHAR -> listOf(LlvmParameterAttribute.ZeroExt)
                    KonanPrimitiveType.BYTE -> listOf(LlvmParameterAttribute.SignExt)
                    KonanPrimitiveType.SHORT -> listOf(LlvmParameterAttribute.SignExt)
                    KonanPrimitiveType.INT -> emptyList()
                    KonanPrimitiveType.LONG -> emptyList()
                    KonanPrimitiveType.FLOAT -> emptyList()
                    KonanPrimitiveType.DOUBLE -> emptyList()
                    KonanPrimitiveType.NON_NULL_NATIVE_PTR -> emptyList()
                    KonanPrimitiveType.VECTOR128 -> emptyList()
                }
            },
            ifReference = {
                return listOf()
            },
    )
}
