/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.runtime

import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.InternalForKotlinNative

/**
 * __Note__: this API is unstable and may change in any release.
 *
 * A set of utilities for debugging Kotlin/Native runtime.
 */
@NativeRuntimeApi
@SinceKotlin("1.9")
public object Debugging {
    /**    
     * Run full checked deinitialization on shutdown.
     *
     * Make sure that after exiting `main()` only a single thread with Kotlin runtime remains.
     * Run GC collecting everything including globals.
     *
     * When enabled together with [Platform.isCleanersLeakCheckerActive] additionally checks that no cleaners get executed after `main()`
     */
    public var forceCheckedShutdown: Boolean
        get() = Debugging_getForceCheckedShutdown()
        set(value) = Debugging_setForceCheckedShutdown(value)

    /**
     * Whether the current thread's state allows running Kotlin code.
     *
     * Used by Kotlin/Native internal tests.
     * If it returns `false`, it's a bug.
     */
    @InternalForKotlinNative
    public val isThreadStateRunnable: Boolean
        get() = Debugging_isThreadStateRunnable()
}


@GCUnsafeCall("Kotlin_Debugging_getForceCheckedShutdown")
private external fun Debugging_getForceCheckedShutdown(): Boolean

@GCUnsafeCall("Kotlin_Debugging_setForceCheckedShutdown")
private external fun Debugging_setForceCheckedShutdown(value: Boolean): Unit

@GCUnsafeCall("Kotlin_Debugging_isThreadStateRunnable")
private external fun Debugging_isThreadStateRunnable(): Boolean