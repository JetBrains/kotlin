/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.cuda

import kotlinx.cinterop.*
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.unreachable

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
public annotation class CudaCompile

@Target(AnnotationTarget.LOCAL_VARIABLE)
public annotation class Shared(val size: Int = 0)

/**
 * Marker recognised by `CudaLaunchKernelLowering` on every [CudaLaunchpad.launch] overload. The
 * lowering rewrites annotated call sites into a direct invocation of the internal [launchKernel]
 * with the kernel's mangled name embedded as a string literal. Without the lowering the call
 * falls through to the body (`unreachable()`), so a stale or misconfigured pipeline crashes at
 * the unlowered call site instead of silently dispatching to the wrong kernel.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class CudaLaunchKernel

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

// K/N's libllvmstubs.dylib is statically linked against an LLVM (>=21) where the bare
// `llvm.nvvm.barrier0` intrinsic was retired in favor of `llvm.nvvm.barrier.cta.sync.
// aligned.all(i32 id)`, per the migration table in LLVM's `IntrinsicsNVVM.td`. Calling
// the old name produces a bitcode declaration that LLVM doesn't resolve to an intrinsic
// ID (the IR dump marks it `; Unknown intrinsic`), and the NVPTX backend then emits it
// as an opaque `call.uni llvm.nvvm.barrier0, ();` plus a stray `.extern .func` line that
// `ptxas` rejects (dots aren't valid in PTX identifiers). The new intrinsic takes an
// `i32` barrier-id argument; the inline wrapper preserves `__syncthreads()`'s zero-arg
// surface by passing 0 (barrier 0 — the implicit barrier for all `__syncthreads` use).
@PublishedApi
@GCUnsafeCall("llvm.nvvm.barrier.cta.sync.aligned.all")
internal external fun nvvm_barrier_cta_sync_aligned_all(barrier: Int)

@Suppress("NOTHING_TO_INLINE")
public inline fun __syncthreads(): Unit = nvvm_barrier_cta_sync_aligned_all(0)

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

@GCUnsafeCall("cuModuleLoadDataEx")
public external fun cuModuleLoadDataEx(
        module: CPointer<COpaquePointerVar>,
        image: COpaquePointer,
        numOptions: Int,
        options: CPointer<IntVar>,
        optionValues: CPointer<COpaquePointerVar>,
): Int

/** Subset of `CUjit_option` codes used by [launchKernel] for JIT diagnostics. */
private const val CU_JIT_INFO_LOG_BUFFER: Int = 3
private const val CU_JIT_INFO_LOG_BUFFER_SIZE_BYTES: Int = 4
private const val CU_JIT_ERROR_LOG_BUFFER: Int = 5
private const val CU_JIT_ERROR_LOG_BUFFER_SIZE_BYTES: Int = 6

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

/**
 * Pairs the raw [CUdeviceptr] (the 8-byte device address returned by the driver — what
 * [cuMemcpyHtoD] / [cuMemcpyDtoH] / [cuMemFree] take) with a `CPointer<T>` view of the same
 * address (what a `@CudaCompile` kernel's typed parameter slot expects). The two are the same
 * 8 bytes — `cuLaunchKernel` only ever sees the call-ABI payload, so [pointer] is a Kotlin
 * type-system bridge, not a runtime conversion. Use [handle] for driver-API calls, [pointer]
 * when passing this buffer to [CudaLaunchpad.launch].
 */
public class DeviceBuffer<T : CPointed> internal constructor(
        public val handle: CUdeviceptr,
        public val pointer: CPointer<T>,
)

/**
 * Allocates [byteCount] bytes of device memory and returns a [DeviceBuffer] that carries both
 * the raw [CUdeviceptr] (for memcpy/free) and a typed `CPointer<T>` (for kernel-arg use). Use
 * this instead of the raw [cuMemAlloc] when the resulting buffer will be passed to
 * [CudaLaunchpad.launch], so the call site doesn't need a ULong→CPointer bitcast per kernel arg.
 *
 * Throws [IllegalStateException] on driver error.
 */
public fun <T : CPointed> cuMemAllocTyped(byteCount: ULong): DeviceBuffer<T> = memScoped {
    val dptr = alloc<ULongVar>()
    val rc = cuMemAlloc(dptr.ptr, byteCount)
    if (rc != 0) throw IllegalStateException("CUDA Driver API error in cuMemAllocTyped: code $rc")
    val handle: CUdeviceptr = dptr.value
    DeviceBuffer(handle, handle.toLong().toCPointer()!!)
}

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
 * Captures the launch configuration for a `__global__` kernel — grid/block dimensions, shared
 * memory size, and an optional stream — separately from the kernel identity. The user-facing
 * launch surface: build one and call [launch] with a function reference to a `@CudaCompile`
 * top-level function plus the kernel's typed arguments.
 *
 * `CudaLaunchKernelLowering` rewrites each `launch(ref, …)` call into [launchKernel] with the
 * kernel's mangled name embedded as a string literal — same runtime path as before, but the
 * compiler verifies the kernel identity and the argument shape at the source level instead of
 * leaving the user to type a mangled name and an `Any?`-vararg and discover the typo at
 * `cuModuleGetFunction` time. The overloads below cover arities 0..10; their bodies call
 * [unreachable] so any unlowered call crashes loudly instead of silently mis-launching.
 *
 * `launch` MUST NOT be `inline` — an inline body would expand at the user call site and erase
 * the `IrCall` the lowering needs to dispatch on.
 */
public class CudaLaunchpad(
        public val gridSize: Dim3,
        public val blockSize: Dim3,
        public val sharedMemSize: Int = 0,
        public val stream: CUstream? = null,
) {
    @CudaLaunchKernel
    public fun launch(ref: () -> Unit) { unreachable() }

    @CudaLaunchKernel
    public fun <P1> launch(ref: (P1) -> Unit, p1: P1) { unreachable() }

    @CudaLaunchKernel
    public fun <P1, P2> launch(ref: (P1, P2) -> Unit, p1: P1, p2: P2) { unreachable() }

    @CudaLaunchKernel
    public fun <P1, P2, P3> launch(ref: (P1, P2, P3) -> Unit, p1: P1, p2: P2, p3: P3) { unreachable() }

    @CudaLaunchKernel
    public fun <P1, P2, P3, P4> launch(
            ref: (P1, P2, P3, P4) -> Unit,
            p1: P1, p2: P2, p3: P3, p4: P4,
    ) { unreachable() }

    @CudaLaunchKernel
    public fun <P1, P2, P3, P4, P5> launch(
            ref: (P1, P2, P3, P4, P5) -> Unit,
            p1: P1, p2: P2, p3: P3, p4: P4, p5: P5,
    ) { unreachable() }

    @CudaLaunchKernel
    public fun <P1, P2, P3, P4, P5, P6> launch(
            ref: (P1, P2, P3, P4, P5, P6) -> Unit,
            p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6,
    ) { unreachable() }

    @CudaLaunchKernel
    public fun <P1, P2, P3, P4, P5, P6, P7> launch(
            ref: (P1, P2, P3, P4, P5, P6, P7) -> Unit,
            p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7,
    ) { unreachable() }

    @CudaLaunchKernel
    public fun <P1, P2, P3, P4, P5, P6, P7, P8> launch(
            ref: (P1, P2, P3, P4, P5, P6, P7, P8) -> Unit,
            p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8,
    ) { unreachable() }

    @CudaLaunchKernel
    public fun <P1, P2, P3, P4, P5, P6, P7, P8, P9> launch(
            ref: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> Unit,
            p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9,
    ) { unreachable() }

    @CudaLaunchKernel
    public fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> launch(
            ref: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> Unit,
            p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10,
    ) { unreachable() }
}

/**
 * Internal launch entry: invoked only by `CudaLaunchKernelLowering`, which rewrites every typed
 * [CudaLaunchpad.launch] call site into a direct call to this function with [name] supplied as
 * a string literal mangled from the kernel's function symbol. The runtime path — driver init,
 * module load, function lookup, argument marshalling — is unchanged.
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
internal fun launchKernel(
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
        // Use `cuModuleLoadDataEx` instead of `cuModuleLoadData` so we receive the PTX JIT
        // info/error logs alongside the result code. Without this, `CUDA_ERROR_INVALID_PTX`
        // (218) and similar errors carry no detail about which directive or feature the
        // JIT rejected — debugging requires running `ptxas` on the embedded PTX outside.
        val mod = alloc<COpaquePointerVar>()
        val logBufSize = 4 * 1024
        val infoBuf = allocArray<ByteVar>(logBufSize)
        val errBuf = allocArray<ByteVar>(logBufSize)
        // `allocArray` returns uninitialized memory; null-terminate the start so
        // `toKString()` is safe if the driver writes nothing into the buffer.
        infoBuf[0] = 0
        errBuf[0] = 0
        val options = allocArray<IntVar>(4)
        val values = allocArray<COpaquePointerVar>(4)
        options[0] = CU_JIT_INFO_LOG_BUFFER
        values[0] = infoBuf.reinterpret()
        options[1] = CU_JIT_INFO_LOG_BUFFER_SIZE_BYTES
        values[1] = logBufSize.toLong().toCPointer<CPointed>()
        options[2] = CU_JIT_ERROR_LOG_BUFFER
        values[2] = errBuf.reinterpret()
        options[3] = CU_JIT_ERROR_LOG_BUFFER_SIZE_BYTES
        values[3] = logBufSize.toLong().toCPointer<CPointed>()
        val rc = cuModuleLoadDataEx(mod.ptr, getKotlinCudaPtx(), 4, options, values)
        if (rc != 0) {
            val errLog = errBuf.toKString()
            val infoLog = infoBuf.toKString()
            val detail = buildString {
                if (errLog.isNotEmpty()) append("\nerror log: ").append(errLog)
                if (infoLog.isNotEmpty()) append("\ninfo log: ").append(infoLog)
            }
            throw IllegalStateException("CUDA Driver API error in cuModuleLoadDataEx: code $rc$detail")
        }
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
