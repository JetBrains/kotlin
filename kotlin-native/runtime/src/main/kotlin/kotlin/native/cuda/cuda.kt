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

public const val WarpSize: Int = 32

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

@PublishedApi
@GCUnsafeCall("llvm.nvvm.read.ptx.sreg.ntid.x")
internal external fun blockDim_x(): Int

@PublishedApi
@GCUnsafeCall("llvm.nvvm.read.ptx.sreg.ntid.y")
internal external fun blockDim_y(): Int

@PublishedApi
@GCUnsafeCall("llvm.nvvm.read.ptx.sreg.ntid.z")
internal external fun blockDim_z(): Int

public object blockDim {
    public val x: Int inline get() = blockDim_x()
    public val y: Int inline get() = blockDim_y()
    public val z: Int inline get() = blockDim_z()
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
// Device-side atomics
// =========================================================================
// Modern LLVM (≥ 18) removed the named `llvm.nvvm.atomic.load.add.*` intrinsics in favor
// of the generic `atomicrmw` IR instruction. `@GCUnsafeCall` can only synthesize a call to
// a named function — not an `atomicrmw` — so each typed atomic is declared here as an
// extern stub, and `IrToBitcode.evaluateFunctionCall` recognises calls to those stubs by
// symbol identity (mirroring how `kotlin.native.internal.unreachable` is intercepted) and
// emits the matching `atomicrmw add` / `atomicrmw fadd` instead of generating a real call.
// No entry in the global `IntrinsicType` enum, no compiler-side dispatch on non-CUDA paths.
// The stub function declaration is never referenced after that intercept, so `globaldce`
// in the device-IR cleanup drops it from the emitted PTX.

@PublishedApi
@GCUnsafeCall("cuda_atomicAdd_i32")
internal external fun cuda_atomicAdd_i32(address: CPointer<IntVar>, value: Int): Int

@PublishedApi
@GCUnsafeCall("cuda_atomicAdd_i64")
internal external fun cuda_atomicAdd_i64(address: CPointer<LongVar>, value: Long): Long

@PublishedApi
@GCUnsafeCall("cuda_atomicAdd_f32")
internal external fun cuda_atomicAdd_f32(address: CPointer<FloatVar>, value: Float): Float

@PublishedApi
@GCUnsafeCall("cuda_atomicAdd_f64")
internal external fun cuda_atomicAdd_f64(address: CPointer<DoubleVar>, value: Double): Double

/**
 * Atomically reads the [Int] at [address], adds [value] to it, writes the sum back, and
 * returns the original (pre-add) value. Lowers to NVPTX `atom.add.s32` on global pointers
 * (or `atom.shared.add.s32` on shared-memory pointers, etc., per the pointer's address
 * space).
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun atomicAdd(address: CPointer<IntVar>, value: Int): Int =
        cuda_atomicAdd_i32(address, value)

/**
 * Atomically reads the [Long] at [address], adds [value] to it, writes the sum back, and
 * returns the original (pre-add) value. Lowers to NVPTX `atom.add.s64`.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun atomicAdd(address: CPointer<LongVar>, value: Long): Long =
        cuda_atomicAdd_i64(address, value)

/**
 * Atomically reads the [Float] at [address], adds [value] to it, writes the sum back, and
 * returns the original (pre-add) value. Lowers to NVPTX `atom.add.f32`. Supported on all
 * compute capabilities targeted by this runtime (sm_50+).
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun atomicAdd(address: CPointer<FloatVar>, value: Float): Float =
        cuda_atomicAdd_f32(address, value)

/**
 * Atomically reads the [Double] at [address], adds [value] to it, writes the sum back, and
 * returns the original (pre-add) value. Lowers to NVPTX `atom.add.f64`. **Requires compute
 * capability `sm_60` (Pascal) or higher** — older targets reject `atom.add.f64` at `ptxas`
 * time. The runtime's default `sm_50` target won't include this lowering; raise the target
 * before using it.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun atomicAdd(address: CPointer<DoubleVar>, value: Double): Double =
        cuda_atomicAdd_f64(address, value)

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
 * Allocates room for [count] elements of type `T` on the device (so `count * sizeOf<T>()` bytes)
 * and returns a typed `CPointer<T>` view of the device address. Overload of the raw [cuMemAlloc]
 * driver external; prefer this form when the resulting pointer will be passed to
 * [CudaLaunchpad.launch], so the call site doesn't need a ULong→CPointer bitcast per kernel arg
 * and the byte arithmetic doesn't need to be repeated by hand. The returned pointer is a Kotlin
 * type-system bridge: at the driver-API call ABI level it carries the same 8-byte device address
 * as the raw [cuMemAlloc]'s [CUdeviceptr], and the `cuMemFree` / `cuMemcpyHtoD` / `cuMemcpyDtoH`
 * overloads below accept it directly.
 *
 * Throws [IllegalStateException] on driver error.
 */
public inline fun <reified T : CVariable> cuMemAlloc(count: Int): CPointer<T> = memScoped {
    val byteCount = sizeOf<T>().toULong() * count.toULong()
    val dptr = alloc<ULongVar>()
    val rc = cuMemAlloc(dptr.ptr, byteCount)
    if (rc != 0) throw IllegalStateException("CUDA Driver API error in cuMemAlloc: code $rc")
    dptr.value.toLong().toCPointer<T>()!!
}

@GCUnsafeCall("cuMemcpyHtoD_v2")
public external fun cuMemcpyHtoD(dstDevice: CUdeviceptr, srcHost: COpaquePointer, byteCount: ULong): Int

/**
 * Typed overload — accepts a [CPointer] returned by the typed [cuMemAlloc] in place of the raw
 * [CUdeviceptr], expresses the transfer size as an element [count] of type `T` (with
 * `count * sizeOf<T>()` bytes computed internally, matching the typed [cuMemAlloc]'s shape),
 * and throws [IllegalStateException] on driver error so callers don't have to wrap every
 * memcpy in an explicit result check. Both pointers carry the same `T`, so the host and
 * device element types can't accidentally drift apart.
 */
public inline fun <reified T : CVariable> cuMemcpyHtoD(dstDevice: CPointer<T>, srcHost: CPointer<T>, count: Int) {
    val byteCount = sizeOf<T>().toULong() * count.toULong()
    val rc = cuMemcpyHtoD(dstDevice.toLong().toULong(), srcHost, byteCount)
    if (rc != 0) throw IllegalStateException("CUDA Driver API error in cuMemcpyHtoD: code $rc")
}

@GCUnsafeCall("cuMemcpyDtoH_v2")
public external fun cuMemcpyDtoH(dstHost: COpaquePointer, srcDevice: CUdeviceptr, byteCount: ULong): Int

/**
 * Typed overload — accepts a [CPointer] returned by the typed [cuMemAlloc] in place of the raw
 * [CUdeviceptr], expresses the transfer size as an element [count] of type `T` (with
 * `count * sizeOf<T>()` bytes computed internally, matching the typed [cuMemAlloc]'s shape),
 * and throws [IllegalStateException] on driver error so callers don't have to wrap every
 * memcpy in an explicit result check. Both pointers carry the same `T`, so the host and
 * device element types can't accidentally drift apart.
 */
public inline fun <reified T : CVariable> cuMemcpyDtoH(dstHost: CPointer<T>, srcDevice: CPointer<T>, count: Int) {
    val byteCount = sizeOf<T>().toULong() * count.toULong()
    val rc = cuMemcpyDtoH(dstHost, srcDevice.toLong().toULong(), byteCount)
    if (rc != 0) throw IllegalStateException("CUDA Driver API error in cuMemcpyDtoH: code $rc")
}

@GCUnsafeCall("cuMemFree_v2")
public external fun cuMemFree(dptr: CUdeviceptr): Int

/**
 * Typed overload — accepts a [CPointer] returned by the typed [cuMemAlloc] in place of the raw
 * [CUdeviceptr], and throws [IllegalStateException] on driver error so callers don't have to
 * wrap every free in an explicit result check.
 */
public fun cuMemFree(dptr: CPointer<*>) {
    val rc = cuMemFree(dptr.toLong().toULong())
    if (rc != 0) throw IllegalStateException("CUDA Driver API error in cuMemFree: code $rc")
}

// =========================================================================
// Host → device array upload shortcuts
// =========================================================================
// Each overload allocates a device buffer sized to `data.size` elements of the matching
// primitive variable type via the typed [cuMemAlloc], pins the source array, copies its
// contents in via [cuMemcpyHtoD], and returns the typed device pointer. The caller is
// responsible for freeing the result via [cuMemFree] once the kernel is done with it.
// Throws [IllegalStateException] on driver error (via the underlying overloads).
//
// Why one overload per primitive: Kotlin's `XxxArray` primitive arrays are not generic
// and don't share a common supertype that exposes `.size` + a pinnable element layout
// (`Pinned<XxxArray>.addressOf(0)` returns a different `CPointer<XxxVar>` per array kind).

public fun cuUpload(data: FloatArray): CPointer<FloatVar> {
    val devPtr = cuMemAlloc<FloatVar>(data.size)
    data.usePinned { cuMemcpyHtoD(devPtr, it.addressOf(0), data.size) }
    return devPtr
}

public fun cuUpload(data: IntArray): CPointer<IntVar> {
    val devPtr = cuMemAlloc<IntVar>(data.size)
    data.usePinned { cuMemcpyHtoD(devPtr, it.addressOf(0), data.size) }
    return devPtr
}

public fun cuUpload(data: DoubleArray): CPointer<DoubleVar> {
    val devPtr = cuMemAlloc<DoubleVar>(data.size)
    data.usePinned { cuMemcpyHtoD(devPtr, it.addressOf(0), data.size) }
    return devPtr
}

public fun cuUpload(data: LongArray): CPointer<LongVar> {
    val devPtr = cuMemAlloc<LongVar>(data.size)
    data.usePinned { cuMemcpyHtoD(devPtr, it.addressOf(0), data.size) }
    return devPtr
}

// =========================================================================
// Device → host array download shortcuts
// =========================================================================
// Mirror of `cuUpload`: allocates a host primitive array of [count] elements, pins it,
// copies the device buffer into it via [cuMemcpyDtoH], and returns the host array. Not a
// `CuMemScope` extension — these don't allocate anything on the device, so there's nothing
// for the scope to track. The source [deviceSrc] is the caller's to free as usual.

public fun cuDownload(deviceSrc: CPointer<FloatVar>, count: Int): FloatArray {
    val data = FloatArray(count)
    data.usePinned { cuMemcpyDtoH(it.addressOf(0), deviceSrc, count) }
    return data
}

public fun cuDownload(deviceSrc: CPointer<IntVar>, count: Int): IntArray {
    val data = IntArray(count)
    data.usePinned { cuMemcpyDtoH(it.addressOf(0), deviceSrc, count) }
    return data
}

public fun cuDownload(deviceSrc: CPointer<DoubleVar>, count: Int): DoubleArray {
    val data = DoubleArray(count)
    data.usePinned { cuMemcpyDtoH(it.addressOf(0), deviceSrc, count) }
    return data
}

public fun cuDownload(deviceSrc: CPointer<LongVar>, count: Int): LongArray {
    val data = LongArray(count)
    data.usePinned { cuMemcpyDtoH(it.addressOf(0), deviceSrc, count) }
    return data
}

// =========================================================================
// Scoped device-memory management
// =========================================================================
// `cuMemScoped { … }` is the device-memory analogue of `kotlinx.cinterop.memScoped`. Every
// `cuMemAlloc` / `cuUpload` call inside the lambda is registered with the scope's
// allocation list, and the scope frees them all (in reverse order) when the lambda exits
// — whether by normal return or by exception. Explicit `cuMemFree` inside the scope is
// safe: it both frees and unregisters, so the cleanup pass won't double-free.
//
// The allocator surfaces are exposed as extensions on `CuMemScope` so they shadow the
// top-level `cuMemAlloc` / `cuUpload` / `cuMemFree` inside the block — call sites stay
// identical, only the enclosing `memScoped { … }` changes to `cuMemScoped { … }`.

public class CuMemScope @PublishedApi internal constructor() {
    @PublishedApi
    internal val allocations: MutableList<CPointer<*>> = mutableListOf()
}

public inline fun <reified T : CVariable> CuMemScope.cuMemAlloc(count: Int): CPointer<T> {
    val ptr = kotlin.native.cuda.cuMemAlloc<T>(count)
    allocations.add(ptr)
    return ptr
}

public fun CuMemScope.cuUpload(data: FloatArray): CPointer<FloatVar> {
    val ptr = kotlin.native.cuda.cuUpload(data)
    allocations.add(ptr)
    return ptr
}

public fun CuMemScope.cuUpload(data: IntArray): CPointer<IntVar> {
    val ptr = kotlin.native.cuda.cuUpload(data)
    allocations.add(ptr)
    return ptr
}

public fun CuMemScope.cuUpload(data: DoubleArray): CPointer<DoubleVar> {
    val ptr = kotlin.native.cuda.cuUpload(data)
    allocations.add(ptr)
    return ptr
}

public fun CuMemScope.cuUpload(data: LongArray): CPointer<LongVar> {
    val ptr = kotlin.native.cuda.cuUpload(data)
    allocations.add(ptr)
    return ptr
}

/**
 * Frees [dptr] eagerly and removes it from the scope's pending-free list so the scope's
 * cleanup pass on exit doesn't double-free it. Use sparingly — the normal pattern is to
 * let the scope clean up automatically.
 */
public fun CuMemScope.cuMemFree(dptr: CPointer<*>) {
    allocations.remove(dptr)
    kotlin.native.cuda.cuMemFree(dptr)
}

@PublishedApi
internal fun CuMemScope.clearAll() {
    val errors = mutableListOf<Int>()
    for (i in allocations.indices.reversed()) {
        // Use the raw external (Int-returning) rather than the throwing typed overload so a
        // failed free doesn't strand later allocations un-freed. Collected codes are
        // re-surfaced as a single exception at the end.
        val rc = cuMemFree(allocations[i].toLong().toULong())
        if (rc != 0) errors.add(rc)
    }
    allocations.clear()
    if (errors.isNotEmpty()) {
        throw IllegalStateException("cuMemFree failed during cuMemScoped cleanup: codes=$errors")
    }
}

/**
 * Establishes a scope in which `cuMemAlloc` / `cuUpload` register their results for
 * automatic `cuMemFree` on exit. Mirrors `kotlinx.cinterop.memScoped` for device memory.
 */
public inline fun <R> cuMemScoped(block: CuMemScope.() -> R): R {
    val scope = CuMemScope()
    try {
        return scope.block()
    } finally {
        scope.clearAll()
    }
}

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
@Suppress("ClassName")
public data class dim3(val x: Int, val y: Int = 1, val z: Int = 1)

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
        public val gridSize: dim3,
        public val blockSize: dim3,
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
        grid: dim3,
        block: dim3,
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
