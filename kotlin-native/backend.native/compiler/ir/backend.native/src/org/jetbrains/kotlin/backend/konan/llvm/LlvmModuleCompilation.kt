/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.toCValues
import llvm.*


internal interface LlvmModuleCompilationOwner {
    val llvm: LlvmModuleCompilation
}

/**
 * "Pure" version of [Llvm] that does not depend on [NativeGenerationState] and is not [RuntimeAware], thus easier to instantiate.
 */
internal interface LlvmModuleCompilation {
    val module: LLVMModuleRef
    val llvmContext: LLVMContextRef
    val staticData: StaticData
    val targetTriple: String
    val closedWorld: Boolean
    fun verify() {
        verifyModule(module)
    }

    val int1Type: LLVMTypeRef
    val int8Type: LLVMTypeRef
    val int16Type: LLVMTypeRef
    val int32Type: LLVMTypeRef
    val int64Type: LLVMTypeRef
    val floatType: LLVMTypeRef
    val doubleType: LLVMTypeRef
    val vector128Type: LLVMTypeRef
    val voidType: LLVMTypeRef
    val int8PtrType: LLVMTypeRef
    val int8PtrPtrType: LLVMTypeRef

    fun structType(vararg types: LLVMTypeRef): LLVMTypeRef

    fun constInt1(value: Boolean) = ConstInt1(this, value)
    fun constInt8(value: Byte) = ConstInt8(this, value)
    fun constInt16(value: Short) = ConstInt16(this, value)
    fun constChar16(value: Char) = ConstChar16(this, value)
    fun constInt32(value: Int) = ConstInt32(this, value)
    fun constInt64(value: Long) = ConstInt64(this, value)
    fun constFloat32(value: Float) = ConstFloat32(this, value)
    fun constFloat64(value: Double) = ConstFloat64(this, value)

    fun int1(value: Boolean): LLVMValueRef = constInt1(value).llvm
    fun int8(value: Byte): LLVMValueRef = constInt8(value).llvm
    fun int16(value: Short): LLVMValueRef = constInt16(value).llvm
    fun char16(value: Char): LLVMValueRef = constChar16(value).llvm
    fun int32(value: Int): LLVMValueRef = constInt32(value).llvm
    fun int64(value: Long): LLVMValueRef = constInt64(value).llvm
    fun float32(value: Float): LLVMValueRef = constFloat32(value).llvm
    fun float64(value: Double): LLVMValueRef = constFloat64(value).llvm

    val runtimeAnnotationMap: Map<String, List<LLVMValueRef>>

    val usedFunctions: MutableList<LLVMValueRef>
    val usedGlobals: MutableList<LLVMValueRef>

    fun appendGlobal(name: String, args: List<LLVMValueRef>) {
        if (args.isEmpty()) return

        val argsCasted = args.map { constPointer(it).bitcast(int8PtrType) }
        val llvmUsedGlobal = staticData.placeGlobalArray(name, int8PtrType, argsCasted)

        LLVMSetLinkage(llvmUsedGlobal.llvmGlobal, LLVMLinkage.LLVMAppendingLinkage)
        LLVMSetSection(llvmUsedGlobal.llvmGlobal, "llvm.metadata")
    }

    val kNullInt8Ptr: LLVMValueRef
    val kNullInt32Ptr: LLVMValueRef
    val kImmInt32Zero: LLVMValueRef
    val kImmInt32One: LLVMValueRef

    fun struct(vararg elements: ConstValue) = Struct(structType(elements.map { it.llvmType }), *elements)

    private fun structType(types: List<LLVMTypeRef>): LLVMTypeRef =
            LLVMStructTypeInContext(llvmContext, types.toCValues(), types.size, 0)!!
}