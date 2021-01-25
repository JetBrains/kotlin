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
}

@SymbolName("Kotlin_Debugging_getForceCheckedShutdown")
@GCCritical
private external fun Debugging_getForceCheckedShutdown(): Boolean

@SymbolName("Kotlin_Debugging_setForceCheckedShutdown")
@GCCritical
private external fun Debugging_setForceCheckedShutdown(value: Boolean): Unit
