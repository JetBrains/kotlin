/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native.internal

/*
 * Internal utilities for debugging K/N compiler and runtime.
 */
public object Debugging {
    public var forceCheckedShutdown: Boolean
        get() = Debugging_getForceCheckedShutdown()
        set(value) = Debugging_setForceCheckedShutdown(value)

    public val isThreadStateRunnable: Boolean
        get() = Debugging_isThreadStateRunnable()
}

@GCUnsafeCall("Kotlin_Debugging_isPermanent")
@InternalForKotlinNative
public external fun Any.isPermanent() : Boolean

@GCUnsafeCall("Kotlin_Debugging_isLocal")
@InternalForKotlinNative
public external fun Any.isLocal() : Boolean

@GCUnsafeCall("Kotlin_Debugging_getForceCheckedShutdown")
private external fun Debugging_getForceCheckedShutdown(): Boolean

@GCUnsafeCall("Kotlin_Debugging_setForceCheckedShutdown")
private external fun Debugging_setForceCheckedShutdown(value: Boolean): Unit

@GCUnsafeCall("Kotlin_Debugging_isThreadStateRunnable")
private external fun Debugging_isThreadStateRunnable(): Boolean
