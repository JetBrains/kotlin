/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, ExperimentalNativeApi::class)

package kotlin.native.runtime

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.escapeAnalysis.Escapes
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.InternalForKotlinNative
import kotlin.native.internal.NativePtr
import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner

/**
 * Options for [Debugging.dumpMemory] and [Debugging.dumpMemoryAsync].
 *
 * @param omitPrimitiveArrayPayloads If `true`, primitive array contents are omitted from the dump.
 *   Object arrays and native-pointer arrays are still written so the heap graph can be
 *   reconstructed. `kdumputil` fills omitted primitive arrays with zeros when converting to hprof.
 */
@NativeRuntimeApi
@SinceKotlin("2.5")
public class MemoryDumpOptions(
        public val omitPrimitiveArrayPayloads: Boolean = false,
)

/**
 * Handle to an asynchronous heap dump started by [Debugging.dumpMemoryAsync].
 */
@NativeRuntimeApi
@SinceKotlin("2.5")
public class AsyncMemoryDump internal constructor(
        private val handle: NativePtr,
) {
    @Suppress("unused")
    private val cleaner: Cleaner = createCleaner(handle) {
        disposeAsyncDumpHandle(it)
    }

    /**
     * `true` if the dump write has finished (either succeeded or failed).
     */
    public val isCompleted: Boolean
        get() = isCompletedImpl(handle)

    /**
     * Waits until the dump write finishes and returns `true` if it succeeded.
     *
     * May be called multiple times; subsequent calls return the same result immediately.
     */
    public fun waitForCompletion(): Boolean = waitForCompletionImpl(handle)

    @GCUnsafeCall("Kotlin_native_runtime_AsyncMemoryDump_isCompleted")
    @Escapes.Nothing
    private external fun isCompletedImpl(handle: NativePtr): Boolean

    @GCUnsafeCall("Kotlin_native_runtime_AsyncMemoryDump_waitForCompletion")
    @Escapes.Nothing
    private external fun waitForCompletionImpl(handle: NativePtr): Boolean
}

/**
 * __Note__: this API is unstable and may change in any release.
 *
 * A set of utilities for debugging Kotlin/Native runtime.
 */
@NativeRuntimeApi
@SinceKotlin("1.9")
public object Debugging {
    @Deprecated("Checked deinitialization is deprecated.")
    public var forceCheckedShutdown: Boolean = false

    /**
     * Whether the current thread's state allows running Kotlin code.
     *
     * Used by Kotlin/Native internal tests.
     * If it returns `false`, it's a bug.
     */
    @InternalForKotlinNative
    public val isThreadStateRunnable: Boolean
        get() = Debugging_isThreadStateRunnable()

    /**
     * Dump memory in binary format to the given POSIX file descriptor with [options]
     * and returns success flag.
     *
     * The default [options] write a dump with full object and array payloads.
     * Mutators stay suspended until the write finishes, then this function returns
     * so the caller may close [fd].
     *
     * To resume mutators after the snapshot is taken and write in the background, see [dumpMemoryAsync].
     */
    // Keep dumpMemory(Long) in the klib ABI: options did not exist in 1.9.
    @SinceKotlin("2.5")
    public fun dumpMemory(
            fd: Long,
            @IntroducedAt("2.5.20") options: MemoryDumpOptions = MemoryDumpOptions(),
    ): Boolean =
            dumpMemoryWithOptions(fd, options.omitPrimitiveArrayPayloads)

    /**
     * Opt-in dump for production diagnostics that cannot keep mutators stopped for the write
     * (for example a dump triggered from a running service). This is not a replacement for
     * [dumpMemory] when the process is already near out-of-memory.
     *
     * On POSIX except tvOS and watchOS, this forks during the stop-the-world pause after the
     * snapshot is published. The parent resumes mutators and returns an [AsyncMemoryDump] handle
     * while the child writes the copy-on-write snapshot. The dump contents are the heap at fork,
     * not a live view. Copy-on-write can raise parent RSS by about 2× while the child writes, if
     * mutators dirty those pages. Because the child holds its own copy of [fd], the caller may
     * close [fd] after this function returns unless it needs the descriptor to read back an
     * unlinked file. Call [AsyncMemoryDump.waitForCompletion] (or check
     * [AsyncMemoryDump.isCompleted]) before reading or processing the output file and to verify
     * that the write succeeded. If the process or device runs out of memory before the write
     * finishes, the dump may still be lost.
     *
     * On Windows, tvOS, and watchOS there is no fork: this writes in-process like [dumpMemory]
     * and returns an already-completed [AsyncMemoryDump] handle (or `null` if the write failed).
     *
     * Returns `null` if the snapshot could not be started, or if the in-process fallback write failed.
     */
    @SinceKotlin("2.5")
    public fun dumpMemoryAsync(
            fd: Long,
            options: MemoryDumpOptions = MemoryDumpOptions(),
    ): AsyncMemoryDump? {
        val handle = dumpMemoryAsyncWithOptions(fd, options.omitPrimitiveArrayPayloads)
        return if (handle == NativePtr.NULL) null else AsyncMemoryDump(handle)
    }

    @GCUnsafeCall("Kotlin_native_runtime_Debugging_dumpMemoryWithOptions")
    @Escapes.Nothing
    private external fun dumpMemoryWithOptions(fd: Long, omitPrimitiveArrayPayloads: Boolean): Boolean

    @GCUnsafeCall("Kotlin_native_runtime_Debugging_dumpMemoryAsyncWithOptions")
    @Escapes.Nothing
    private external fun dumpMemoryAsyncWithOptions(fd: Long, omitPrimitiveArrayPayloads: Boolean): NativePtr
}

@GCUnsafeCall("Kotlin_Debugging_isThreadStateRunnable")
private external fun Debugging_isThreadStateRunnable(): Boolean

@GCUnsafeCall("Kotlin_native_runtime_AsyncMemoryDump_dispose")
private external fun disposeAsyncDumpHandle(handle: NativePtr)
