/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import kotlinx.cinterop.signExtend
import kotlinx.cinterop.toCValues
import llvm.LLVMGetInlineAsm
import llvm.LLVMInlineAsmDialect
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.getARCRetainAutoreleasedReturnValueMarker
import org.jetbrains.kotlin.backend.konan.llvm.*

internal open class ObjCCodeGenerator(val codegen: CodeGenerator) {
    val context = codegen.context

    val dataGenerator = codegen.objCDataGenerator!!

    fun FunctionGenerationContext.genSelector(selector: String): LLVMValueRef = genObjCSelector(selector)

    fun FunctionGenerationContext.genGetLinkedClass(name: String): LLVMValueRef {
        val classRef = dataGenerator.genClassRef(name)
        return load(classRef.llvm)
    }

    private val objcMsgSend = constPointer(
            context.llvm.externalFunction(LlvmFunctionProto(
                    "objc_msgSend",
                    LlvmRetType(int8TypePtr),
                    listOf(LlvmParamType(int8TypePtr), LlvmParamType(int8TypePtr)),
                    isVararg = true,
                    origin = context.stdlibModule.llvmSymbolOrigin
            )).llvmValue
    )

    val objcRelease = run {
        val proto = LlvmFunctionProto(
                "llvm.objc.release",
                LlvmRetType(voidType),
                listOf(LlvmParamType(int8TypePtr)),
                listOf(LlvmFunctionAttribute.NoUnwind),
                origin = context.stdlibModule.llvmSymbolOrigin
        )
        context.llvm.externalFunction(proto)
    }

    val objcAlloc = context.llvm.externalFunction(LlvmFunctionProto(
            "objc_alloc",
            LlvmRetType(int8TypePtr),
            listOf(LlvmParamType(int8TypePtr)),
            origin = context.stdlibModule.llvmSymbolOrigin
    ))

    val objcAutoreleaseReturnValue = context.llvm.externalFunction(LlvmFunctionProto(
            "llvm.objc.autoreleaseReturnValue",
            LlvmRetType(int8TypePtr),
            listOf(LlvmParamType(int8TypePtr)),
            listOf(LlvmFunctionAttribute.NoUnwind),
            origin = context.stdlibModule.llvmSymbolOrigin
    ))

    val objcRetainAutoreleasedReturnValue = context.llvm.externalFunction(LlvmFunctionProto(
            "llvm.objc.retainAutoreleasedReturnValue",
            LlvmRetType(int8TypePtr),
            listOf(LlvmParamType(int8TypePtr)),
            listOf(LlvmFunctionAttribute.NoUnwind),
            origin = context.stdlibModule.llvmSymbolOrigin
    ))

    val objcRetainAutoreleasedReturnValueMarker: LLVMValueRef? by lazy {
        // See emitAutoreleasedReturnValueMarker in Clang.
        val asmString = codegen.context.config.target.getARCRetainAutoreleasedReturnValueMarker() ?: return@lazy null
        val asmStringBytes = asmString.toByteArray()
        LLVMGetInlineAsm(
                Ty = functionType(voidType, false),
                AsmString = asmStringBytes.toCValues(),
                AsmStringSize = asmStringBytes.size.signExtend(),
                Constraints = null,
                ConstraintsSize = 0,
                HasSideEffects = 1,
                IsAlignStack = 0,
                Dialect = LLVMInlineAsmDialect.LLVMInlineAsmDialectATT
        )
    }

    // TODO: this doesn't support stret.
    fun msgSender(functionType: LlvmFunctionSignature): LlvmCallable =
            LlvmCallable(
                    objcMsgSend.bitcast(pointerType(functionType.llvmFunctionType)).llvm,
                    functionType
            )
}

internal fun FunctionGenerationContext.genObjCSelector(selector: String): LLVMValueRef {
    val selectorRef = codegen.objCDataGenerator!!.genSelectorRef(selector)
    // TODO: clang emits it with `invariant.load` metadata.
    return load(selectorRef.llvm)
}