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
 * It is not intended for public or internal use from the Kotlin's side.
 */
@OptIn(NativeRuntimeApi::class)
@ExportForCppRuntime("Kotlin_native_internal_HotReload_invokeReloadSuccessHandler")
internal fun invokeReloadSuccessHandler() {
    HotReload.registeredSuccessHandlers.forEach { it.invoke() }
}

private class HotReloadStatsBuilder(
        var start: Long = 0,
        var end: Long = 0,
        var loadedObjects: List<String> = emptyList(),
        var reboundSymbols: Int = 0,
        var successful: Boolean = false,
        var loadNs: Long = 0,
        var stubsNs: Long = 0,
        var redirectNs: Long = 0,
        var stateTransferNs: Long = 0,
        var requestParseNs: Long = 0,
        var stwWaitNs: Long = 0,
) {

    @OptIn(NativeRuntimeApi::class)
    fun build(): HotReload.Stats {
        fill()
        return HotReload.Stats(start, end, loadedObjects, reboundSymbols, successful, loadNs, stubsNs, redirectNs, stateTransferNs, requestParseNs, stwWaitNs)
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
    private fun setLoadedObjects(paths: Array<String>) {
        loadedObjects = paths.asList()
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setReboundSymbols")
    private fun setReboundSymbols(symbols: Int) {
        reboundSymbols = symbols
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setSuccessful")
    private fun setSuccessful(wasSuccessful: Boolean) {
        successful = wasSuccessful
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setLoadNs")
    private fun setLoadNs(ns: Long) {
        loadNs = ns
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStubsNs")
    private fun setStubsNs(ns: Long) {
        stubsNs = ns
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setRedirectNs")
    private fun setRedirectNs(ns: Long) {
        redirectNs = ns
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStateTransferNs")
    private fun setStateTransferNs(ns: Long) {
        stateTransferNs = ns
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setRequestParseNs")
    private fun setRequestParseNs(ns: Long) {
        requestParseNs = ns
    }

    @ExportForCppRuntime("Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStwWaitNs")
    private fun setStwWaitNs(ns: Long) {
        stwWaitNs = ns
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
     * @property loadedLibraries The absolute path or filename of the dynamic library loaded by the runtime.
     * @property reboundSymbols The total count of function symbols that were successfully rebound.
     * @property successful True if the reload command completed without errors, false otherwise.
     * @property loadNs Time spent loading object files into the JIT, in nanoseconds.
     * @property stubsNs Time spent creating redirectable stubs, in nanoseconds.
     * @property redirectNs Time spent redirecting stubs to new implementations, in nanoseconds.
     * @property stateTransferNs Time spent migrating live objects to new class layouts, in nanoseconds.
     * @property requestParseNs Time spent on the server side reading and parsing the reload request, in nanoseconds.
     * @property stwWaitNs Time elapsed between requesting thread suspension and all threads reaching safepoints, in nanoseconds.
     */
    public data class Stats(
            val start: Long,
            val end: Long,
            val loadedLibraries: List<String>,
            val reboundSymbols: Int,
            val successful: Boolean,
            val loadNs: Long,
            val stubsNs: Long,
            val redirectNs: Long,
            val stateTransferNs: Long,
            val requestParseNs: Long,
            val stwWaitNs: Long,
    )

    internal val registeredSuccessHandlers: LinkedHashSet<ReloadSuccessHandler> = LinkedHashSet()

    /**
     * Registers a handler to be invoked after a successful reload operation.
     * Note that handles are invoked in the same order they were registered to.
     * Also, the callback is executed only once per successful reload event.
     * Note that the execution of handlers is not thread-safe.
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
    public fun collectLatestStats(): Stats = HotReloadStatsBuilder().build()

    /**
     * Perform hot-code reload in a stop-the-world fashion if there is an upcoming reloading request.
     * Note that this function is invoked implicitly in safe-points, and it should be used for test-only.
     */
    @GCUnsafeCall("Kotlin_native_internal_HotReload_perform")
    @Escapes.Nothing
    @InternalForKotlinNativeTests
    public external fun perform(objectPath: String)
}
