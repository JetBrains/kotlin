/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.toCValues
import llvm.*

internal class BasicLlvmModuleCompilation(
    override val module: LLVMModuleRef,
    override val llvmContext: LLVMContextRef,
    override val staticData: StaticData,
    override val targetTriple: String,
    override val closedWorld: Boolean,
) : LlvmModuleCompilation {
    override val int1Type = LLVMInt1TypeInContext(llvmContext)!!
    override val int8Type = LLVMInt8TypeInContext(llvmContext)!!
    override val int16Type = LLVMInt16TypeInContext(llvmContext)!!
    override val int32Type = LLVMInt32TypeInContext(llvmContext)!!
    override val int64Type = LLVMInt64TypeInContext(llvmContext)!!
    override val floatType = LLVMFloatTypeInContext(llvmContext)!!
    override val doubleType = LLVMDoubleTypeInContext(llvmContext)!!
    override val vector128Type = LLVMVectorType(floatType, 4)!!
    override val voidType = LLVMVoidTypeInContext(llvmContext)!!
    override val int8PtrType = pointerType(int8Type)
    override val int8PtrPtrType = pointerType(int8PtrType)

    override val usedFunctions: MutableList<LLVMValueRef> = mutableListOf<LLVMValueRef>()
    override val usedGlobals: MutableList<LLVMValueRef> = mutableListOf<LLVMValueRef>()

    override fun structType(vararg types: LLVMTypeRef): LLVMTypeRef = structType(types.toList())

    override val kNullInt8Ptr by lazy { LLVMConstNull(int8PtrType)!! }
    override val kNullInt32Ptr by lazy { LLVMConstNull(pointerType(int32Type))!! }
    override val kImmInt32Zero by lazy { int32(0) }
    override val kImmInt32One by lazy { int32(1) }

    override val runtimeAnnotationMap: Map<String, List<LLVMValueRef>> by lazy {
        staticData.getGlobal("llvm.global.annotations")
                ?.getInitializer()
                ?.let { getOperands(it) }
                ?.groupBy(
                    { LLVMGetInitializer(LLVMGetOperand(LLVMGetOperand(it, 1), 0))?.getAsCString() ?: "" },
                    { LLVMGetOperand(LLVMGetOperand(it, 0), 0)!! }
                )
                ?.filterKeys { it != "" }
                ?: emptyMap()
    }
}