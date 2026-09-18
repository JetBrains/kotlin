/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.runtime

import kotlin.native.internal.escapeAnalysis.Escapes
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.InternalForKotlinNative

/**
 * Options for [Debugging.dumpMemory].
 *
 * @param omitPrimitiveArrayPayloads If `true`, primitive array contents are omitted from the dump.
 *   Object arrays and native-pointer arrays are still written so the heap graph can be
 *   reconstructed. `kdumputil` fills omitted primitive arrays with zeros when converting to hprof.
 * @param gzip If `true`, the dump is written as a gzip member. `kdumputil` detects the gzip
 *   magic and decompresses before parsing. Ignored on tvOS and watchOS, where the
 *   sysroot does not provide zlib; the dump is then written uncompressed.
 */
@NativeRuntimeApi
public class MemoryDumpOptions(
        public val omitPrimitiveArrayPayloads: Boolean = false,
        public val gzip: Boolean = false,
)

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
     * Dump memory in binary format to the given POSIX file descriptor and
     * returns success flag.
     *
     * The dump is written uncompressed, with full object and array payloads.
     * Mutators stay suspended until the write finishes, then this function returns.
     */
    @GCUnsafeCall("Kotlin_native_runtime_Debugging_dumpMemory")
    @Escapes.Nothing
    public external fun dumpMemory(fd: Long): Boolean

    /**
     * Dump memory in binary format to the given POSIX file descriptor with [options]
     * and returns success flag.
     *
     * Mutators stay suspended until the write finishes, then this function returns
     * so the caller may close [fd].
     */
    public fun dumpMemory(fd: Long, options: MemoryDumpOptions): Boolean =
            dumpMemoryWithOptions(fd, options.omitPrimitiveArrayPayloads, options.gzip)

    @GCUnsafeCall("Kotlin_native_runtime_Debugging_dumpMemoryWithOptions")
    @Escapes.Nothing
    private external fun dumpMemoryWithOptions(fd: Long, omitPrimitiveArrayPayloads: Boolean, gzip: Boolean): Boolean
}

@GCUnsafeCall("Kotlin_Debugging_isThreadStateRunnable")
private external fun Debugging_isThreadStateRunnable(): Boolean
