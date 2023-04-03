/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal

import kotlin.time.*

/**
 * __Note__: this API is unstable and may change in any release.
 *
 * Kotlin/Native uses tracing garbage collector (GC) that is executed periodically to collect objects
 * that are not reachable from the "roots", like local and global variables.
 * See [documentation](https://kotlinlang.org/docs/native-memory-manager.html) to learn more about
 * Kotlin/Native memory management.
 *
 * This object provides a set of functions and properties that allows to tune garbage collector.
 *
 * __Legacy memory manager__
 *
 * Kotlin/Native relies upon reference counting for object management, however it could
 * not collect cyclical garbage, so we perform periodic garbage collection.
 * This may slow down application, so this interface provides control over how
 * garbage collector activates and runs.
 * Garbage collector can be in one of the following states:
 *  - running
 *  - suspended (so cycle candidates are collected, but GC is not performed until resume)
 *  - stopped (all cyclical garbage is hopelessly lost)
 * Immediately after startup GC is in running state.
 * Depending on application needs it may select to suspend GC for certain phases of
 * its lifetime, and resume it later on, or just completely turn it off, if GC pauses
 * are less desirable than cyclical garbage leaks.
 */
@Deprecated("Use kotlin.native.runtime.GC instead.", ReplaceWith("GC", "kotlin.native.runtime.GC"))
@DeprecatedSinceKotlin(warningSince = "1.9")
@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
object GC {
    /**
     * Trigger new collection and wait for its completion.
     *
     * Legacy MM: force garbage collection immediately, unless collector is stopped
     * with [stop] operation. Even if GC is suspended, [collect] still triggers collection.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_collect")
    external fun collect()

    /**
     * Trigger new collection without waiting for its completion.
     */
     @GCUnsafeCall("Kotlin_native_internal_GC_schedule")
     external fun schedule()

    /**
     * Deprecated and unused.
     *
     * Legacy MM: Request global cyclic collector, operation is async and just triggers the collection.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_collectCyclic")
    @Deprecated("No-op in modern GC implementation")
    external fun collectCyclic()

    /**
     * Deprecated and unused.
     *
     * Legacy MM: Suspend garbage collection. Release candidates are still collected, but
     * GC algorithm is not executed.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_suspend")
    external fun suspend()

    /**
     * Deprecated and unused.
     *
     * Legacy MM: Resume garbage collection. Can potentially lead to GC immediately.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_resume")
    @Deprecated("No-op in modern GC implementation")
    external fun resume()

    /**
     * Deprecated and unused.
     *
     * Legacy MM: Stop garbage collection. Cyclical garbage is no longer collected.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_stop")
    @Deprecated("No-op in modern GC implementation")
    external fun stop()

    /**
     * Deprecated and unused.
     *
     * Legacy MM: Start garbage collection. Cyclical garbage produced while GC was stopped
     * cannot be reclaimed, but all new garbage is collected.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_start")
    @Deprecated("No-op in modern GC implementation")
    external fun start()

    /**
     * Deprecated and unused.
     *
     * Legacy MM: GC threshold, controlling how frequenly GC is activated, and how much time GC
     * takes. Bigger values lead to longer GC pauses, but less GCs.
     *
     * Default: 8 * 1024
     *
     * @throws [IllegalArgumentException] when value is not positive.
     */
    @Suppress("DEPRECATION")
    @Deprecated("No-op in modern GC implementation")
    var threshold: Int by kotlin.native.runtime.GC::threshold

    /**
     * Deprecated and unused.
     *
     * Legacy MM: GC allocation threshold, controlling how frequenly GC collect cycles, and how much time
     * this process takes. Bigger values lead to longer GC pauses, but less GCs.
     *
     * Default: 8 * 1024
     *
     * @throws [IllegalArgumentException] when value is not positive.
     */
    @Suppress("DEPRECATION")
    @Deprecated("No-op in modern GC implementation")
    var collectCyclesThreshold: Long by kotlin.native.runtime.GC::collectCyclesThreshold

    /**
     * How many bytes a thread can allocate before informing the GC scheduler.
     *
     * Default: 10 * 1024
     *
     * Legacy MM: GC allocation threshold, controlling how many bytes allocated since last
     * collection will trigger new GC.
     *
     * Default: (legacy MM) 8 * 1024 * 1024
     *
     * @throws [IllegalArgumentException] when value is not positive.
     */
    var thresholdAllocations: Long by kotlin.native.runtime.GC::thresholdAllocations

    /**
     * If true update targetHeapBytes after each collection.
     *
     * Legacy MM: If GC shall auto-tune thresholds, depending on how much time is spent in collection.
     *
     * Default: true
     */
    var autotune: Boolean by kotlin.native.runtime.GC::autotune


    /**
     * Deprecated and unused.
     *
     * Legacy MM: If cyclic collector for atomic references to be deployed.
     */
    @Suppress("DEPRECATION")
    @Deprecated("No-op in modern GC implementation")
    var cyclicCollectorEnabled: Boolean by kotlin.native.runtime.GC::cyclicCollectorEnabled

    /**
     * When Kotlin code is not allocating enough to trigger GC, the GC scheduler uses timer to drive collection.
     * Timer-triggered collection will happen roughly in [regularGCInterval] .. 2 * [regularGCInterval] since
     * any previous collection.
     * Unused with on-safepoints GC scheduler.
     *
     * Default: 10 seconds
     *
     * Unused in legacy MM.
     *
     * @throws [IllegalArgumentException] when value is negative.
     */
     var regularGCInterval: Duration by kotlin.native.runtime.GC::regularGCInterval

    /**
     * Total amount of heap available for Kotlin objects. When Kotlin objects overflow this heap,
     * the garbage collection is requested. Automatically adjusts when [autotune] is true:
     * after each collection the [targetHeapBytes] is set to heapBytes / [targetHeapUtilization] and
     * capped between [minHeapBytes] and [maxHeapBytes], where heapBytes is heap usage after the garbage
     * is collected.
     * Note, that if after a collection heapBytes > [targetHeapBytes] (which may happen if [autotune] is false,
     * or [maxHeapBytes] is set too low), the next collection will be triggered almost immediately.
     *
     * Default: 1 MiB
     *
     * Unused in legacy MM.
     *
     * @throws [IllegalArgumentException] when value is negative.
     */
    var targetHeapBytes: Long by kotlin.native.runtime.GC::targetHeapBytes

    /**
     * What fraction of the Kotlin heap should be populated.
     * Only used if [autotune] is true. See [targetHeapBytes] for more details.
     *
     * Default: 0.5
     *
     * Unused in legacy MM.
     *
     * @throws [IllegalArgumentException] when value is outside (0, 1] interval.
     */
     var targetHeapUtilization: Double by kotlin.native.runtime.GC::targetHeapUtilization

    /**
     * The minimum value for [targetHeapBytes]
     * Only used if [autotune] is true. See [targetHeapBytes] for more details.
     *
     * Default: 1 MiB
     *
     * Unused in legacy MM.
     *
     * @throws [IllegalArgumentException] when value is negative.
     */
     var minHeapBytes: Long by kotlin.native.runtime.GC::minHeapBytes

    /**
     * The maximum value for [targetHeapBytes].
     * Only used if [autotune] is true. See [targetHeapBytes] for more details.
     *
     * Default: [Long.MAX_VALUE]
     *
     * Unused in legacy MM.
     *
     * @throws [IllegalArgumentException] when value is negative.
     */
     var maxHeapBytes: Long by kotlin.native.runtime.GC::maxHeapBytes

    /**
     * Deprecated and unused. Always returns null.
     *
     * Legacy MM: Detect cyclic references going via atomic references and return list of cycle-inducing objects
     * or `null` if the leak detector is not available. Use [Platform.isMemoryLeakCheckerActive] to check
     * leak detector availability.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_detectCycles")
    @Deprecated("No-op in modern GC implementation")
    external fun detectCycles(): Array<Any>?

    /**
     * Returns statistics of the last finished garbage collection run.
     * This information is supposed to be used for testing and debugging purposes only
     *
     * Can return null, if there was no garbage collection runs yet.
     *
     * Legacy MM: Always returns null
     */
    @ExperimentalStdlibApi
    @Suppress("DEPRECATION")
    val lastGCInfo: kotlin.native.internal.gc.GCInfo?
        get() = kotlin.native.internal.gc.GCInfo.lastGCInfo
}
