/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.isSuspend

/**
 * LLVM function's parameter type with its attributes.
 */
class LlvmParamType(val llvmType: LLVMTypeRef, val attributes: List<LlvmParameterAttribute> = emptyList())

/**
 * A bit better readability for cases when [LlvmParamType] represents return type.
 */
typealias LlvmRetType = LlvmParamType

internal fun ContextUtils.getLlvmFunctionParameterTypes(function: IrFunction): List<LlvmParamType> {
    val returnType = getLlvmFunctionReturnType(function).llvmType
    val paramTypes = ArrayList(function.allParameters.map {
        LlvmParamType(getLLVMType(it.type), argumentAbiInfo.defaultParameterAttributesForIrType(it.type))
    })
    if (function.isSuspend)
        paramTypes.add(LlvmParamType(kObjHeaderPtr))                       // Suspend functions have implicit parameter of type Continuation<>.
    if (isObjectType(returnType))
        paramTypes.add(LlvmParamType(kObjHeaderPtrPtr))

    return paramTypes
}

internal fun ContextUtils.getLlvmFunctionReturnType(function: IrFunction): LlvmRetType {
    val returnType = when {
        function is IrConstructor -> LlvmParamType(voidType)
        function.isSuspend -> LlvmParamType(kObjHeaderPtr)                // Suspend functions return Any?.
        else -> LlvmParamType(
                getLLVMReturnType(function.returnType),
                argumentAbiInfo.defaultParameterAttributesForIrType(function.returnType)
        )
    }
    return returnType
}