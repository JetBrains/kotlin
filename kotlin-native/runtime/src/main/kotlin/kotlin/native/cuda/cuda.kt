/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.cuda

import kotlinx.cinterop.*
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

@TypedIntrinsic(IntrinsicType.CREATE_UNINITIALIZED_ARRAY)
public external fun <T : CPointed> alloc(size: Int): CPointer<T>

// =========================================================================
// CUDA Driver API host-side launcher
// =========================================================================
// Everything below runs on the host. The CUDA Driver API is reached via direct
// @GCUnsafeCall externals — symbols resolve at final binary-link time against
// nvcuda.dll (Windows, mingw_x64) or libcuda.so.1 (Linux, linux_x64). The
// Gradle plugin (Task #7) appends `-lcuda` to additionalLinkerOpts when the
// compilation has @CudaCompile files; without it, references to these symbols
// won't resolve. Programs that never call any of these are unaffected — DCE
// drops the unused declarations.
//
// v0 is single-threaded. The CUDA context is thread-bound; multi-threaded
// host use will need an explicit per-thread attach mechanism, deferred.

/** Opaque handle to a CUDA context. */
public typealias CUcontext = COpaquePointer

/** Opaque handle to a loaded CUDA module (a PTX or cubin payload). */
public typealias CUmodule = COpaquePointer

/** Opaque handle to a `__global__` function inside a [CUmodule]. */
public typealias CUfunction = COpaquePointer

/** Opaque handle to a CUDA stream (null = the default stream). */
public typealias CUstream = COpaquePointer

/** CUDA device ordinal — 0 selects the first GPU on the system. */
public typealias CUdevice = Int

/**
 * Device pointer (`unsigned long long` in the CUDA Driver API on 64-bit platforms).
 * Returned by [cuMemAlloc] and consumed by [cuMemcpyHtoD] / [cuMemcpyDtoH] / [cuMemFree]
 * and as kernel argument values for pointer-typed parameters.
 */
public typealias CUdeviceptr = ULong

@GCUnsafeCall("cuInit")
public external fun cuInit(flags: Int): Int

@GCUnsafeCall("cuDeviceGet")
public external fun cuDeviceGet(device: CPointer<IntVar>, ordinal: Int): Int

@GCUnsafeCall("cuCtxCreate_v2")
public external fun cuCtxCreate(pctx: CPointer<COpaquePointerVar>, flags: Int, dev: CUdevice): Int

@GCUnsafeCall("cuCtxSynchronize")
public external fun cuCtxSynchronize(): Int

@GCUnsafeCall("cuModuleLoadData")
public external fun cuModuleLoadData(module: CPointer<COpaquePointerVar>, image: COpaquePointer): Int

@GCUnsafeCall("cuModuleGetFunction")
public external fun cuModuleGetFunction(hfunc: CPointer<COpaquePointerVar>, hmod: CUmodule, name: CPointer<ByteVar>): Int

@GCUnsafeCall("cuLaunchKernel")
public external fun cuLaunchKernel(
        f: CUfunction,
        gridDimX: Int, gridDimY: Int, gridDimZ: Int,
        blockDimX: Int, blockDimY: Int, blockDimZ: Int,
        sharedMemBytes: Int,
        stream: CUstream?,
        kernelParams: CPointer<COpaquePointerVar>?,
        extra: CPointer<COpaquePointerVar>?,
): Int

@GCUnsafeCall("cuMemAlloc_v2")
public external fun cuMemAlloc(dptr: CPointer<ULongVar>, bytesize: ULong): Int

@GCUnsafeCall("cuMemcpyHtoD_v2")
public external fun cuMemcpyHtoD(dstDevice: CUdeviceptr, srcHost: COpaquePointer, byteCount: ULong): Int

@GCUnsafeCall("cuMemcpyDtoH_v2")
public external fun cuMemcpyDtoH(dstHost: COpaquePointer, srcDevice: CUdeviceptr, byteCount: ULong): Int

@GCUnsafeCall("cuMemFree_v2")
public external fun cuMemFree(dptr: CUdeviceptr): Int

/**
 * Accessor for the embedded PTX text. The Gradle plugin (Task #7) generates a `.c` file
 * containing `static const char kKotlinCudaPtx[] = "<ptx text>"` plus a thin
 * `const char* getKotlinCudaPtx(void) { return kKotlinCudaPtx; }`, compiled and linked
 * into the final binary. Resolves only when the user's source set carries @CudaCompile
 * files; pure-host builds DCE the call chain that reaches it.
 */
@GCUnsafeCall("getKotlinCudaPtx")
internal external fun getKotlinCudaPtx(): COpaquePointer

/**
 * Three-component grid or block dimension for [launchKernel]. `y` and `z` default to 1
 * for the common 1D-launch case.
 */
public data class Dim3(val x: Int, val y: Int = 1, val z: Int = 1)

/**
 * Launches a `__global__` kernel by name with the given grid/block dimensions and
 * arguments.
 *
 * On the first call this initializes the CUDA Driver, picks device 0, creates a primary
 * context, and loads the embedded PTX module via `cuModuleLoadData`. The resolved
 * [CUfunction] for [name] is cached across launches.
 *
 * Argument marshalling: each value in [args] is allocated in a temporary memory scope and
 * its address contributes one entry in the `void**` array passed to `cuLaunchKernel`.
 * Supported types: [Int], [Long], [Float], [Double], [ULong] (for [CUdeviceptr]), and
 * [CPointer]. Other types throw [IllegalArgumentException].
 *
 * v0 is single-threaded — the CUDA context is thread-bound, so multi-threaded use needs
 * explicit per-thread context attachment which is not yet implemented.
 */
public fun launchKernel(
        name: String,
        grid: Dim3,
        block: Dim3,
        sharedMemBytes: Int = 0,
        stream: CUstream? = null,
        vararg args: Any?,
) {
    initCudaContext()
    val function = getOrLoadKernel(name)

    memScoped {
        val params: CPointer<COpaquePointerVar>? = if (args.isEmpty()) {
            null
        } else {
            val array = allocArray<COpaquePointerVar>(args.size)
            for (i in args.indices) {
                array[i] = marshalKernelArg(this, args[i])
            }
            array
        }
        val result = cuLaunchKernel(
                function,
                grid.x, grid.y, grid.z,
                block.x, block.y, block.z,
                sharedMemBytes,
                stream,
                params,
                null,
        )
        checkCuResult(result, "cuLaunchKernel($name)")
    }
}

private var cudaContext: CUcontext? = null
private var cudaModule: CUmodule? = null
private val kernelCache: MutableMap<String, CUfunction> = mutableMapOf()

/**
 * Initializes the CUDA Driver, picks device 0, and creates a primary context. Idempotent —
 * subsequent calls are no-ops. [launchKernel] invokes this on first use, but callers using
 * the raw Driver API ([cuMemAlloc] etc.) before launching a kernel must invoke this
 * themselves; otherwise those calls fail with `CUDA_ERROR_NOT_INITIALIZED` (code 3).
 *
 * v0 hardcodes device 0 and is single-threaded — the created context is bound to the
 * calling thread.
 */
public fun initCudaContext() {
    if (cudaContext != null) return
    checkCuResult(cuInit(0), "cuInit")
    memScoped {
        val device = alloc<IntVar>()
        checkCuResult(cuDeviceGet(device.ptr, 0), "cuDeviceGet")

        val ctx = alloc<COpaquePointerVar>()
        checkCuResult(cuCtxCreate(ctx.ptr, 0, device.value), "cuCtxCreate")
        cudaContext = ctx.value!!
    }
}

private fun getOrLoadKernel(name: String): CUfunction {
    kernelCache[name]?.let { return it }

    val module = cudaModule ?: memScoped {
        val mod = alloc<COpaquePointerVar>()
        checkCuResult(cuModuleLoadData(mod.ptr, getKotlinCudaPtx()), "cuModuleLoadData")
        mod.value!!.also { cudaModule = it }
    }

    return memScoped {
        val func = alloc<COpaquePointerVar>()
        checkCuResult(cuModuleGetFunction(func.ptr, module, name.cstr.ptr), "cuModuleGetFunction($name)")
        func.value!!.also { kernelCache[name] = it }
    }
}

private fun marshalKernelArg(scope: MemScope, arg: Any?): COpaquePointer = when (arg) {
    is Int -> scope.alloc<IntVar>().apply { value = arg }.ptr.reinterpret()
    is Long -> scope.alloc<LongVar>().apply { value = arg }.ptr.reinterpret()
    is Float -> scope.alloc<FloatVar>().apply { value = arg }.ptr.reinterpret()
    is Double -> scope.alloc<DoubleVar>().apply { value = arg }.ptr.reinterpret()
    is ULong -> scope.alloc<ULongVar>().apply { value = arg }.ptr.reinterpret()
    is CPointer<*> -> scope.alloc<COpaquePointerVar>().apply { value = arg.reinterpret() }.ptr.reinterpret()
    null -> throw IllegalArgumentException("null kernel argument is not supported")
    else -> throw IllegalArgumentException("unsupported kernel argument type: ${arg::class.simpleName}")
}

private fun checkCuResult(result: Int, what: String) {
    if (result != 0) throw IllegalStateException("CUDA Driver API error in $what: code $result")
}
