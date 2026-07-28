/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.toCValues
import llvm.*

sealed class LlvmCallable(
        val functionType: LLVMTypeRef,
        val returnsObjectType: Boolean,
        protected val llvmValue: LLVMValueRef,
        protected val attributeProvider: LlvmFunctionAttributeProvider,
) {

    val name: String? by lazy { llvmValue.valueName }
    val returnType: LLVMTypeRef by lazy { LLVMGetReturnType(functionType)!! }
    val numParams: Int by lazy { LLVMCountParamTypes(functionType) }
    val isConstant by lazy { llvmValue.isConst }

    fun buildCall(builder: LLVMBuilderRef, args: List<LLVMValueRef>, name: String = "") =
            LLVMBuildCall2(builder, functionType, llvmValue, args.toCValues(), args.size, name)!!.also {
                attributeProvider.addCallSiteAttributes(it)
            }

    fun buildInvoke(builder: LLVMBuilderRef, args: List<LLVMValueRef>, success: LLVMBasicBlockRef, catch: LLVMBasicBlockRef, name: String = "") =
            LLVMBuildInvoke2(builder, functionType, llvmValue, args.toCValues(), args.size, success, catch, name)!!.also {
                attributeProvider.addCallSiteAttributes(it)
            }

    internal fun toConstPointer() = constPointer(llvmValue)

    internal fun asCallback() = llvmValue
}

class LlvmFunctionPointer(
        functionType: LLVMTypeRef,
        returnsObjectType: Boolean,
        llvmValue: LLVMValueRef,
        attributeProvider: LlvmFunctionAttributeProvider,
) : LlvmCallable(functionType, returnsObjectType, llvmValue, attributeProvider) {
    internal constructor(llvmValue: LLVMValueRef, signature: LlvmFunctionSignature) :
            this(signature.llvmFunctionType, signature.returnsObjectType, llvmValue, signature)
}

sealed class LlvmFunction(
        functionType: LLVMTypeRef,
        returnsObjectType: Boolean,
        llvmValue: LLVMValueRef,
        attributeProvider: LlvmFunctionAttributeProvider,
) : LlvmCallable(functionType, returnsObjectType, llvmValue, attributeProvider) {

    val isNoUnwind by lazy {
        requireNotNull(LLVMIsAFunction(llvmValue)) {
            "The LLVM value '${llvmValue.valueName}' is not a function. Supposed to be a function named '$name'."
        }
        isFunctionNoUnwind(llvmValue)
    }

    fun param(i: Int): LLVMValueRef {
        require(i in 0 until numParams) {
            "Requested index $i but function '$name' got only $numParams params."
        }
        return LLVMGetParam(llvmValue, i)!!
    }

    fun buildLandingpad(builder: LLVMBuilderRef, landingpadType: LLVMTypeRef, numClauses: Int, name: String = "") =
            LLVMBuildLandingPad(builder, landingpadType, llvmValue, numClauses, name)!!

    /**
     * Function prototypes (or [declaration in LLVM terms](https://llvm.org/docs/LangRef.html#functions)) do not belong to a specific module.
     */
    class Declaration(
            functionType: LLVMTypeRef,
            returnsObjectType: Boolean,
            llvmValue: LLVMValueRef,
            attributeProvider: LlvmFunctionAttributeProvider,
    ) : LlvmFunction(functionType, returnsObjectType, llvmValue, attributeProvider) {
        internal constructor(llvmValue: LLVMValueRef, signature: LlvmFunctionSignature) :
                this(signature.llvmFunctionType, signature.returnsObjectType, llvmValue, signature)
    }

    class Definition(
            functionType: LLVMTypeRef,
            returnsObjectType: Boolean,
            llvmValue: LLVMValueRef,
            attributeProvider: LlvmFunctionAttributeProvider,
    ) : LlvmFunction(functionType, returnsObjectType, llvmValue, attributeProvider) {

        internal constructor(llvmValue: LLVMValueRef, signature: LlvmFunctionSignature) :
                this(signature.llvmFunctionType, signature.returnsObjectType, llvmValue, signature)

        fun addBasicBlock(context: LLVMContextRef, name: String = "") =
                LLVMAppendBasicBlockInContext(context, llvmValue, name)!!

        fun blockAddress(label: LLVMBasicBlockRef) =
                LLVMBlockAddress(llvmValue, label)!!

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
    }
}
