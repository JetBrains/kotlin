/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.toCValues
import llvm.*

/**
 *  Wrapper around LLVM value of functional type.
 *
 *  @todo This class mixes "something that can be called" and function abstractions.
 *        Some of it's methods make sense only for functions. Probably, LlvmFunction sub-class should be extracted.
 */
class LlvmCallable(val functionType: LLVMTypeRef, val returnsObjectType: Boolean, private val llvmValue: LLVMValueRef, private val attributeProvider: LlvmFunctionAttributeProvider) {
    internal constructor(
            llvmValue: LLVMValueRef,
            signature: LlvmFunctionSignature,
    ) : this(signature.llvmFunctionType, signature.returnsObjectType, llvmValue, signature)

    val returnType: LLVMTypeRef by lazy {
        LLVMGetReturnType(functionType)!!
    }

    val name by lazy {
        llvmValue.name
    }

    val numParams by lazy {
        LLVMCountParamTypes(functionType)
    }

    val isConstant by lazy {
        LLVMIsConstant(llvmValue) == 1
    }

    fun buildCall(builder: LLVMBuilderRef, args: List<LLVMValueRef>, name: String = "") =
            LLVMBuildCall2(builder, functionType, llvmValue, args.toCValues(), args.size, name)!!.also {
                attributeProvider.addCallSiteAttributes(it)
            }

    fun buildInvoke(builder: LLVMBuilderRef, args: List<LLVMValueRef>, success: LLVMBasicBlockRef, catch: LLVMBasicBlockRef, name: String = "") =
            LLVMBuildInvoke2(builder, functionType, llvmValue, args.toCValues(), args.size, success, catch, name)!!.also {
                attributeProvider.addCallSiteAttributes(it)
            }

    fun buildLandingpad(builder: LLVMBuilderRef, landingpadType: LLVMTypeRef, numClauses: Int, name: String = "") =
            LLVMBuildLandingPad(builder, landingpadType, llvmValue, numClauses, name)!!

    fun addBasicBlock(context: LLVMContextRef, name: String = "") =
            LLVMAppendBasicBlockInContext(context, llvmValue, name)!!

    fun blockAddress(label: LLVMBasicBlockRef) = LLVMBlockAddress(llvmValue, label)!!

    fun addDebugInfoSubprogram(subprogram: DISubprogramRef) {
        DIFunctionAddSubprogram(llvmValue, subprogram)
    }

    fun createBridgeFunctionDebugInfo(
            builder: DIBuilderRef,
            scope: DIScopeOpaqueRef,
            file: DIFileRef,
            lineNo: Int,
            type: DISubroutineTypeRef,
            isLocal: Int,
            isDefinition: Int,
            scopeLine: Int,
            isTransparentStepping: Boolean,
    ) = DICreateBridgeFunction(
            builder = builder,
            scope = scope,
            function = llvmValue,
            file = file,
            lineNo = lineNo,
            type = type,
            isLocal = isLocal,
            isDefinition = isDefinition,
            scopeLine = scopeLine,
            isTransparentStepping = if (isTransparentStepping) 1 else 0,
    )!!

    fun param(i: Int) : LLVMValueRef {
        require(i in 0 until numParams)
        return LLVMGetParam(llvmValue, i)!!
    }

    val isNoUnwind by lazy {
        LLVMIsAFunction(llvmValue) != null && isFunctionNoUnwind(llvmValue)
    }

    // these functions are potentially unsafe, as they need to use same attribute provider when converted to callable
    internal fun toConstPointer() = constPointer(llvmValue)
    internal fun asCallback() = llvmValue
}
