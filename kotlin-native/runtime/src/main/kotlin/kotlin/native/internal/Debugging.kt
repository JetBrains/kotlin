/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native.internal

import kotlin.native.internal.escapeAnalysis.Escapes

/*
 * Internal utilities for debugging/testing K/N compiler and runtime.
 */

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
@Deprecated("Use kotlin.native.runtime.Debugging instead.", ReplaceWith("Debugging", "kotlin.native.runtime.Debugging"))
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
public object Debugging {
    public var forceCheckedShutdown: Boolean by kotlin.native.runtime.Debugging::forceCheckedShutdown

    public val isThreadStateRunnable: Boolean by kotlin.native.runtime.Debugging::isThreadStateRunnable
}

@GCUnsafeCall("Kotlin_Debugging_isPermanent")
@InternalForKotlinNative
@Escapes.Nothing
public external fun Any.isPermanent(): Boolean

@GCUnsafeCall("Kotlin_Debugging_isStack")
@InternalForKotlinNative
@Escapes.Nothing
public external fun Any.isStack(): Boolean
