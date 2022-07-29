/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.isBuiltInOperator
import org.jetbrains.kotlin.backend.konan.lower.isStaticInitializer
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_OBJECT_INSTANCE_GETTER
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.*

/**
 * LLVM function's parameter type with its attributes.
 */
class LlvmParamType(val llvmType: LLVMTypeRef, val attributes: List<LlvmParameterAttribute> = emptyList())

/**
 * A bit better readability for cases when [LlvmParamType] represents return type.
 */
typealias LlvmRetType = LlvmParamType

internal fun ContextUtils.functionHasOuterStackParam(function: IrFunction): Boolean =
        (function !is IrConstructor || !function.constructedClass.isObject)
                && !function.hasAnnotation(RuntimeNames.exportForCppRuntime) && !function.hasAnnotation(KonanFqNames.gcUnsafeCall)
                && !function.isBuiltInOperator && !function.isStaticInitializer
                && function.origin != DECLARATION_ORIGIN_OBJECT_INSTANCE_GETTER
                && !function.hasObjCMethodAnnotation && !function.hasObjCFactoryAnnotation && !function.isObjCClassMethod()
                && function !in generationState.lambdasReferencedFromNative

internal fun ContextUtils.getLlvmFunctionParameterTypes(function: IrFunction): List<LlvmParamType> {
    val returnType = getLlvmFunctionReturnType(function).llvmType
    val paramTypes = ArrayList(function.allParameters.map {
        LlvmParamType(it.type.toLLVMType(llvm), argumentAbiInfo.defaultParameterAttributesForIrType(it.type))
    })
    require(!function.isSuspend) { "Suspend functions should be lowered out at this point"}
    if (functionHasOuterStackParam(function))
        paramTypes.add(LlvmParamType(llvm.int8PtrType))
    if (isObjectType(returnType))
        paramTypes.add(LlvmParamType(kObjHeaderPtrPtr))

    return paramTypes
}

internal fun ContextUtils.getLlvmFunctionReturnType(function: IrFunction): LlvmRetType {
    val returnType = when {
        function is IrConstructor -> LlvmParamType(llvm.voidType)
        function.isSuspend -> error("Suspend functions should be lowered out at this point, but ${function.render()} is still here")
        else -> LlvmParamType(
                function.returnType.getLLVMReturnType(llvm),
                argumentAbiInfo.defaultParameterAttributesForIrType(function.returnType)
        )
    }
    return returnType
}