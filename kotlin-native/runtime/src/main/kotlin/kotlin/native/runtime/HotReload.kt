/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.runtime

import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.escapeAnalysis.Escapes

@NativeRuntimeApi
@SinceKotlin("1.9")
public object HotReload {

    @GCUnsafeCall("Kotlin_native_internal_HotReload_perform")
    @Escapes.Nothing
    public external fun perform()
}