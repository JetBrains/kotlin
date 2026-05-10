/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*

/**
 * Members shared by every runtime LLVM module the K/N backend builds, regardless of target.
 *
 * Implemented by [Runtime] (host runtime, loaded from a per-target `runtime.bc`) and by
 * [CudaDeviceRuntime] (CUDA device runtime, constructed in-memory with the NVPTX target).
 */
internal interface RuntimeBase {
    val llvmModule: LLVMModuleRef
    val target: String
    val dataLayout: String
    val targetData: LLVMTargetDataRef
    val pointerType: LLVMTypeRef
    val pointerSize: Int
    val pointerAlignment: Int
    val isBigEndian: Boolean

    fun sizeOf(type: LLVMTypeRef): Int
    fun alignOf(type: LLVMTypeRef): Int
    fun offsetOf(type: LLVMTypeRef, index: Int): Int
}
