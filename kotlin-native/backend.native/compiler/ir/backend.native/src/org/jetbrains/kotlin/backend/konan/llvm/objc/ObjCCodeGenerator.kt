/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import kotlinx.cinterop.signExtend
import llvm.*
import org.jetbrains.kotlin.backend.konan.getARCRetainAutoreleasedReturnValueMarker
import org.jetbrains.kotlin.backend.konan.llvm.*

internal open class ObjCCodeGenerator(val codegen: CodeGenerator) {
    val generationState = codegen.generationState
    val context = codegen.context
    val llvm = codegen.llvm

    val dataGenerator = codegen.objCDataGenerator!!

    fun FunctionGenerationContext.genSelector(selector: String): LLVMValueRef = genObjCSelector(selector)

    fun FunctionGenerationContext.genGetLinkedClass(name: String): LLVMValueRef {
        val classRef = dataGenerator.genClassRef(name)
        return load(llvm.pointerType, classRef.llvm)
    }

    private val objcMsgSend = llvm.externalNativeRuntimeFunction(
            "objc_msgSend",
            LlvmRetType(llvm.pointerType, isObjectType = false),
            listOf(LlvmParamType(llvm.pointerType), LlvmParamType(llvm.pointerType)),
            isVararg = true
    ).toConstPointer()

    val objcRelease = llvm.externalNativeRuntimeFunction(
            "llvm.objc.release",
            LlvmRetType(llvm.voidType, isObjectType = false),
            listOf(LlvmParamType(llvm.pointerType)),
            listOf(LlvmFunctionAttribute.NoUnwind)
    )

    val objcAlloc = llvm.externalNativeRuntimeFunction(
            "objc_alloc",
            LlvmRetType(llvm.pointerType, isObjectType = false),
            listOf(LlvmParamType(llvm.pointerType))
    )

    val objcAutoreleaseReturnValue = llvm.externalNativeRuntimeFunction(
            "llvm.objc.autoreleaseReturnValue",
            LlvmRetType(llvm.pointerType, isObjectType = false),
            listOf(LlvmParamType(llvm.pointerType)),
            listOf(LlvmFunctionAttribute.NoUnwind)
    )

    val objcRetainAutoreleasedReturnValue = llvm.externalNativeRuntimeFunction(
            "llvm.objc.retainAutoreleasedReturnValue",
            LlvmRetType(llvm.pointerType, isObjectType = false),
            listOf(LlvmParamType(llvm.pointerType)),
            listOf(LlvmFunctionAttribute.NoUnwind)
    )

    val objcRetainAutoreleasedReturnValueMarker: LLVMValueRef? by lazy {
        // See emitAutoreleasedReturnValueMarker in Clang.
        val asmString = codegen.context.config.target.getARCRetainAutoreleasedReturnValueMarker() ?: return@lazy null
        LLVMGetInlineAsm(
                Ty = functionType(llvm.voidType, false),
                AsmString = asmString,
                AsmStringSize = asmString.toByteArray().size.signExtend(),
                Constraints = null,
                ConstraintsSize = 0,
                HasSideEffects = 1,
                IsAlignStack = 0,
                Dialect = LLVMInlineAsmDialect.LLVMInlineAsmDialectATT,
                CanThrow = 0,
        )
    }

    // TODO: this doesn't support stret.
    fun msgSender(functionType: LlvmFunctionSignature): LlvmCallable {
        val llvmType = functionType.llvmFunctionType
        return LlvmCallable(
                objcMsgSend.bitcast(llvm.pointerType).llvm,
                functionType)
    }
}

internal fun FunctionGenerationContext.genObjCSelector(selector: String): LLVMValueRef {
    val selectorRef = codegen.objCDataGenerator!!.genSelectorRef(selector).llvm
    // TODO: clang emits it with `invariant.load` metadata.
    // TODO: Propagate the type here without using the typed pointer.
    return load(LLVMGlobalGetValueType(selectorRef)!!, selectorRef)
}