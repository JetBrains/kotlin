/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*

/**
 * The "runtime" for CUDA device code. Unlike [Runtime], this is not loaded from a per-target
 * `runtime.bc` on disk — it is constructed in-memory because its content is small: NVPTX
 * target/datalayout configuration plus declarations of the NVVM intrinsics referenced from
 * `kotlin.native.cuda`.
 *
 * Used as the runtime for `BackendJobFragment`s tagged as device fragments. Device codegen
 * targets `nvptx64-nvidia-cuda`; the resulting LLVM module is later lowered to PTX via the
 * LLVM NVPTX backend.
 */
internal class CudaDeviceRuntime(
        private val llvmContext: LLVMContextRef
) : RuntimeBase {
    override val target: String = NVPTX64_TARGET_TRIPLE
    override val dataLayout: String = NVPTX64_DATA_LAYOUT

    override val llvmModule: LLVMModuleRef =
            LLVMModuleCreateWithNameInContext("cuda_device_runtime", llvmContext)!!.also {
                LLVMSetTarget(it, target)
                LLVMSetDataLayout(it, dataLayout)
            }

    override val targetData: LLVMTargetDataRef = LLVMCreateTargetData(dataLayout)!!

    override val pointerType: LLVMTypeRef = LLVMPointerTypeInContext(llvmContext, 0)!!

    override val pointerSize: Int by lazy { sizeOf(pointerType) }
    override val pointerAlignment: Int by lazy { alignOf(pointerType) }
    override val isBigEndian: Boolean by lazy { LLVMByteOrder(targetData) == LLVMByteOrdering.LLVMBigEndian }

    override fun sizeOf(type: LLVMTypeRef) = LLVMABISizeOfType(targetData, type).toInt()
    override fun alignOf(type: LLVMTypeRef) = LLVMABIAlignmentOfType(targetData, type)
    override fun offsetOf(type: LLVMTypeRef, index: Int) = LLVMOffsetOfElement(targetData, type, index).toInt()

    init {
        declareNvvmIntrinsics()
    }

    private fun declareNvvmIntrinsics() {
        val i32 = LLVMInt32TypeInContext(llvmContext)!!
        val voidType = LLVMVoidTypeInContext(llvmContext)!!
        val i32FromNothing = functionType(i32)
        val voidFromNothing = functionType(voidType)

        for (name in NVVM_I32_INTRINSICS) {
            LLVMAddFunction(llvmModule, name, i32FromNothing)
        }
        LLVMAddFunction(llvmModule, NVVM_BARRIER0, voidFromNothing)
    }

    companion object {
        const val NVPTX64_TARGET_TRIPLE = "nvptx64-nvidia-cuda"

        // Standard NVPTX 64-bit datalayout per LLVM upstream NVPTXTargetMachine.
        const val NVPTX64_DATA_LAYOUT = "e-i64:64-i128:128-v16:16-v32:32-n16:32:64"

        // Mirrors the @GCUnsafeCall names in kotlin.native.cuda.cuda.kt; extend as new
        // intrinsics are surfaced there.
        private val NVVM_I32_INTRINSICS = listOf(
                "llvm.nvvm.read.ptx.sreg.tid.x",
                "llvm.nvvm.read.ptx.sreg.tid.y",
                "llvm.nvvm.read.ptx.sreg.tid.z",
                "llvm.nvvm.read.ptx.sreg.ctaid.x",
                "llvm.nvvm.read.ptx.sreg.ctaid.y",
                "llvm.nvvm.read.ptx.sreg.ctaid.z",
        )
        private const val NVVM_BARRIER0 = "llvm.nvvm.barrier0"
    }
}
