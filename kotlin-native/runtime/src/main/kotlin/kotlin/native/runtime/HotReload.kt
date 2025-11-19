/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.runtime

import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.escapeAnalysis.Escapes

public typealias ReloadSuccessHandler = () -> Unit

@ExportForCppRuntime("Kotlin_native_internal_HotReload_invokeSuccessCallback")
internal fun Kotlin_native_internal_HotReload_invokeSuccessCallback(cb: ReloadSuccessHandler) {
    cb.invoke()
}

@NativeRuntimeApi
public object HotReload {

    /**
     *  Register a callback that will be executed when hot-code reload performs successfully.
     */
    @GCUnsafeCall("Kotlin_native_internal_HotReload_registerSuccessCallback")
    @Escapes.Nothing
    private external fun setReloadSuccessHandler(onReloadSuccess: ReloadSuccessHandler)

    /**
     * Perform hot-code reload in a stop-the-world fashion if there is an upcoming reloading request.
     * Note that this function is invoked implicitly in safe-points, and it should be used for test-only.
     */
    @GCUnsafeCall("Kotlin_native_internal_HotReload_perform")
    @Escapes.Nothing
    public external fun perform(dylibPath: String)
}