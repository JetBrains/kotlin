/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.runtime

import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.escapeAnalysis.Escapes
import kotlin.reflect.KClass

@NativeRuntimeApi
public object HotReload {

    /**
     * Perform hot-code reload in a stop-the-world fashion if there is an upcoming reloading request.
     * Note that this function is invoked implicitly in safe-points, and it should be used for test-only.
     */
    @GCUnsafeCall("Kotlin_native_internal_HotReload_perform")
    @Escapes.Nothing
    public external fun performIfNeeded()

    /**
     * Forces the runtime to reload a new type into the place of an existing type. This is used to replace the old
     * type with a new one, potentially reflecting changes in the structure or attributes of the class.
     *
     * @param oldType The existing class type that needs to be replaced.
     * @param newType The new class type that will replace the old one.
     *
     * @param T1 The type parameter representing the old class type.
     * @param T2 The type parameter representing the new class type.
     */
    @GCUnsafeCall("Kotlin_native_internal_HotReload_forceReloadOf")
    @Escapes.Nothing
    public external fun <T1 : Any, T2: Any> forceReloadOf(oldType: KClass<T1>, newType: KClass<T2>)
}