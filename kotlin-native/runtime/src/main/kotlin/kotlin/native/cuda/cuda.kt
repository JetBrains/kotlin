/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.native.cuda

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.IntrinsicType
import kotlin.native.internal.TypedIntrinsic

@Target(AnnotationTarget.FILE)
public annotation class CudaCompile

@Target(AnnotationTarget.LOCAL_VARIABLE)
public annotation class Shared

@PublishedApi
@GCUnsafeCall("llvm.nvvm.read.ptx.sreg.tid.x")
internal external fun threadIdx_x(): Int

@PublishedApi
@GCUnsafeCall("llvm.nvvm.read.ptx.sreg.tid.y")
internal external fun threadIdx_y(): Int

@PublishedApi
@GCUnsafeCall("llvm.nvvm.read.ptx.sreg.tid.z")
internal external fun threadIdx_z(): Int

public object threadIdx {
    public val x: Int inline get() = threadIdx_x()
    public val y: Int inline get() = threadIdx_y()
    public val z: Int inline get() = threadIdx_z()
}

@PublishedApi
@GCUnsafeCall("llvm.nvvm.read.ptx.sreg.ctaid.x")
internal external fun blockIdx_x(): Int

@PublishedApi
@GCUnsafeCall("llvm.nvvm.read.ptx.sreg.ctaid.y")
internal external fun blockIdx_y(): Int

@PublishedApi
@GCUnsafeCall("llvm.nvvm.read.ptx.sreg.ctaid.z")
internal external fun blockIdx_z(): Int

public object blockIdx {
    public val x: Int inline get() = blockIdx_x()
    public val y: Int inline get() = blockIdx_y()
    public val z: Int inline get() = blockIdx_z()
}

// TODO: gridIdx

@GCUnsafeCall("llvm.nvvm.barrier0")
public external fun __syncthreads()

@OptIn(ExperimentalForeignApi::class)
@TypedIntrinsic(IntrinsicType.CREATE_UNINITIALIZED_ARRAY)
public external fun <T : CPointed> alloc(size: Int): CPointer<T>
