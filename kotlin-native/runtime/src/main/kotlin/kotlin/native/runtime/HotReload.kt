/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.runtime

import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.InternalForKotlinNativeTests
import kotlin.native.internal.escapeAnalysis.Escapes

/**
 * Internal function invoked by the C++ runtime when hot-code reload completes successfully.
 * It is not intended for public or internal use from the Kotlin's side.*
 */
@OptIn(NativeRuntimeApi::class)
@ExportForCppRuntime("Kotlin_native_internal_HotReload_invokeSuccessCallback")
internal fun invokeReloadSuccessHandler() {
    HotReload.registeredSuccessHandlers.forEach { it.invoke() }
}

private class HotReloadStatsBuilder(
        var start: Long = 0,
        var end: Long = 0,
        var loadedLibrary: String = "",
        var reboundSymbols: Int = 0,
        var successful: Boolean = false,
) {

    @OptIn(NativeRuntimeApi::class)
    fun build(): HotReload.Stats {
        fill()
        return HotReload.Stats(start, end, loadedLibrary, reboundSymbols, successful)
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStartEpoch")
    private fun setStartEpoch(epoch: Long) {
        start = epoch
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setEndEpoch")
    private fun setEndEpoch(epoch: Long) {
        end = epoch
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setLoadedLibrary")
    private fun setLoadedLibrary(path: String) {
        loadedLibrary = path
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setReboundSymbols")
    private fun setReboundSymbols(symbols: Int) {
        reboundSymbols = symbols
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setSuccessful")
    private fun setSuccessful(wasSuccessful: Boolean) {
        successful = wasSuccessful
    }

    @GCUnsafeCall("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_fill")
    @Escapes.Nothing
    private external fun fill()
}

@NativeRuntimeApi
public object HotReload {

    /**
     * A type alias representing a function that takes no arguments and returns no value.
     * It can be used to handle reload success events or similar callbacks.
     */
    public typealias ReloadSuccessHandler = () -> Unit

    /**
     * Captures telemetry and status information for a single hot-code reload event.
     *
     * @property start The timestamp (in milliseconds since Epoch) when the reload command was received.
     * @property end The timestamp (in milliseconds since Epoch) when the reload process finished.
     * @property loadedLibrary The absolute path or filename of the dynamic library loaded by the runtime.
     * @property reboundSymbols The total count of function symbols that were successfully rebound.
     * @property successful True if the reload command completed without errors, false otherwise.
     */
    public data class Stats(
            val start: Long,
            val end: Long,
            val loadedLibrary: String,
            val reboundSymbols: Int,
            val successful: Boolean,
    )

    internal val registeredSuccessHandlers: LinkedHashSet<ReloadSuccessHandler> = LinkedHashSet()

    /**
     * Registers a handler to be invoked after a successful reload operation.
     * Note that handles are invoked in the same order they were registered to.
     * Also, the callback is executed only once per successful reload event.
     *
     * @param reloadSuccessHandler The handler to be executed when the reload completes successfully.
     */
    public fun addReloadSuccessHandler(reloadSuccessHandler: ReloadSuccessHandler) {
        registeredSuccessHandlers.add(reloadSuccessHandler)
    }

    /**
     * Removes the specified reload success handler from the collection of registered success handlers.
     *
     * @param reloadSuccessHandler The reload success handler to be removed.
     */
    public fun removeReloadSuccessHandler(reloadSuccessHandler: ReloadSuccessHandler) {
        registeredSuccessHandlers.remove(reloadSuccessHandler)
    }

    /**
     * Collect statistics about the latest hot-code reload event.
     */
    @InternalForKotlinNativeTests
    public fun collectLatestStats(): Stats = HotReloadStatsBuilder().build()

    /**
     * Perform hot-code reload in a stop-the-world fashion if there is an upcoming reloading request.
     * Note that this function is invoked implicitly in safe-points, and it should be used for test-only.
     */
    @GCUnsafeCall("Kotlin_native_internal_HotReload_perform")
    @Escapes.Nothing
    @InternalForKotlinNativeTests
    public external fun perform(dylibPath: String)
}